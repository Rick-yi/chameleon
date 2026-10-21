package com.chameleon.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * App 壳：只做 WebView 容器
 *
 * 职责（刻意保持最小）：
 *  1. 加载 H5（默认 {h5BaseUrl}{h5EntryPath}）
 *  2. 需要免登录时，把 token 通过 ?token=xxx 传给 H5（前端会落地为登录态并从地址栏移除）
 *  3. 返回键：能后退就后退；到首页后返回退出
 *  4. 文件选择（拍照/选文件）与下载
 *
 * 所有业务逻辑都在 H5 里，壳不感知业务。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    /** 外部返回时是否已回不到首页 */
    private var exitConfirmAt = 0L

    /** 文件选择回调 */
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        setupWebView()
        loadHome()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // 允许混合内容（内网 http 调试）
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
        }
        CookieManager.getInstance().setAcceptCookie(true)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                // 外部链接（如拨号、地图）交给系统处理
                if (url.scheme == "tel" || url.scheme == "mailto" || url.scheme == "sms") {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    return true
                }
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                android.util.Log.d("H5", "${msg.message()} @${msg.sourceId()}:${msg.lineNumber()}")
                return true
            }

            // 选择文件（拍照 / 相册 / 文件）
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback
                return try {
                    val intent = fileChooserParams.createIntent()
                    startActivityForResult(intent, REQUEST_FILE)
                    true
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }
        }
    }

    /** 加载 H5 首页；extra 里可带 token 实现免登录 */
    private fun loadHome() {
        val token = intent?.getStringExtra(EXTRA_TOKEN).orEmpty()
        val url = buildString {
            append(BuildConfig.H5_BASE_URL.trimEnd('/'))
            append(BuildConfig.H5_ENTRY_PATH)
            if (token.isNotBlank()) {
                append("login?token=").append(Uri.encode(token))
            }
        }
        webView.loadUrl(url)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE) {
            val callback = filePathCallback
            filePathCallback = null
            callback?.onReceiveValue(
                if (resultCode == RESULT_OK) {
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                } else {
                    null
                }
            )
        }
    }

    /** 返回键：优先后退，回不到首页时两次退出 */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val now = System.currentTimeMillis()
            if (now - exitConfirmAt < 2000) {
                finish()
            } else {
                exitConfirmAt = now
                android.widget.Toast.makeText(this, R.string.exit_hint, android.widget.Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    companion object {
        private const val REQUEST_FILE = 1001
        const val EXTRA_TOKEN = "token"
    }
}
