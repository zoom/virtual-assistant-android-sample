package us.zoom.livesdkdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONObject;

import us.zoom.livesdkdemo.databinding.ActivityMainBinding;

public class MainJavaActivity extends AppCompatActivity {

    // View binding for the activity
    private ActivityMainBinding binding;

    // Choose file listener
    private ValueCallback<Uri[]> mFilePathCallback;

    // URI to be loaded in the WebView
    private String mUri;

    // Flag to determine if URL should be opened in system browser
    private boolean mOpenURLInSystemBrowser = false;
    private boolean mUseJSURLHandler = false;

    public static final String ARG_URL = "arg_url";
    public static final String ARG_OPEN_URL_IN_SYSTEM_BROWSER = "arg_open_url_in_system_browser";
    public static final String ARG_USE_JS_URL_HANDLER = "arg_use_js_url_handler";
    private static final int REQUEST_CODE_INTENT = 1;

    // JSON data to be injected into the WebView as JavaScript
    private static final String JSON_DATA = "window.zoomCampaignSdkConfig = window.zoomCampaignSdkConfig || {" +
            "language: 'user language', " +
            "firstName: 'ZVA DEMO', " +
            "lastName: 'user last name', " +
            "nickName: 'user nick name', " +
            "address: 'user address', " +
            "company: 'user company', " +
            "email: 'zva@demo.com', " +
            "phoneNumber: 'user phone number'" +
            "};";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve URL and browser flag from intent extras
        mUri = getIntent().getStringExtra(ARG_URL);
        mOpenURLInSystemBrowser = getIntent().getBooleanExtra(ARG_OPEN_URL_IN_SYSTEM_BROWSER, false);
        mUseJSURLHandler = getIntent().getBooleanExtra(ARG_USE_JS_URL_HANDLER, false);

        // Setup the WebView with necessary configurations
        setupWebview();

        // Load the initial URL if available
        if (mUri != null) {
            binding.webView.loadUrl(mUri);
        }
    }

    @Override
    public void onBackPressed() {
        // Override this method to allow the user to go back inside the WebView
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        // Remove JavaScript interfaces when the activity is destroyed
        binding.webView.removeJavascriptInterface(Constants.EXIT_HANDLER_NAME);
        binding.webView.removeJavascriptInterface(Constants.COMMON_HANDLER_NAME);
        binding.webView.removeJavascriptInterface(Constants.SUPPORT_HANDOFF_HANDLER_NAME);
        super.onDestroy();
    }

    private void setupWebview() {
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.equals(mUri)) {
                    injectJsonDataFunction();
                    injectJavaScriptFunction();
                    injectHandoffFunction();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (mUseJSURLHandler) {
                    return super.shouldOverrideUrlLoading(view, request);
                } else {
                    if (request != null) {
                        processUrl(request.getUrl().toString());
                    }
                    return true;
                }
            }
        });

        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                return MainJavaActivity.this.onShowFileChooser(webView, filePathCallback, fileChooserParams);
            }
        });

        // Set a download listener to handle file downloads
        binding.webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> downloadFileFromUrl(url));

        // Add JavaScript interfaces for interaction with the WebView content
        binding.webView.addJavascriptInterface(this, Constants.EXIT_HANDLER_NAME);
        binding.webView.addJavascriptInterface(this, Constants.COMMON_HANDLER_NAME);
        binding.webView.addJavascriptInterface(this, Constants.SUPPORT_HANDOFF_HANDLER_NAME);
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
    private void injectJavaScriptFunction() {

        String commonHandlerString = mUseJSURLHandler ?
                "commonHandler: {\n" +
                "    handle: function(e) {\n" +
                "        " + Constants.COMMON_HANDLER_NAME + ".handleCommon(JSON.stringify(e));\n" +
                "    }\n" +
                "}\n" : "";

        String js = "javascript: window.addEventListener('zoomCampaignSdk:ready', () => {\n" +
                "                if (window.zoomCampaignSdk) {\n" +
                "                    window.zoomCampaignSdk.native = {\n" +
                "                        exitHandler: {\n" +
                "                            handle: function() {\n" +
                "                                " + Constants.EXIT_HANDLER_NAME + ".handleExit();\n" +
                "                            }\n" +
                "                        },\n" + commonHandlerString +
                "                    };\n" +
                "                }\n" +
                "});";

        binding.webView.loadUrl(js);
    }

    /**
     * Handles the support_handoff command. For this event, the JavaScript context will archive all the
     * messages into a JSON string and post the JSON string to the native logic via the
     * message.body. Customize your actions here to handle the JSON string as needed.
     */
    private void injectHandoffFunction() {
        String js = "javascript: window.addEventListener('support_handoff', (e) => {\n" +
                "    " + Constants.SUPPORT_HANDOFF_HANDLER_NAME + ".handleHandoff(JSON.stringify(e.detail));\n" +
                "});";

        binding.webView.loadUrl(js);
    }

    // Inject JSON data as JavaScript into the WebView
    private void injectJsonDataFunction() {
        String js = "javascript: " + JSON_DATA + ";";
        binding.webView.loadUrl(js);
    }

    // JavaScript interface method to handle exit commands
    @JavascriptInterface
    public void handleExit() {
        runOnUiThread(() -> finish());
    }

    // JavaScript interface method to handle common commands
    @JavascriptInterface
    public void handleCommon(String jsonString) {
        if (jsonString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                String cmd = jsonObject.getString(Constants.ZMCCWebKitMessageCmdKey);
                if (Constants.ZMCCWebkitMessageCmdType_OpenURL.equals(cmd)) {
                    processUrl(jsonObject.getString(Constants.ZMCCWebkitMessageValueKey));
                }
            } catch (Exception e) {
                Log.e("MainJavaActivity", "Failed to parse JSON", e);
            }
        }
    }

    private void processUrl(String url) {
        //Customize your actions here to handle the URL as needed.
        if (mOpenURLInSystemBrowser) {
            // Case 1: open url in system browser
            openUriInNewBrowser(Uri.parse(url));
        } else {
            // Case 2: open url in new tab but same application, use CustomTabsIntent
            openUriInCustomTabs(Uri.parse(url));
        }
    }

    // Open URI in the system's default browser
    private void openUriInNewBrowser(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(uri);
        startActivity(intent);
    }

    // Open URI in a custom tab within the application
    private void openUriInCustomTabs(Uri uri) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .setUrlBarHidingEnabled(true)
                .setShowTitle(true)
                .build();
        intent.launchUrl(this, uri);
    }

    // JavaScript interface method to handle handoff commands
    @JavascriptInterface
    public void handleHandoff(String events) {
        // Customize your actions here to handle the JSON string as needed.
        Log.i("MainJavaActivity", "handleHandoff: " + events);
        runOnUiThread(() -> new AlertDialog.Builder(MainJavaActivity.this)
                .setMessage(events)
                .setPositiveButton("OK", null)
                .show());
    }

    // Handles file chooser for web uploads
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;
        Intent intent = fileChooserParams.createIntent();
        startActivityForResult(intent, REQUEST_CODE_INTENT);
        return true;
    }

    // Handle file downloads from the WebView
    private void downloadFileFromUrl(String url) {
        // Handle the download request
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    // Handle the result of the file chooser intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_INTENT) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            }
        }
    }
}