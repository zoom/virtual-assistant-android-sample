
# Zoom Virtual Agent Web SDK for Android Sample App

This README provides guidance on integrating the Zoom Virtual Agent Web SDK for Android, handling JavaScript callbacks, passing environment parameters, managing WebView scenarios, and processing download requests.

## Prerequisites

- Android 6.0 (API level 23) or later
- Android Studio Hedgehog | 2023.1.1 or later
- Basic knowledge of Java or Kotlin

## Setup Instructions

Clone the sample app repository from GitHub to your local machine, then open the project in Android Studio.

## Configure the App

Before running the app, you need to configure it with the required parameters.

### 1. Generate Campaign Chat URL

1. Navigate to the Campaign Management tab on the contact center management page.
2. Select a campaign, go to the settings tab, and find the "Full-page chat URL" section.
3. Click "Generate" to create the URL.

### 2. Integration Overview

To integrate on Android, support essential JavaScript functions and handle JavaScript native callback events in the `MainJavaActivity` or `MainKotlinActivity`.

### 3. How to Integrate

You can either:

- Use the core code provided in this document and integrate it into your project.
- Utilize the provided sample project on GitHub, available in both Java and Kotlin.

**Example Classes:**

- Java: `MainJavaActivity.java`
- Kotlin: `MainKotlinActivity.kt`

### 4. Handle JavaScript Events

#### Inject Environment Parameters

```kotlin
// JSON data to be injected into the WebView as JavaScript
private const val JSON_DATA = """
    window.zoomCampaignSdkConfig = window.zoomCampaignSdkConfig || {
        language: 'user language',
        firstName: 'user first name',
        lastName: 'user last name',
        nickName: 'user nick name',
        address: 'user address',
        company: 'user company',
        email: 'user email',
        phoneNumber: 'user phone number'
    };
"

// Inject JSON data as JavaScript into the WebView when the URL is loaded
private fun injectJsonDataFunction() {
    val js = "javascript: \$JSON_DATA;"
    binding.webView.loadUrl(js)
}
```

#### Handle Exit Callback Functions

```kotlin
// Inject JavaScript handler functions into the WebView when the URL is loaded
private fun injectJavaScriptFunction() {
    val js = """
        javascript: window.addEventListener('zoomCampaignSdk:ready', () => {
            if (window.zoomCampaignSdk) {
                window.zoomCampaignSdk.native = {
                    exitHandler: {
                        handle: function() {
                            \$EXIT_HANDLER_NAME.handleExit();
                        }
                    },
                    commonHandler: {
                        handle: function(e) {
                            \$COMMON_HANDLER_NAME.handleCommon(JSON.stringify(e));
                        }
                    }
                };
            }
        });
    """

    binding.webView.loadUrl(js)
}

// JavaScript interface method to handle exit commands
@JavascriptInterface
fun handleExit() {
    // Your code
}
```

#### Handle Handoff Events

```kotlin
// Inject handleHandoff function into the WebView when the URL is loaded
private fun injectHandoffFunction() {
    val js = """
        javascript: window.addEventListener('support_handoff', (e) => {
            \$SUPPORT_HANDOFF_HANDLER_NAME.handleHandoff(JSON.stringify(e.detail));
        });
    """

    binding.webView.loadUrl(js)
}

// JavaScript interface method to handle handoff commands
@JavascriptInterface
fun handleHandoff(e: String?) {
    // Your code
}
```

### 5. Integration Scenarios

#### Scenario 1: Embed Custom Website in WebView

Embed a customer's website in the Android WebView with links utilizing the ZVA WebSDK. Since JavaScript callback events are handled by the customer’s website, no additional native code is needed on Android.

#### Scenario 2: Trigger WebView from Native Button

Trigger native events to open the WebView displaying the Web Chat window. In this scenario, native code needs to handle JavaScript events received from the WebView.

### 6. Common Cases under Scenario 2

- **Case 1: Close Web Chat View**  
  Handle the callback event in `MainJavaActivity` or `MainKotlinActivity`, and close the current native web view window.
- **Case 2: Pass Native Parameters to Web Chat**  
  Pass parameters from the native app to the WebView context when opening a chat window.
- **Case 3: Open URL in the Current WebView (In-App)**  
  Handle navigation actions within the current WebView.
- **Case 4: Open URL in System Browser**  
  Open URLs from the WebView in the system browser.
- **Case 5: Open URL in new CustomTabs (In-App)**  
  Use the internal browser intents, such as CustomTabs, to open the ZVA Web SDK URL in the Android app.
- **Case 6: Dispatch Support Handoff Event**  
  Use JavaScript's handoff event to pass data from the web to the Android native app.

For a detailed integration guide, please refer to the [Zoom Virtual Agent Web SDK Documentation](https://developers.zoom.us/docs/virtual-agent/web/chat/).
