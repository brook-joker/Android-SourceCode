/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app;

import android.annotation.Nullable;
import android.content.SharedPreferences;
import android.os.FileUtils;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.util.Log;

import com.google.android.collect.Maps;
import com.android.internal.util.XmlUtils;

import dalvik.system.BlockGuard;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;

import libcore.io.IoUtils;

final class SharedPreferencesImpl implements SharedPreferences {
    private static final String TAG = "SharedPreferencesImpl";
    private static final boolean DEBUG = false;

    // 锁规则
    // Lock ordering rules:
    //  - acquire SharedPreferencesImpl.this before EditorImpl.this
    //  - acquire mWritingToDiskLock before EditorImpl.this

    private final File mFile;
    private final File mBackupFile;
    private final int mMode;

    private Map<String, Object> mMap;     // guarded by 'this'
    private int mDiskWritesInFlight = 0;  // guarded by 'this'
    private boolean mLoaded = false;      // guarded by 'this'
    private long mStatTimestamp;          // guarded by 'this'
    private long mStatSize;               // guarded by 'this'

    private final Object mWritingToDiskLock = new Object();
    private static final Object mContent = new Object();
    private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners =
            new WeakHashMap<OnSharedPreferenceChangeListener, Object>();

    SharedPreferencesImpl(File file, int mode) {
        mFile = file;
        //创建为.bak为后缀的备份文件
        mBackupFile = makeBackupFile(file);
        mMode = mode;
        mLoaded = false;
        mMap = null;
        startLoadFromDisk();
    }

    /**
     * 从磁盘加载SP文件
     * 首次使用则创建相应xml文件;
     * 异步加载文件内容到内存; 此时执行getXXX()和setxxx()以及edit()方法都是阻塞等待的, 直到文件数据全部加载到内存;
     * 一旦完全加载到内存, 后续的getXXX()则是直接访问内存.
     */
    private void startLoadFromDisk() {
        synchronized (this) {
            //标记SP文件是否加载到内存
            mLoaded = false;
        }
        new Thread("SharedPreferencesImpl-load") {
            public void run() {
                loadFromDisk();
            }
        }.start();
    }

    private void loadFromDisk() {
        synchronized (SharedPreferencesImpl.this) {
            if (mLoaded) {
                return;
            }
            if (mBackupFile.exists()) {
                mFile.delete();
                mBackupFile.renameTo(mFile);
            }
        }

        // Debugging
        if (mFile.exists() && !mFile.canRead()) {
            Log.w(TAG, "Attempt to read preferences file " + mFile + " without permission");
        }

        Map map = null;
        StructStat stat = null;
        try {
            stat = Os.stat(mFile.getPath());
            if (mFile.canRead()) {
                BufferedInputStream str = null;
                try {
                    str = new BufferedInputStream(
                            new FileInputStream(mFile), 16*1024);
                    map = XmlUtils.readMapXml(str);
                } catch (XmlPullParserException | IOException e) {
                    Log.w(TAG, "getSharedPreferences", e);
                } finally {
                    IoUtils.closeQuietly(str);
                }
            }
        } catch (ErrnoException e) {
            /* ignore */
        }

        synchronized (SharedPreferencesImpl.this) {
            mLoaded = true;
            if (map != null) {
                mMap = map;//从文件读取的信息保存到mMap
                mStatTimestamp = stat.st_mtime;//更新修改时间
                mStatSize = stat.st_size;//更新文件大小
            } else {
                mMap = new HashMap<>();
            }
            notifyAll();//唤醒处于等待状态的线程
        }
    }

