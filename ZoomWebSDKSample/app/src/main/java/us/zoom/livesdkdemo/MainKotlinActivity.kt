package us.zoom.livesdkdemo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import org.json.JSONObject
import us.zoom.livesdkdemo.Constants.COMMON_HANDLER_NAME
import us.zoom.livesdkdemo.Constants.EXIT_HANDLER_NAME
import us.zoom.livesdkdemo.Constants.SUPPORT_HANDOFF_HANDLER_NAME
import us.zoom.livesdkdemo.Constants.ZMCCWebKitMessageCmdKey
import us.zoom.livesdkdemo.Constants.ZMCCWebkitMessageCmdType_OpenURL
import us.zoom.livesdkdemo.Constants.ZMCCWebkitMessageValueKey
import us.zoom.livesdkdemo.databinding.ActivityMainBinding

class MainKotlinActivity : AppCompatActivity() {

    // View binding for the activity
    private lateinit var binding: ActivityMainBinding

    // Choose file listener
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    // URI to be loaded in the WebView
    private var mUri: String? = null

    // Flag to determine if URL should be opened in system browser
    private var mOpenURLInSystemBrowser = false
    private var mUseJSURLHandler = false

    companion object {

        // Keys for passing URL and browser flag through intents
        public const val ARG_URL = "arg_url"
        public const val ARG_OPEN_URL_IN_SYSTEM_BROWSER = "arg_open_url_in_system_browser"
        public const val ARG_USE_JS_URL_HANDLER = "arg_use_js_url_handler"
        private const val REQUEST_CODE_INTENT = 1

        // JSON data to be injected into the WebView as JavaScript
        private const val JSON_DATA = """
            window.zoomCampaignSdkConfig = window.zoomCampaignSdkConfig || {
                language: 'user language', 
                firstName: 'ZVA DEMO', 
                lastName: 'user last name', 
                nickName: 'user nick name', 
                address: 'user address', 
                company: 'user company', 
                email: 'zva@demo.com', 
                phoneNumber: 'user phone number'
            };
            """
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve URL and browser flag from intent extras
        mUri = intent?.getStringExtra(ARG_URL)
        mOpenURLInSystemBrowser =
            intent?.getBooleanExtra(ARG_OPEN_URL_IN_SYSTEM_BROWSER, false) ?: false
        mUseJSURLHandler =
            intent?.getBooleanExtra(ARG_USE_JS_URL_HANDLER, false) ?: false

        setupWebview()

        // Load the initial URL if available
        mUri?.let { binding.webView.loadUrl(it) }
    }

