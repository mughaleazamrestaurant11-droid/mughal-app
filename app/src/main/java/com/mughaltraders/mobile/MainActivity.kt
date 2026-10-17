package com.mughaltraders.mobile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Mughal Traders - mobile follower (Kotlin).
 *
 * The phone is ALWAYS a client of the shop's MAIN PC, exactly like a client
 * PC: it holds no shop data of its own. It mirrors the main PC's screen live
 * over the same LAN/WiFi router, and it refuses to keep running stale when
 * the main PC goes off - it shows a waiting screen and opens the shop by
 * itself the moment the main PC answers again.
 *
 * Printing from the phone happens through the main PC's printer, same as a
 * client PC.
 */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var webView: WebView
    private lateinit var offlinePanel: View
    private lateinit var setupPanel: View
    private lateinit var splashPanel: View
    private lateinit var offlineStatus: TextView
    private lateinit var setupStatus: TextView
    private lateinit var serverInput: EditText

    private val io = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())

    private var server = ""      // "192.168.1.10:8765" - the MAIN PC
    private var checks = 0       // how many times the waiting screen asked

    // Branded splash: shown from cold start so the phone never flashes white
    // while the WebView is still drawing the shop page.
    private val minSplashMs = 1300L   // let the logo actually be seen
    private val maxSplashMs = 9000L   // never trap the user behind it
    private var splashAt = 0L
    private var contentReady = false  // the shop page has really drawn itself
    private var splashGone = false

    // Waits for the main PC and opens the shop by itself when it answers.
    private lateinit var waiter: Runnable

    // Guards the whole session: the phone never keeps working on a dead main PC.
    private lateinit var keepAlive: Runnable

    // Holds the branded splash until the shop page has really drawn itself.
    private lateinit var splashPoller: Runnable

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("mughal_mobile", MODE_PRIVATE)
        webView = findViewById(R.id.web_view)
        offlinePanel = findViewById(R.id.offline_panel)
        setupPanel = findViewById(R.id.setup_panel)
        splashPanel = findViewById(R.id.splash_panel)
        offlineStatus = findViewById(R.id.offline_status)
        setupStatus = findViewById(R.id.setup_status)
        serverInput = findViewById(R.id.server_input)

        // Anything the WebView paints late is navy, never white.
        webView.setBackgroundColor(Color.parseColor("#0F172A"))

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true          // the shop system keeps its working copy here
        s.cacheMode = WebSettings.LOAD_NO_CACHE
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) ui.post { showOffline() }
            }
        }

        waiter = object : Runnable {
            override fun run() {
                checks += 1
                offlineStatus.text = "checked " + checks + " time" +
                    (if (checks == 1) "" else "s") + " - still waiting..."
                io.execute {
                    val up = mainAnswering()
                    ui.post {
                        if (isFinishing) return@post
                        if (up) {
                            checks = 0
                            openShop()
                        } else {
                            ui.postDelayed(waiter, 4000)
                        }
                    }
                }
            }
        }

        keepAlive = object : Runnable {
            override fun run() {
                io.execute {
                    val up = server.isNotEmpty() && mainAnswering()
                    ui.post {
                        if (isFinishing) return@post
                        if (!up && offlinePanel.visibility != View.VISIBLE) showOffline()
                        ui.postDelayed(keepAlive, 20000)
                    }
                }
            }
        }

        // Holds the splash until the shop page has really drawn itself (or
        // the setup / waiting screen is up), with a hard stop so the user is
        // never stuck behind the logo.
        splashPoller = object : Runnable {
            override fun run() {
                if (splashGone || isFinishing) return
                try {
                    // true once React has put the app into #root
                    webView.evaluateJavascript(
                        "(document.getElementById('root') || { childElementCount: 0 })" +
                            ".childElementCount > 0",
                        ValueCallback { v -> contentReady = v == "true" }
                    )
                } catch (ignored: Exception) {
                }
                val elapsed = SystemClock.uptimeMillis() - splashAt
                val panelUp = setupPanel.visibility == View.VISIBLE ||
                    offlinePanel.visibility == View.VISIBLE
                if ((panelUp || contentReady) && elapsed > minSplashMs) fadeSplash()
                else if (elapsed > maxSplashMs) fadeSplash()
                else ui.postDelayed(this, 250)
            }
        }

        findViewById<Button>(R.id.btn_connect).setOnClickListener { connectTried() }
        findViewById<Button>(R.id.btn_try_now).setOnClickListener { startWaiting() }
        findViewById<Button>(R.id.btn_change).setOnClickListener { showSetup() }

        splashAt = SystemClock.uptimeMillis()
        ui.post(splashPoller)

        server = prefs.getString("server", "") ?: ""
        if (server.isEmpty()) showSetup() else openShop()
    }

    private fun fadeSplash() {
        if (splashGone) return
        splashGone = true
        ui.removeCallbacks(splashPoller)
        splashPanel.animate().alpha(0f).setDuration(280)
            .withEndAction { splashPanel.visibility = View.GONE }.start()
    }

    // -------- first-run setup: type the main PC address once --------

    private fun connectTried() {
        val typed = serverInput.text.toString().trim()
        if (typed.isEmpty()) {
            setupStatus.text = "Type the main PC address first"
            return
        }
        val candidate = normalize(typed)
        setupStatus.text = "Connecting to the main PC..."
        io.execute {
            val ok = mainAnswering(candidate)
            ui.post {
                if (isFinishing) return@post
                if (ok) {
                    server = candidate
                    prefs.edit().putString("server", candidate).apply()
                    openShop()
                } else {
                    setupStatus.text = "Cannot reach the main PC at " + candidate +
                        ".\nCheck it is switched on and this phone is on the same WiFi."
                }
            }
        }
    }

    /** "192.168.1.10" -> "192.168.1.10:8765" (the shop port) */
    private fun normalize(raw: String): String {
        var s = raw.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        if (!s.contains(":")) s += ":8765"
        return s
    }

    // -------- screens --------

    private fun openShop() {
        ui.removeCallbacks(waiter)
        setupPanel.visibility = View.GONE
        offlinePanel.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl("http://" + server + "/")
        ui.removeCallbacks(keepAlive)
        ui.postDelayed(keepAlive, 20000)
    }

    private fun showOffline() {
        if (offlinePanel.visibility == View.VISIBLE) return
        webView.visibility = View.GONE
        setupPanel.visibility = View.GONE
        offlinePanel.visibility = View.VISIBLE
        startWaiting()
    }

    private fun startWaiting() {
        checks = 0
        ui.removeCallbacks(waiter)
        ui.post(waiter)
    }

    private fun showSetup() {
        ui.removeCallbacks(waiter)
        ui.removeCallbacks(keepAlive)
        webView.stopLoading()
        webView.visibility = View.GONE
        offlinePanel.visibility = View.GONE
        setupPanel.visibility = View.VISIBLE
        serverInput.setText(server)
        setupStatus.text = ""
    }

    // -------- talking to the MAIN PC --------

    /** Is the main PC answering? Used for the first setup and while running. */
    private fun mainAnswering(base: String = server): Boolean {
        if (base.isEmpty()) return false
        return try {
            val conn = URL("http://" + base + "/api/health").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            body.contains("mughal-traders")
        } catch (e: Exception) {
            false
        }
    }

    // -------- lifecycle --------

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(waiter)
        ui.removeCallbacks(keepAlive)
        ui.removeCallbacks(splashPoller)
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        if (!splashGone) ui.post(splashPoller)
        if (server.isNotEmpty()) {
            if (offlinePanel.visibility == View.VISIBLE) startWaiting()
            ui.postDelayed(keepAlive, 20000)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