    static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }

    void startReloadIfChangedUnexpectedly() {
        synchronized (this) {
            // TODO: wait for any pending writes to disk?
            if (!hasFileChangedUnexpectedly()) {
                return;
            }
            startLoadFromDisk();
        }
    }

    // Has the file changed out from under us?  i.e. writes that
    // we didn't instigate.
    private boolean hasFileChangedUnexpectedly() {
        synchronized (this) {
            if (mDiskWritesInFlight > 0) {
                // If we know we caused it, it's not unexpected.
                if (DEBUG) Log.d(TAG, "disk write in flight, not unexpected.");
                return false;
            }
        }

        final StructStat stat;
        try {
            /*
             * Metadata operations don't usually count as a block guard
             * violation, but we explicitly want this one.
             */
            BlockGuard.getThreadPolicy().onReadFromDisk();
            stat = Os.stat(mFile.getPath());
        } catch (ErrnoException e) {
            return true;
        }

        synchronized (this) {
            return mStatTimestamp != stat.st_mtime || mStatSize != stat.st_size;
        }
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
            mListeners.put(listener, mContent);
        }
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
            mListeners.remove(listener);
        }
    }

    private void awaitLoadedLocked() {
        if (!mLoaded) {
            // Raise an explicit StrictMode onReadFromDisk for this
            // thread, since the real read will be in a different
            // thread and otherwise ignored by StrictMode.
            BlockGuard.getThreadPolicy().onReadFromDisk();
        }
        while (!mLoaded) {
            try {
                //当没有加载完成,则进入等待状态
                wait();
            } catch (InterruptedException unused) {
            }
        }
    }

    public Map<String, ?> getAll() {
        synchronized (this) {
            awaitLoadedLocked();
            //noinspection unchecked
            return new HashMap<String, Object>(mMap);
        }
    }

    /**
     * 当loadFromDisk没有执行完成, 则会阻塞查询操作;
     * 当数据加载完成, 则直接从mMap来查询相应数据;
     * @param key The name of the preference to retrieve.
     * @param defValue Value to return if this preference does not exist.
     *
     * @return
     */
    @Nullable
    public String getString(String key, @Nullable String defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            String v = (String)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Nullable
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        synchronized (this) {
            awaitLoadedLocked();
            Set<String> v = (Set<String>) mMap.get(key);
            return v != null ? v : defValues;
        }
    }

    public int getInt(String key, int defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Integer v = (Integer)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public long getLong(String key, long defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Long v = (Long)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public float getFloat(String key, float defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Float v = (Float)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Boolean v = (Boolean)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    public boolean contains(String key) {
        synchronized (this) {
            awaitLoadedLocked();
            return mMap.containsKey(key);
        }
    }

    public Editor edit() {
        // TODO: remove the need to call awaitLoadedLocked() when
        // requesting an editor.  will require some work on the
        // Editor, but then we should be able to do:
        //
        //      context.getSharedPreferences(..).edit().putString(..).apply()
        //
        // ... all without blocking.
        //该过程同样要等待awaitLoadedLocked完成, 然后创建EditorImpl对象. 而EditorImpl作为SharedPreferencesImpl的内部类,其继承于Editor类.
        synchronized (this) {
            awaitLoadedLocked();
        }

        return new EditorImpl();
    }

    // Return value from EditorImpl#commitToMemory()
    private static class MemoryCommitResult {
        public boolean changesMade;  // any keys different?
        public List<String> keysModified;  // may be null
        public Set<OnSharedPreferenceChangeListener> listeners;  // may be null
        public Map<?, ?> mapToWriteToDisk;
        public final CountDownLatch writtenToDiskLatch = new CountDownLatch(1);
        public volatile boolean writeToDiskResult = false;

        public void setDiskWriteResult(boolean result) {
            writeToDiskResult = result;
            writtenToDiskLatch.countDown();
        }
    }

    /**
     * 从这里可以看出, 这些数据修改操作仅仅是修改mModified和mClear. 直到数据提交commit或许apply过程,
     * 才会真正的把数据更新到SharedPreferencesImpl(简称SPI).
     * 比如设置mClear=true则会情况SPI的mMap数据.从这里可以看出, 这些数据修改操作仅仅是修改mModified和mClear.
     * 直到数据提交commit或许apply过程, 才会真正的把数据更新到SharedPreferencesImpl(简称SPI). 比如设置mClear=true则会清空SPI的mMap数据.
     */
    public final class EditorImpl implements Editor {
        private final Map<String, Object> mModified = Maps.newHashMap();
        private boolean mClear = false;

        public Editor putString(String key, @Nullable String value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            synchronized (this) {
                mModified.put(key,
                        (values == null) ? null : new HashSet<String>(values));
                return this;
            }
        }
        public Editor putInt(String key, int value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putLong(String key, long value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putFloat(String key, float value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }
        public Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                mModified.put(key, value);
                return this;
            }
        }

        public Editor remove(String key) {
            synchronized (this) {
                mModified.put(key, this);
                return this;
            }
        }

        public Editor clear() {
            synchronized (this) {
                mClear = true;
                return this;
            }
        }

        /**
         * apply跟commit的最大区别 在于apply的写入文件操作是在单线程的线程池来完成.
         *
         * apply方法开始的时候, 会把awaitCommit放入QueuedWork;
         * 文件写入操作完成, 则会把相应的awaitCommit从QueuedWork中移除.
         */
        public void apply() {
            //将数据同步到SharedPreferencesImpl的mMap, 并保存到MemoryCommitResult的mapToWriteToDisk
            final MemoryCommitResult mcr = commitToMemory();
            final Runnable awaitCommit = new Runnable() {
                    public void run() {
                        try {
                            //进入等待状态
                            mcr.writtenToDiskLatch.await();
                        } catch (InterruptedException ignored) {
                        }
                    }
                };
            //将awaitCommit添加到QueuedWork
            QueuedWork.add(awaitCommit);

            Runnable postWriteRunnable = new Runnable() {
                    public void run() {
                        awaitCommit.run();
                        //从QueuedWork移除
                        QueuedWork.remove(awaitCommit);
                    }
                };
            //写入到磁盘文件; 先之前把原有数据保存到.bak为后缀的文件,用于在写磁盘的过程出现任何异常可恢复数据;
            SharedPreferencesImpl.this.enqueueDiskWrite(mcr, postWriteRunnable);

            // Okay to notify the listeners before it's hit disk
            // because the listeners should always get the same
            // SharedPreferences instance back, which has the
            // changes reflected in memory.
            notifyListeners(mcr);
        }

        // Returns true if any changes were made
        // 将mMap信息赋值给mapToWriteToDisk, 并mDiskWritesInFlight加1;
        // 当mClear为true, 则直接清空mMap;
        // 当value值为this或null, 则移除相应的key;
        // 当value值发生改变, 则会更新到mMap;
        // 只要有key/value发生改变(新增, 删除), 则设置mcr.changesMade = true. 最后会清空EditorImpl中的mModified数据.
        private MemoryCommitResult commitToMemory() {
            MemoryCommitResult mcr = new MemoryCommitResult();
            synchronized (SharedPreferencesImpl.this) {
                // We optimistically don't make a deep copy until
                // a memory commit comes in when we're already
                // writing to disk.
                if (mDiskWritesInFlight > 0) {
                    // We can't modify our mMap as a currently
                    // in-flight write owns it.  Clone it before
                    // modifying it.
                    // noinspection unchecked
                    mMap = new HashMap<String, Object>(mMap);
                }
                mcr.mapToWriteToDisk = mMap;
                mDiskWritesInFlight++;

                //是否有监听key改变的监听者
                boolean hasListeners = mListeners.size() > 0;
                if (hasListeners) {
                    mcr.keysModified = new ArrayList<String>();
                    mcr.listeners =
                            new HashSet<OnSharedPreferenceChangeListener>(mListeners.keySet());
                }

                synchronized (this) {
                    //当mClear为true, 则直接清空mMap
                    if (mClear) {
                        if (!mMap.isEmpty()) {
                            mcr.changesMade = true;
                            mMap.clear();
                        }
                        mClear = false;
                    }

                    for (Map.Entry<String, Object> e : mModified.entrySet()) {
                        String k = e.getKey();
                        Object v = e.getValue();
                        // "this" is the magic value for a removal mutation. In addition,
                        // setting a value to "null" for a given key is specified to be
                        // equivalent to calling remove on that key.
                        // 程序员的恶趣味,在remove的时候其实是修改当前对应的key的value为this
                        // 所以在判断条件为value == this or value == null时再remove
                        if (v == this || v == null) {
                            if (!mMap.containsKey(k)) {
                                continue;
                            }
                            mMap.remove(k);
                        } else {
                            if (mMap.containsKey(k)) {
                                Object existingValue = mMap.get(k);
                                if (existingValue != null && existingValue.equals(v)) {
                                    continue;
                                }
                            }
                            mMap.put(k, v);
                        }
                        // changesMade代表数据是否有改变
                        mcr.changesMade = true;
                        if (hasListeners) {
                            mcr.keysModified.add(k);
                        }
                    }
                    //清空EditorImpl中的mModified数据
                    mModified.clear();
                }
            }
            return mcr;
        }

        /**
         * 当没有key发生改变, 则直接返回; 否则执行step2;
         * 将mMap全部信息写入文件, 如果写入成功则删除备份文件,如果写入失败则删除mFile.
         * 每次commit是把全部数据更新到文件, 所以每个文件的数据量必须保证足够精简.
         * @return
         */
        public boolean commit() {
            MemoryCommitResult mcr = commitToMemory();
            SharedPreferencesImpl.this.enqueueDiskWrite(
                mcr, null /* sync write on this thread okay */);
            try {
                mcr.writtenToDiskLatch.await();
            } catch (InterruptedException e) {
                return false;
            }
            notifyListeners(mcr);
            return mcr.writeToDiskResult;
        }

        private void notifyListeners(final MemoryCommitResult mcr) {
            if (mcr.listeners == null || mcr.keysModified == null ||
                mcr.keysModified.size() == 0) {
                return;
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (int i = mcr.keysModified.size() - 1; i >= 0; i--) {
                    final String key = mcr.keysModified.get(i);
                    for (OnSharedPreferenceChangeListener listener : mcr.listeners) {
                        if (listener != null) {
                            listener.onSharedPreferenceChanged(SharedPreferencesImpl.this, key);
                        }
                    }
                }
            } else {
                // Run this function on the main thread.
                ActivityThread.sMainThreadHandler.post(new Runnable() {
                        public void run() {
                            notifyListeners(mcr);
                        }
                    });
            }
        }
    }

    /**
     * Enqueue an already-committed-to-memory result to be written
     * to disk.
     *
     * They will be written to disk one-at-a-time in the order
     * that they're enqueued.
     *
     * @param postWriteRunnable if non-null, we're being called
     *   from apply() and this is the runnable to run after
     *   the write proceeds.  if null (from a regular commit()),
     *   then we're allowed to do this disk write on the main
     *   thread (which in addition to reducing allocations and
     *   creating a background thread, this has the advantage that
     *   we catch them in userdebug StrictMode reports to convert
     *   them where possible to apply() ...)
     */
    private void enqueueDiskWrite(final MemoryCommitResult mcr,
                                  final Runnable postWriteRunnable) {
        final Runnable writeToDiskRunnable = new Runnable() {
                public void run() {
                    synchronized (mWritingToDiskLock) {
                        //执行文件写入操作
                        writeToFile(mcr);
                    }
                    synchronized (SharedPreferencesImpl.this) {
                        mDiskWritesInFlight--;
                    }
                    if (postWriteRunnable != null) {
                        postWriteRunnable.run();
                    }
                }
            };

        final boolean isFromSyncCommit = (postWriteRunnable == null);

        // Typical #commit() path with fewer allocations, doing a write on
        // the current thread.
        if (isFromSyncCommit) { //commit方法会进入该分支
            boolean wasEmpty = false;
            synchronized (SharedPreferencesImpl.this) {
                //commitToMemory过程会加1,则wasEmpty=true
                wasEmpty = mDiskWritesInFlight == 1;
            }
            if (wasEmpty) {
                //跳转到上面
                writeToDiskRunnable.run();
                return;
            }
        }
        // commit 不执行该方法
        // apply 将任务放入单线程的线程池来执行
        QueuedWork.singleThreadExecutor().execute(writeToDiskRunnable);
    }

    private static FileOutputStream createFileOutputStream(File file) {
        FileOutputStream str = null;
        try {
            str = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            File parent = file.getParentFile();
            if (!parent.mkdir()) {
                Log.e(TAG, "Couldn't create directory for SharedPreferences file " + file);
                return null;
            }
            FileUtils.setPermissions(
                parent.getPath(),
                FileUtils.S_IRWXU|FileUtils.S_IRWXG|FileUtils.S_IXOTH,
                -1, -1);
            try {
                str = new FileOutputStream(file);
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "Couldn't create SharedPreferences file " + file, e2);
            }
        }
        return str;
    }

    // Note: must hold mWritingToDiskLock
    private void writeToFile(MemoryCommitResult mcr) {
        // Rename the current file so it may be used as a backup during the next read
        // 重命名当前文件，以便在下次读取时将其用作备份
        if (mFile.exists()) {
            ////没有key发生改变, 则直接返回
            if (!mcr.changesMade) {
                // If the file already exists, but no changes were
                // made to the underlying map, it's wasteful to
                // re-write the file.  Return as if we wrote it
                // out.
                mcr.setDiskWriteResult(true);
                return;
            }
            if (!mBackupFile.exists()) {
                //当备份文件不存在, 则把mFile重命名为备份文件
                if (!mFile.renameTo(mBackupFile)) {
                    Log.e(TAG, "Couldn't rename file " + mFile
                          + " to backup file " + mBackupFile);
                    mcr.setDiskWriteResult(false);
                    return;
                }
            } else {
                //否则,直接删除mFile
                mFile.delete();
            }
        }

        // Attempt to write the file, delete the backup and return true as atomically as
        // possible.  If any exception occurs, delete the new file; next time we will restore
        // from the backup.
        try {
            FileOutputStream str = createFileOutputStream(mFile);
            if (str == null) {
                mcr.setDiskWriteResult(false);
                return;
            }
            //将mMap全部信息写入文件
            XmlUtils.writeMapXml(mcr.mapToWriteToDisk, str);
            FileUtils.sync(str);
            str.close();
            ContextImpl.setFilePermissionsFromMode(mFile.getPath(), mMode, 0);
            try {
                final StructStat stat = Os.stat(mFile.getPath());
                synchronized (this) {
                    mStatTimestamp = stat.st_mtime;
                    mStatSize = stat.st_size;
                }
            } catch (ErrnoException e) {
                // Do nothing
            }
            // Writing was successful, delete the backup file if there is one.
            //写入成功, 则删除备份文件
            mBackupFile.delete();
            //返回写入成功, 唤醒等待线程
            mcr.setDiskWriteResult(true);
            return;
        } catch (XmlPullParserException e) {
            Log.w(TAG, "writeToFile: Got exception:", e);
        } catch (IOException e) {
            Log.w(TAG, "writeToFile: Got exception:", e);
        }
        // Clean up an unsuccessfully written file
        //如果写入文件的操作失败, 则删除未成功写入的文件
        if (mFile.exists()) {
            if (!mFile.delete()) {
                Log.e(TAG, "Couldn't clean up partially-written file " + mFile);
            }
        }
        //返回写入失败, 唤醒等待线程
        mcr.setDiskWriteResult(false);
    }
}
