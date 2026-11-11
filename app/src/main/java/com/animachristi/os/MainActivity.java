
package com.animachristi.os;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(new AnimaAndroid(), "AnimaAndroid");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                if (url.startsWith("https://")) {
                    try {
                        startActivity(new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)));
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Unable to open link",
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Select a file"),
                            FILE_REQUEST);
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this,
                            "File picker unavailable",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST && fileCallback != null) {
            Uri[] results = null;

            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];

                    for (int i = 0; i < count; i++) {
                        results[i] =
                                data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }

            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    public class AnimaAndroid {

        @android.webkit.JavascriptInterface
        public String getPlatformInfo() {
            return "ANIMA CHRISTI OS - Android";
        }

        @android.webkit.JavascriptInterface
        public void openSystemSettings() {
            runOnUiThread(() -> {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            });
        }

        @android.webkit.JavascriptInterface
        public void openTermux() {
            runOnUiThread(() -> {
                Intent intent = getPackageManager()
                        .getLaunchIntentForPackage("com.termux");

                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Termux is not installed",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void showMessage(String message) {
            runOnUiThread(() ->
                    Toast.makeText(MainActivity.this,
                            message,
                            Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
