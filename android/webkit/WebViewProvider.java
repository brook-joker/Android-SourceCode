/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.webkit;

import android.annotation.SystemApi;
import android.content.res.Configuration;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.http.SslCertificate;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.print.PrintDocumentAdapter;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebView.PictureListener;
import android.webkit.WebView.VisualStateCallback;


import java.io.BufferedWriter;
import java.io.File;
import java.util.Map;

/**
 * WebView后端提供程序接口：此接口是WebView实例的抽象后端;
 * 每个WebView对象都绑定到一个WebViewProvider对象，该对象实现该WebView的运行时行为。
 *
 * WebView backend provider interface: this interface is the abstract backend to a WebView
 * instance; each WebView object is bound to exactly one WebViewProvider object which implements
 * the runtime behavior of that WebView.
 *
 * 除非另有说明，否则所有方法必须按照{@link WebView}中的同名行为。
 * All methods must behave as per their namesake in {@link WebView}, unless otherwise noted.
 *
 *
 * 不属于公共API; 仅系统实现者需要。
 * @hide Not part of the public API; only required by system implementors.
 */
@SystemApi
public interface WebViewProvider {
    //-------------------------------------------------------------------------
    // Main interface for backend provider of the WebView class.
    //-------------------------------------------------------------------------
    /**
     * Initialize this WebViewProvider instance. Called after the WebView has fully constructed.
     * @param javaScriptInterfaces is a Map of interface names, as keys, and
     * object implementing those interfaces, as values.
     * @param privateBrowsing If true the web view will be initialized in private / incognito mode.
     */
    public void init(Map<String, Object> javaScriptInterfaces,
            boolean privateBrowsing);

    // Deprecated - should never be called
    public void setHorizontalScrollbarOverlay(boolean overlay);

    // Deprecated - should never be called
    public void setVerticalScrollbarOverlay(boolean overlay);

    // Deprecated - should never be called
    public boolean overlayHorizontalScrollbar();

    // Deprecated - should never be called
    public boolean overlayVerticalScrollbar();

    public int getVisibleTitleHeight();

    public SslCertificate getCertificate();

    public void setCertificate(SslCertificate certificate);

    public void savePassword(String host, String username, String password);

    public void setHttpAuthUsernamePassword(String host, String realm,
            String username, String password);

    public String[] getHttpAuthUsernamePassword(String host, String realm);

    /**
     * See {@link WebView#destroy()}.
     * 销毁WebView
     * 除了释放实现所持有的内部状态和资源之外，provider应该使它在WebView代理类上保留的所有引用都为null，并确保不再对其进行进一步的方法调用。
     * As well as releasing the internal state and resources held by the implementation,
     * the provider should null all references it holds on the WebView proxy class, and ensure
     * no further method calls are made to it.
     */
    public void destroy();

    public void setNetworkAvailable(boolean networkUp);

    public WebBackForwardList saveState(Bundle outState);

    public boolean savePicture(Bundle b, final File dest);

    public boolean restorePicture(Bundle b, File src);

    public WebBackForwardList restoreState(Bundle inState);

    /**
     * 加载网络URL
     * @param url
     * @param additionalHttpHeaders header
     */
    public void loadUrl(String url, Map<String, String> additionalHttpHeaders);

    public void loadUrl(String url);

    /**
     * post方式加载URL
     * @param url
     * @param postData 参数
     */
    public void postUrl(String url, byte[] postData);

    /**
     * 问题:1.loadData不能加载图片内容，如果要加载图片内容或者获得更强大的Web支持请使用loadDataWithBaseURL。
     *     2.使用loadData方法显示乱码。那是因为编码器设置错误导致的。我们知道String类型的数据主要是unicode编码，
     *     而WebView一般为了节省资源使用的是UTF-8编码，所以我们在loadData的时候要告诉方法怎样转码。
     *     即要告诉它要将unicode编码的内容转成UTF-8编码的内容。有些朋友虽然在loadData的时候设置了编码方式，
     *     但是还是显示乱码，这是因为还需要为WebView的text编码指定编码方式.
     *     3.loadData()中的html data中不能包含’#’, ‘%’, ‘\’, ‘?’四中特殊字符，出现这种字符就会出现解析错误，显示找不到网页还有部分html代码。
     *     需要如何处理呢？我们需要用UrlEncoder编码为%23, %25, %27, %3f 。
     *
     *     webView.loadData(URLEncoder.encode(data, "utf-8"), "text/html",  "utf-8");
     *
     *     这样一些背景效果什么的都不怎么好看了。不推荐。
     *     推荐做法
     *     webView.loadDataWithBaseURL(null,data, "text/html",  "utf-8", null);
     *
     * @param data  数据源
     * @param mimeType 数据类型
     * @param encoding 网页的编码方式
     */
    public void loadData(String data, String mimeType, String encoding);

    public void loadDataWithBaseURL(String baseUrl, String data,
            String mimeType, String encoding, String historyUrl);
    //Api 19 之后可以采用此方法之行 Js
    public void evaluateJavaScript(String script, ValueCallback<String> resultCallback);