    // Override this method to allow user go back inside webview.
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        binding.webView.removeJavascriptInterface(EXIT_HANDLER_NAME)
        binding.webView.removeJavascriptInterface(COMMON_HANDLER_NAME)
        binding.webView.removeJavascriptInterface(SUPPORT_HANDOFF_HANDLER_NAME)
        super.onDestroy()
    }

    // Setup WebView settings and JavaScript interfaces
    private fun setupWebview() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url == mUri) {
                        injectJsonDataFunction()
                        injectJavaScriptFunction()
                        injectHandoffFunction()
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    if (mUseJSURLHandler) {
                        return super.shouldOverrideUrlLoading(view, request)
                    } else {
                        request?.url?.also {
                            processUrl(it.toString())
                        }
                        return true
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    return true
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    return this@MainKotlinActivity.onShowFileChooser(
                        webView, filePathCallback, fileChooserParams
                    )
                }
            }

            binding.webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                downloadFileFromUrl(url)
            }
        }

        // Add JavaScript interfaces for interaction with the WebView content
        binding.webView.addJavascriptInterface(this, EXIT_HANDLER_NAME)
        binding.webView.addJavascriptInterface(this, COMMON_HANDLER_NAME)
        binding.webView.addJavascriptInterface(this, SUPPORT_HANDOFF_HANDLER_NAME)
    }

    /**
     * Creates an exit message handler to handle the case where the JavaScript context needs
     * to pop to the previous page in the host app. Also creates a common message handler to handle
     * commands from the JavaScript context. The command message will be a JSON formatted string, and it will
     * contain keys "cmd" and "value". Both values of the keys are string types.
     * The value can be a simple string or a JSON string, depending on the command type.
     * Currently, we support one command with a JSON format like {"cmd":"openURL", "value":"https://zoom.us"}.
     * When this command type message is received, developers should start an activity with a browser to
     * load the URL within the value, such as "https://zoom.us".
     */
    private fun injectJavaScriptFunction() {
        val commonHandlerScript = if (mUseJSURLHandler) {
            """
               ,commonHandler: {
                    handle: function(e) {
                        $COMMON_HANDLER_NAME.handleCommon(JSON.stringify(e));
                    }
                }
            """
        } else ""

        val js = """
            javascript: window.addEventListener('zoomCampaignSdk:ready', () => {
                if (window.zoomCampaignSdk) {
                    window.zoomCampaignSdk.native = {
                        exitHandler: {
                            handle: function() {
                                $EXIT_HANDLER_NAME.handleExit();
                            }
                        }$commonHandlerScript
                    };
                }
            });
        """.trimIndent()

        binding.webView.loadUrl(js)
    }

    /**
     * Handles the support_handoff command. For this event, the JavaScript context will archive all the
     * messages into a JSON string and post the JSON string to the native logic via the
     * message.body. Here, developers should handle the JSON string as needed.
     */
    private fun injectHandoffFunction() {
        val js = """
            javascript: window.addEventListener('support_handoff', (e) => {
                $SUPPORT_HANDOFF_HANDLER_NAME.handleHandoff(JSON.stringify(e.detail));
            });
        """.trimIndent()

        binding.webView.loadUrl(js)
    }

    // Inject JSON data as JavaScript into the WebView
    private fun injectJsonDataFunction() {
        val js = "javascript: $JSON_DATA;"
        binding.webView.loadUrl(js)
    }

    // JavaScript interface method to handle exit commands
    @JavascriptInterface
    fun handleExit() {
        Handler(Looper.getMainLooper()).post {
            finish()
        }
    }

    // JavaScript interface method to handle common commands
    @JavascriptInterface
    fun handleCommon(jsonString: String?) {
        jsonString?.let {
            val jsonObject = JSONObject(jsonString)
            val cmd = jsonObject.getString(ZMCCWebKitMessageCmdKey)
            if (cmd == ZMCCWebkitMessageCmdType_OpenURL) { //Handle OpenURL command
                processUrl(jsonObject.getString(ZMCCWebkitMessageValueKey))
            }
        }
    }

    private fun processUrl(url: String?) {
        url?.also {
            //Customize your actions here to handle the URL as needed.
            if (mOpenURLInSystemBrowser) {
                // Case 1: open url in system browser
                openUriInNewBrowser(Uri.parse(it))
            } else {
                // Case 2: open url in new tab but same application, use CustomTabsIntent
                openUriInCustomTabs(Uri.parse(it))
            }
        }
    }

    // Open URI in the system's default browser
    private fun openUriInNewBrowser(uri: Uri) {
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setAction(Intent.ACTION_VIEW)
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.setData(uri)
        startActivity(intent)
    }

    // Open URI in a custom tab within the application
    private fun openUriInCustomTabs(uri: Uri) {
        val intent = CustomTabsIntent.Builder()
            .setUrlBarHidingEnabled(true)
            .setShowTitle(true)
            .build()
        intent.launchUrl(this, uri)
    }

    // JavaScript interface method to handle handoff commands
    @JavascriptInterface
    fun handleHandoff(events: String?) {

        //Customize your actions here to handle the JSON string as needed.
        Log.i("MainKotlinActivity", "handleHandoff: $events")
        runOnUiThread {
            AlertDialog.Builder(this)
                .setMessage(events)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // Handles file chooser for web uploads
    fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        mFilePathCallback?.onReceiveValue(null)
        mFilePathCallback = filePathCallback
        fileChooserParams?.createIntent()?.let {
            startActivityForResult(it, REQUEST_CODE_INTENT)
        }
        return true
    }

    // Handle file downloads from the WebView
    private fun downloadFileFromUrl(url: String?) {
        // Handle the download request
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    // Handle the result of the file chooser intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_INTENT -> {
                val results = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                mFilePathCallback?.onReceiveValue(results)
                mFilePathCallback = null
            }
        }
    }
}