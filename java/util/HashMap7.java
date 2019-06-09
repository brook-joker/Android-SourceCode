package java.util;

import java.io.Serializable;

public class HashMap7<K,V>
        extends AbstractMap<K,V>
        implements Map<K,V>, Cloneable, Serializable {

    /**
     * 默认容量为16
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * 最大容量为2^30. 超过最大容量则, 自动转换为2^30
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认负载因子为0.75
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 空的Entry数组, 初始化使用
     */
    static final Entry<?,?>[] EMPTY_TABLE = {};

    /**
     * Entry数组, 用于存放HashMap中的数据, 其大小必须为2的幂
     * HashMap采用Hash表的方式存放数据, 若出现hash碰撞, 则采用单向链表的方式进行存储.
     * 具体的实现方式在下面介绍
     */
    transient Entry<K,V>[] table = (Entry<K,V>[]) EMPTY_TABLE;

    /**
     * Map的实际大小
     */
    transient int size;

    // 阀值, 当size超过该阀值时, 自动扩容量.
    int threshold;

    /**
     * 负载因子
     */
    final float loadFactor;

    /**
     * HashMap的修改次数. 仅用于采用iterator进行遍历时, 若发现modCount不同, 则直接报错. 即fail-fast模式.
     */
    transient int modCount;

    /**
     * 默认构造函数.
     * 默认容量为16
     * 默认负载因子为0.75
     * 默认的阀值是16*0.75=12
     * 即当HashMap的size>12时, 则会自动扩充容量. 扩充后的容量=当前容量*2
     * 在JDK1.7中HashMap中table在第一次put数据时才会被初始化, 具体初始化过程后面进行详细介绍
     */
    public HashMap7() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 带有初始容量及负载因子的构造器
     */
    public HashMap7(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);

        this.loadFactor = loadFactor;
        threshold = initialCapacity;
        init();
    }

    /**
     * 在HashMap子类中, 可以通过该方法进行一些初始化或扩展. 熟称Hook(钩子).
     * 因为是包访问权限, 我们是用不到了. 有兴趣的可以看下LinkedHashMap中的实现.
     */
    void init() {
    }


    /**
     * 带有初始容量的构造器
     */
    public HashMap7(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 我们来先看下put方法. 方法中涉及的其他方法将按照调用顺序逐个进行说明.
     */
    public V put(K key, V value) {
        //在put时进行初始化
        //因为HashMap是非线程安全的, 所以如果在多线程中使用可能会导致数据被初始化多次.
        //最终导致数据丢失.
        if (table == EMPTY_TABLE) {
            inflateTable(threshold);
        }

        //key==null时, 把该值put到table[0]中
        //在HashMap中仅存在一个key=null的数据
        if (key == null)
            return putForNullKey(value);

        //rehash, 重新计算key的hash值
        int hash = hash(key);

        //找到hash在table中的位置(索引)
        int i = indexFor(hash, table.length);

        //因为Hash表中的数据是采用单向链表的方式存储的, 所以这里通过索引取出该单向链表,
        //然后逐个比对.
        for (Entry<K,V> e = table[i]; e != null; e = e.next) {
            Object k;

            //值已经存在, 则替换value
            //判断方式为:
            // 1) 先判断hash值是否相同. 注: hash值相同key不一定相同
            // 2) 再判断key是否相等
            if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                V oldValue = e.value;
                e.value = value;

                //空方法, 只在put时调用.
                e.recordAccess(this);
                return oldValue;
            }
        }

        //modCount++, 记录数据修改次数. 方便做fail-fast
        modCount++;

        //若不存在该实体, 则创建新的Entry
        addEntry(hash, key, value, i);
        return null;
    }

    /**
     * 往table中put key==null的值.
     * 一个HashMap中只存在一个key==null的值, 且存放在table[0]中
     */
    private V putForNullKey(V value) {
        for (Entry<K,V> e = table[0]; e != null; e = e.next) {
            //key已经存在, 则替换.
            if (e.key == null) {
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }
        //创建新的Entry
        modCount++;
        addEntry(0, null, value, 0);
        return null;
    }

    /**
     * 初始化table
     */
    private void inflateTable(int toSize) {
        //找到大于或等于toSize的2的幂, 具体查找方法看下方法roundUpToPowerOf2.
        //有时候在面试时会被问到, 如果已知有18条数据, 那么在初始化HashMap时长度给定多少合适?
        //如果面试官只关心长度, 那么你回答长度在17-32之间都可以. 可能他更想要的答案时32吧.
        //因为输入初始化长度17-32时，经过源码取最高位为1,其余为全部置0之后必定为32
        int capacity = roundUpToPowerOf2(toSize);

        //而在JDK6中是通过不停的左移得到capacity的. 所以在JDK7中对此做了优化.
        //int capacity = 1;
        //while (capacity < initialCapacity)
        //    capacity <<= 1;

        //初始化threshold
        threshold = (int) Math.min(capacity * loadFactor, MAXIMUM_CAPACITY + 1);

        //初始化table
        table = new Entry[capacity];

        //对String的特殊处理, 将放在最后进行介绍
        //因为这一块是Jdk7中的尝试性方法, 在Jdk8中已经去掉了, 所以将不进行详细说明
        initHashSeedAsNeeded(capacity);
    }

    /**
     * 返回大于number的2的幂.
     * if number>=2^30 return 2^30
     * if number=1 return 1 即2^0
     * else return 最接近number的2^x
     */
    private static int roundUpToPowerOf2(int number) {
        // assert number >= 0 : "number must be non-negative";
        return number >= MAXIMUM_CAPACITY
                ? MAXIMUM_CAPACITY
                : (number > 1)
                //这里是避免number正好是2的幂. 所以采用number-1再左移一位的方式
                ? Integer.highestOneBit((number - 1) << 1)
                : 1;
    }

    /**
     * 此方法为Integer中的方法, 加进来只是为了方便说明.
     * 该方法的返回结果为: 取输入值的最高位的1, 后面全部补0
     */
    // public static int highestOneBit(int i) {
    //     // 这里以8为例. 8的二进制: 0000 1000
    //     i |= (i >>  1); //0000 1000 | 0000 0100 = 0000 1100
    //     i |= (i >>  2); //0000 1100 | 0000 0011 = 0000 1111
    //     i |= (i >>  4); //0000 1111 | 0000 1111 = 0000 1111
    //     i |= (i >>  8); //0000 1111 | 0000 1111 = 0000 1111
    //     i |= (i >> 16); //0000 1111 | 0000 1111 = 0000 1111
    //     //可以看出上面这些 右移 异或 等操作其实就是把最高位右移31位
    //     //最后的结果就是经过处理变成从最高位的1开始, 之后全部为1
    //     return i - (i >>> 1); //0000 1111 - 0000 0111 = 15 - 7 = 8
    // }

    /**
     * 对key进行rehash, 让hash值更为散列, 尽量避免hash碰撞.
     */
    final int hash(Object k) {
        //这一块主要时为String类型进行特殊的hash处理,
        //新的哈希算法可能会严重影响高并发、多线程代码. 所以在Jdk8中已经弃用.
        //有兴趣的可以参考: http://www.importnew.com/7656.html
        int h = hashSeed;
        if (0 != h && k instanceof String) {
            return sun.misc.Hashing.stringHash32((String) k);
        }

        h ^= k.hashCode();

        // 主要是为了让高位低位都参与计算, 让hash值更散列.
        // 在此不做详细介绍. 想了解的可以参照: http://www.iteye.com/topic/709945
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Returns index for hash code h.
     * 计算hash在table中的位置.
     * 通过该方法可以看出为什么要求table的length必须等于2的幂
     * 1111 1111 & 0000 0010 = 0000 0010.
     * 说白了就是把hash值与length-1取余
     */
    static int indexFor(int h, int length) {
        // assert Integer.bitCount(length) == 1 : "length must be a non-zero power of 2";
        return h & (length-1);
    }

    /**
     * 添加Entry, 当size>=threshold, 并且bucketIndex有值时进行resize.
     */
    void addEntry(int hash, K key, V value, int bucketIndex) {
        //当size>=threshold(阀值), 并且要加入的位置有值时进行容量扩充
        //比如: new HashMap(), 默认的threshold=16*0.75=12
        //也就是说当put第12个数据, 并且该位置上有值时进行resize
        if ((size >= threshold) && (null != table[bucketIndex])) {
            resize(2 * table.length);
            hash = (null != key) ? hash(key) : 0;
            bucketIndex = indexFor(hash, table.length);
        }

        createEntry(hash, key, value, bucketIndex);
    }

    /**
     * 创建Entry, 并把旧的Entry放到单向链表尾部  头插法
     */
    void createEntry(int hash, K key, V value, int bucketIndex) {
        Entry<K,V> e = table[bucketIndex];
        table[bucketIndex] = new Entry<>(hash, key, value, e);
        size++;
    }

    /**
     * 扩充table的大小. 并把原有数据转移到新的table中
     */
    void resize(int newCapacity) {
        Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry[] newTable = new Entry[newCapacity];
        //把oldTable中的数据转移到newTable中
        transfer(newTable, initHashSeedAsNeeded(newCapacity));
        //置换成新表
        table = newTable;
        //重新设置threshold
        threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
    }

    /**
     * 从oldTable中把数据转移到newTable中
     * rehash: 默认为true, 通过该方法获取initHashSeedAsNeeded(newCapacity), 该方法是配合测试String的新的hash策略使用.
     */
    void transfer(Entry[] newTable, boolean rehash) {
        int newCapacity = newTable.length;
        //遍历table中的每个Entry
        for (Entry<K,V> e : table) {
            while(null != e) {
                //取出e.next, 方便后面做数据交换
                Entry<K,V> next = e.next;
                if (rehash) { //是否需要重新hash
                    e.hash = null == e.key ? 0 : hash(e.key);
                }
                //重新计算Entry在table中的位置
                int newKey = indexFor(e.hash, newCapacity);

                //进行数据交换.
                //1. 把newTable[i]对应的Entry赋值给e.next. 即把e拼接到newTable[i]对应的链表中
                //注意这里是头插法（C->B->A）
                //如果没有冲突的话  newTable[newKey] = null
                //如果有冲突的话  以头插法进行链表连接
                e.next = newTable[newKey];
                //2. 把e赋值给newTable[i]. 即: 把拼接后的newTable[i]放回到newTable中.
                newTable[newKey] = e;
                //3. 把next赋值给e, 继续往下遍历oldTable中的数据
                e = next;
            }
        }
    }

    /**
     * putAll与put差不多. 在此不做赘述. 可以参考put的说明
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;

        if (table == EMPTY_TABLE) {
            inflateTable((int) Math.max(numKeysToBeAdded * loadFactor, threshold));
        }

        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * 这个构造方法会在初始化时对table进行初始化. 初始化过程不在赘述
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        inflateTable(threshold);

        putAllForCreate(m);
    }

    /**
     * 通过key取值
     */
    public V get(Object key) {
        if (key == null)
            return getForNullKey();

        Entry<K,V> entry = getEntry(key);

        return null == entry ? null : entry.getValue();
    }

    /**
     * 从table[0]的Entry中取key=null的数据.
     * 有时我们会通过map.get(key) == null来判断map中是否存在指定的数据,
     * 一般情况下是不会有问题的. 但是如果正好key=null, value=null.
     * 此时通过get(key)就无法区分map中是否真正存在这条数据了.
     * 因为get(key)=null是有多种情况的:
     *      1. value不存在
     *      2. map为空
     *      3. key不存在
     * 所以判断数据是否存在还是用containsKey比较靠谱.
     */
    private V getForNullKey() {
        if (size == 0) {
            return null;
        }

        //null的key默认放在table[0]中
        for (Entry<K,V> e = table[0]; e != null; e = e.next) {
            if (e.key == null)
                return e.value;
        }
        return null;
    }

    /**
     * 获取指定key对应的Entry
     */
    final Entry<K,V> getEntry(Object key) {
        if (size == 0) {
            return null;
        }

        //null的hash默认为0.
        int hash = (key == null) ? 0 : hash(key);

        //循环table[indexFor(hash, table.length)]
        //通过e.hash == hash && (key==e.key || key.equals(e.key)), 找到相应的entry
        for (Entry<K,V> e = table[indexFor(hash, table.length)];
             e != null;
             e = e.next) {
            Object k;
            if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                return e;
        }
        return null;
    }

    /**
     * This method is used instead of put by constructors and
     * pseudoconstructors (clone, readObject).  It does not resize the table,
     * check for comodification, etc.  It calls createEntry rather than
     * addEntry.
     *
     * 目前该方法仅在构造器中使用. 该方法不会resize table.
     */
    private void putForCreate(K key, V value) {
        int hash = null == key ? 0 : hash(key);
        int i = indexFor(hash, table.length);

        /**
         * Look for preexisting entry for key.  This will never happen for
         * clone or deserialize.  It will only happen for construction if the
         * input Map is a sorted map whose ordering is inconsistent w/ equals.
         */
        for (Entry<K,V> e = table[i]; e != null; e = e.next) {
            Object k;
            if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k)))) {
                e.value = value;
                return;
            }
        }

        createEntry(hash, key, value, i);
    }

    //仅在构造器中使用.
    private void putAllForCreate(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putForCreate(e.getKey(), e.getValue());
    }



    /**
     * 通过key移除数据
     */
    public V remove(Object key) {
        Entry<K,V> e = removeEntryForKey(key);
        return (e == null ? null : e.value);
    }

    /**
     * 通过key移除数据
     */
    final Entry<K,V> removeEntryForKey(Object key) {
        if (size == 0) {
            return null;
        }

        int hash = (key == null) ? 0 : hash(key);
        //取key在table中的位置
        int i = indexFor(hash, table.length);

        //取出数据, 方便后面进行操作
        Entry<K,V> prev = table[i];
        Entry<K,V> e = prev;

        while (e != null) {
            //先取出e.next
            Entry<K,V> next = e.next;
            Object k;

            if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k)))) {
                //设置modCount
                modCount++;
                size--;
                //如果删除的元素在table[i]的第一个位置. 在需要把table[i]指给next;
                //如果删除的元素在table[i]的其他位置. 则把table[i].next=next;
                if (prev == e)
                    table[i] = next;
                else
                    prev.next = next;

                //Entry中的Hook方法. 方面子类进行扩展
                e.recordRemoval(this);
                return e;
            }
            //没找到, 则继续遍历
            prev = e;
            e = next;
        }

        return e;
    }

    /**
     * 通过Entry移除数据, 根据entry.equals方法进行判断
     */
    final Entry<K,V> removeMapping(Object o) {
        if (size == 0 || !(o instanceof Map.Entry))
            return null;

        Map.Entry<K,V> entry = (Map.Entry<K,V>) o;
        Object key = entry.getKey();
        int hash = (key == null) ? 0 : hash(key);
        int i = indexFor(hash, table.length);
        Entry<K,V> prev = table[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (e.hash == hash && e.equals(entry)) {
                modCount++;
                size--;

                //如果删除的元素在table[i]的第一个位置. 在需要把table[i]指给next;
                //如果删除的元素在table[i]的其他位置. 则把table[i].next=next;
                if (prev == e)
                    table[i] = next;
                else
                    prev.next = next;

                e.recordRemoval(this);
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }

    /**
     * 返回map的实际长度
     */
    public int size() {
        return size;
    }

    /**
     * 判断map是否为空
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 是否包含指定的key
     */
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * 清空map
     */
    public void clear() {
        //需要注意table只会不断变大, 不会自动变小. 即使清空也只是清空里面的内容
        //所以对于一个比较大的map对象, 清空后最好把该对象设置为null. 方便JVM回收.
        modCount++;
        Arrays.fill(table, null);
        size = 0;
    }

    /**
     * 判断map中是否包含指定的value.
     * 当找到一个相等的value即返回true.
     */
    public boolean containsValue(Object value) {

        //containsValue是遍历全部数据, 效率不高
        if (value == null)
            return containsNullValue();

        Entry[] tab = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (value.equals(e.value))
                    return true;
        return false;
    }

    /**
     * 是否存在value=null的数据
     * 当找到一个value==null的数据即返回true.
     */
    private boolean containsNullValue() {
        Entry[] tab = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (e.value == null)
                    return true;
        return false;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    public Object clone() {
        HashMap<K,V> result = null;
        try {
            result = (HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // assert false;
        }
        if (result.table != EMPTY_TABLE) {
            result.inflateTable(Math.min(
                    (int) Math.min(
                            size * Math.min(1 / loadFactor, 4.0f),
                            // we have limits...
                            HashMap.MAXIMUM_CAPACITY),
                    table.length));
        }
        result.entrySet = null;
        result.modCount = 0;
        result.size = 0;
        result.init();
        result.putAllForCreate(this);

        return result;
    }

    //单向链表的数据结构
    static class Entry<K,V> implements Map.Entry<K,V> {
        final K key;
        V value;
        Entry<K,V> next;
        int hash; //key的hash值

        /**
         * Creates new entry.
         */
        Entry(int h, K k, V v, Entry<K,V> n) {
            value = v;
            next = n;
            key = k;
            hash = h;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();

            //两个Entry的key相等, 并且value也相等
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        public final int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
        }

        public final String toString() {
            return getKey() + "=" + getValue();
        }

        /**
         * put时调用
         */
        void recordAccess(HashMap7<K,V> m) {
        }

        /**
         * remove时调用
         */
        void recordRemoval(HashMap7<K,V> m) {
        }
    }

    private abstract class HashIterator<E> implements Iterator<E> {
        Entry<K,V> next;        // next entry to return
        int expectedModCount;   // For fast-fail
        int index;              // current slot
        Entry<K,V> current;     // current entry

        HashIterator() {
            //在创建iterator时把map中的modCount赋值给expectedModCount
            //当遍历过程中发现expectedModCount != modCount时直接报错
            //出现这种情况, 则说明有其他线程操作了(put/remove)该map
            //所以在遍历hashMap时建议使用iterator, 避免出现数据处理异常
            expectedModCount = modCount;
            if (size > 0) { // advance to first entry
                Entry[] t = table;
                while (index < t.length && (next = t[index++]) == null)
                    ;
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K,V> nextEntry() {
            if (modCount != expectedModCount) //fail-fast
                throw new ConcurrentModificationException();
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();

            if ((next = e.next) == null) {
                Entry[] t = table;
                while (index < t.length && (next = t[index++]) == null)
                    ;
            }
            current = e;
            return e;
        }

        public void remove() {
            if (current == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Object k = current.key;
            current = null;
            HashMap7.this.removeEntryForKey(k);
            expectedModCount = modCount;
        }
    }

    private final class ValueIterator extends HashIterator<V> {
        public V next() {
            return nextEntry().value;
        }
    }

    private final class KeyIterator extends HashIterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    private final class EntryIterator extends HashIterator<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    // Subclass overrides these to alter behavior of views' iterator() method
    Iterator<K> newKeyIterator()   {
        return new KeyIterator();
    }
    Iterator<V> newValueIterator()   {
        return new ValueIterator();
    }
    Iterator<Map.Entry<K,V>> newEntryIterator()   {
        return new EntryIterator();
    }


    // Views

    private transient Set<Map.Entry<K,V>> entrySet = null;

    /**
     * 返回所有的key
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    private final class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return newKeyIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return HashMap7.this.removeEntryForKey(o) != null;
        }
        public void clear() {
            HashMap7.this.clear();
        }
    }

    /**
     * 返回所有的values
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    private final class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return newValueIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            HashMap7.this.clear();
        }
    }

    /**
     * 返回所有的Entry
     */
    public Set<Map.Entry<K,V>> entrySet() {
        return entrySet0();
    }

    private Set<Map.Entry<K,V>> entrySet0() {
        Set<Map.Entry<K,V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return newEntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<K,V> e = (Map.Entry<K,V>) o;
            Entry<K,V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }
        public boolean remove(Object o) {
            return removeMapping(o) != null;
        }
        public int size() {
            return size;
        }
        public void clear() {
            HashMap7.this.clear();
        }
    }


    /**
     * 下面的方法不再做重复复述. 有兴趣的可以参考: http://www.importnew.com/7656.html
     */
    static final int ALTERNATIVE_HASHING_THRESHOLD_DEFAULT = Integer.MAX_VALUE;

    /**
     * holds values which can't be initialized until after VM is booted.
     */
    private static class Holder {

        /**
         * Table capacity above which to switch to use alternative hashing.
         */
        static final int ALTERNATIVE_HASHING_THRESHOLD;

        static {
            String altThreshold = java.security.AccessController.doPrivileged(
                    new sun.security.action.GetPropertyAction(
                            "jdk.map.althashing.threshold"));

            int threshold;
            try {
                threshold = (null != altThreshold)
                        ? Integer.parseInt(altThreshold)
                        : ALTERNATIVE_HASHING_THRESHOLD_DEFAULT;

                // disable alternative hashing if -1
                if (threshold == -1) {
                    threshold = Integer.MAX_VALUE;
                }

                if (threshold < 0) {
                    throw new IllegalArgumentException("value must be positive integer.");
                }
            } catch(IllegalArgumentException failed) {
                throw new Error("Illegal value for 'jdk.map.althashing.threshold'", failed);
            }

            ALTERNATIVE_HASHING_THRESHOLD = threshold;
        }
    }

    /**
     * A randomizing value associated with this instance that is applied to
     * hash code of keys to make hash collisions harder to find. If 0 then
     * alternative hashing is disabled.
     */
    transient int hashSeed = 0;

    /**
     * Initialize the hashing mask value. We defer initialization until we
     * really need it.
     */
    final boolean initHashSeedAsNeeded(int capacity) {
        boolean currentAltHashing = hashSeed != 0;
        boolean useAltHashing = sun.misc.VM.isBooted() &&
                (capacity >= Holder.ALTERNATIVE_HASHING_THRESHOLD);
        boolean switching = currentAltHashing ^ useAltHashing;
        if (switching) {
            hashSeed = useAltHashing
                    ? sun.misc.Hashing.randomHashSeed(this)
                    : 0;
        }
        return switching;
    }
}