    public void saveWebArchive(String filename);

    public void saveWebArchive(String basename, boolean autoname, ValueCallback<String> callback);
    //停止 WebView 当前加载。
    public void stopLoading();

    public void reload();
    // 判断 WebView 当前是否可以返回上一页
    public boolean canGoBack();
    // 回退到上一页
    public void goBack();
    //判断 WebView 当前是否可以向前一页
    public boolean canGoForward();
    //回退到前一页
    public void goForward();

    public boolean canGoBackOrForward(int steps);

    public void goBackOrForward(int steps);

    public boolean isPrivateBrowsingEnabled();

    public boolean pageUp(boolean top);

    public boolean pageDown(boolean bottom);

    public void insertVisualStateCallback(long requestId, VisualStateCallback callback);
    //在Android 4.3及其以上系统这个api被丢弃了， 并且这个api大多数情况下会有bug，经常不能清除掉之前的渲染数据。
    // 官方建议通过loadUrl("about:blank")来实现这个功能，阴雨需要重新加载一个页面自然时间会收到影响。
    public void clearView();

    public Picture capturePicture();

    public PrintDocumentAdapter createPrintDocumentAdapter(String documentName);

    public float getScale();

    public void setInitialScale(int scaleInPercent);

    public void invokeZoomPicker();

    public HitTestResult getHitTestResult();

    public void requestFocusNodeHref(Message hrefMsg);

    public void requestImageRef(Message msg);

    public String getUrl();

    public String getOriginalUrl();

    public String getTitle();

    public Bitmap getFavicon();

    public String getTouchIconUrl();

    public int getProgress();

    public int getContentHeight();

    public int getContentWidth();
    //该方法面向全局整个应用程序的webview，它会暂停所有webview的布局，解析，JavaScript Timer。当程序进入后台时，该方法的调用可以降低CPU功耗。
    public void pauseTimers();
    //恢复pauseTimers时的所有操作。（注：pauseTimers和resumeTimers方法必须一起使用，否则再使用其它场景下的WebView会有问题）
    public void resumeTimers();
    //类似活动生命周期，页面进入后台不可见状态 尽力尝试暂停可以暂停的任何处理，如动画和地理位置。 不会暂停JavaScript。 要全局暂停JavaScript，可使用pauseTimers
    public void onPause();
    //在调用onPause（）后，可以调用该方法来恢复WebView的运行
    public void onResume();

    public boolean isPaused();
    //释放内存，不过貌似不好用。
    public void freeMemory();
    // 清空网页访问留下的缓存数据。需要注意的时，由于缓存是全局的，所以只要是WebView用到的缓存都会被清空，即便其他地方也会使用到。
    // 该方法接受一个参数，从命名即可看出作用。若设为假，则只清空内存里的资源缓存，而不清空磁盘里的。
    public void clearCache(boolean includeDiskFiles);
    //清除自动完成填充的表单数据。需要注意的是，该方法仅仅清除当前表单域自动完成填充的表单数据，并不会清除WebView存储到本地的数据。
    public void clearFormData();
    //清除当前WebView访问的历史记录。
    public void clearHistory();
    //清除ssl信息
    public void clearSslPreferences();

    public WebBackForwardList copyBackForwardList();

    public void setFindListener(WebView.FindListener listener);

    public void findNext(boolean forward);

    public int findAll(String find);

    public void findAllAsync(String find);

    public boolean showFindDialog(String text, boolean showIme);
    //清除网页查找的高亮匹配字符。
    public void clearMatches();

    public void documentHasImages(Message response);

    public void setWebViewClient(WebViewClient client);

    public void setDownloadListener(DownloadListener listener);

    public void setWebChromeClient(WebChromeClient client);

    public void setPictureListener(PictureListener listener);

    /**
     * addJavascriptInterface(Object obj,String interfaceName)这个方法，该方法将一个java对象绑定到一个javascript对象中，
     * javascript对象名就是 interfaceName,比如说JSInterfaceDemo，作用域是Global。这样初始化webview后，
     * 在webview加载的页面中就可以直接通过 javascript:window.JSInterfaceDemo访问到绑定的java对象了。
     * 在HTML中如何调用呢，"window.JSInterfaceDemo.getResposeCode()"
     * 其中getResposeCode需要在JavaScriptInterfaceDemo中实现
     * @param obj
     * @param interfaceName
     */
    public void addJavascriptInterface(Object obj, String interfaceName);
    //删除interfaceName 对应的注入对象
    public void removeJavascriptInterface(String interfaceName);

    public WebMessagePort[] createWebMessageChannel();

    public void postMessageToMainFrame(WebMessage message, Uri targetOrigin);

    public WebSettings getSettings();

    public void setMapTrackballToArrowKeys(boolean setMap);

    public void flingScroll(int vx, int vy);

    public View getZoomControls();

    public boolean canZoomIn();

    public boolean canZoomOut();

    public boolean zoomBy(float zoomFactor);

    public boolean zoomIn();

    public boolean zoomOut();

    public void dumpViewHierarchyWithProperties(BufferedWriter out, int level);

    public View findHierarchyView(String className, int hashCode);

    //-------------------------------------------------------------------------
    // Provider internal methods
    //-------------------------------------------------------------------------

    /**
     * @return the ViewDelegate implementation. This provides the functionality to back all of
     * the name-sake functions from the View and ViewGroup base classes of WebView.
     */
    /* package */ ViewDelegate getViewDelegate();

    /**
     * @return a ScrollDelegate implementation. Normally this would be same object as is
     * returned by getViewDelegate().
     */
    /* package */ ScrollDelegate getScrollDelegate();

    /**
     * Only used by FindActionModeCallback to inform providers that the find dialog has
     * been dismissed.
     */
    public void notifyFindDialogDismissed();

    //-------------------------------------------------------------------------
    // View / ViewGroup delegation methods
    //-------------------------------------------------------------------------

    /**
     * Provides mechanism for the name-sake methods declared in View and ViewGroup to be delegated
     * into the WebViewProvider instance.
     * NOTE For many of these methods, the WebView will provide a super.Foo() call before or after
     * making the call into the provider instance. This is done for convenience in the common case
     * of maintaining backward compatibility. For remaining super class calls (e.g. where the
     * provider may need to only conditionally make the call based on some internal state) see the
     * {@link WebView.PrivateAccess} callback class.
     */
    // TODO: See if the pattern of the super-class calls can be rationalized at all, and document
    // the remainder on the methods below.
    interface ViewDelegate {
        public boolean shouldDelayChildPressedState();

        public void onProvideVirtualStructure(android.view.ViewStructure structure);

        public AccessibilityNodeProvider getAccessibilityNodeProvider();

        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info);

        public void onInitializeAccessibilityEvent(AccessibilityEvent event);

        public boolean performAccessibilityAction(int action, Bundle arguments);

        public void setOverScrollMode(int mode);

        public void setScrollBarStyle(int style);

        public void onDrawVerticalScrollBar(Canvas canvas, Drawable scrollBar, int l, int t,
                int r, int b);

        public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY);

        public void onWindowVisibilityChanged(int visibility);

        public void onDraw(Canvas canvas);

        public void setLayoutParams(LayoutParams layoutParams);

        public boolean performLongClick();

        public void onConfigurationChanged(Configuration newConfig);

        public InputConnection onCreateInputConnection(EditorInfo outAttrs);

        public boolean onDragEvent(DragEvent event);

        public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event);

        public boolean onKeyDown(int keyCode, KeyEvent event);

        public boolean onKeyUp(int keyCode, KeyEvent event);

        public void onAttachedToWindow();

        public void onDetachedFromWindow();

        public void onVisibilityChanged(View changedView, int visibility);

        public void onWindowFocusChanged(boolean hasWindowFocus);

        public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect);

        public boolean setFrame(int left, int top, int right, int bottom);

        public void onSizeChanged(int w, int h, int ow, int oh);

        public void onScrollChanged(int l, int t, int oldl, int oldt);

        public boolean dispatchKeyEvent(KeyEvent event);

        public boolean onTouchEvent(MotionEvent ev);

        public boolean onHoverEvent(MotionEvent event);

        public boolean onGenericMotionEvent(MotionEvent event);

        public boolean onTrackballEvent(MotionEvent ev);

        public boolean requestFocus(int direction, Rect previouslyFocusedRect);

        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec);

        public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate);
        //设置背景色
        public void setBackgroundColor(int color);
        //设置硬件加速、软件加速
        public void setLayerType(int layerType, Paint paint);

        public void preDispatchDraw(Canvas canvas);

        public void onStartTemporaryDetach();

        public void onFinishTemporaryDetach();

        public void onActivityResult(int requestCode, int resultCode, Intent data);

        public Handler getHandler(Handler originalHandler);

        public View findFocus(View originalFocusedView);
    }

    interface ScrollDelegate {
        // These methods are declared protected in the ViewGroup base class. This interface
        // exists to promote them to public so they may be called by the WebView proxy class.
        // TODO: Combine into ViewDelegate?
        /**
         * See {@link android.webkit.WebView#computeHorizontalScrollRange}
         */
        public int computeHorizontalScrollRange();

        /**
         * See {@link android.webkit.WebView#computeHorizontalScrollOffset}
         */
        public int computeHorizontalScrollOffset();

        /**
         * See {@link android.webkit.WebView#computeVerticalScrollRange}
         */
        public int computeVerticalScrollRange();

        /**
         * See {@link android.webkit.WebView#computeVerticalScrollOffset}
         */
        public int computeVerticalScrollOffset();

        /**
         * See {@link android.webkit.WebView#computeVerticalScrollExtent}
         */
        public int computeVerticalScrollExtent();

        /**
         * See {@link android.webkit.WebView#computeScroll}
         */
        public void computeScroll();
    }
}
