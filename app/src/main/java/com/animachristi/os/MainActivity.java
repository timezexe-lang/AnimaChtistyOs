package com.animachristi.os;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_REQUEST = 1001;
    private static final int APK_REQUEST = 1002;
    private Uri pendingApkUri;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = params.createIntent();
                try { startActivityForResult(i, FILE_REQUEST); }
                catch (Exception e) { fileCallback = null; toast("No file picker available"); return false; }
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme()) && "android_asset".equals(u.getHost())) return false;
                if ("https".equals(u.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                }
                return true;
            }
        });
        webView.addJavascriptInterface(new AnimaBridge(), "AnimaAndroid");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void chooseApk() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.android.package-archive");
        try { startActivityForResult(i, APK_REQUEST); }
        catch (Exception e) { toast("Unable to open APK picker"); }
    }

    private void installApk(Uri uri) {
        if (uri == null) return;
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            pendingApkUri = uri;
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + getPackageName()));
            startActivity(settings);
            toast("Allow installs from this app, then return here.");
            return;
        }
        try {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(uri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(install);
        } catch (Exception e) { toast("Android could not open this APK"); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (pendingApkUri != null && (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls())) {
            Uri uri = pendingApkUri; pendingApkUri = null; installApk(uri);
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == FILE_REQUEST && fileCallback != null) {
            Uri[] out = null;
            if (result == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int n = data.getClipData().getItemCount(); out = new Uri[n];
                    for (int x=0;x<n;x++) out[x] = data.getClipData().getItemAt(x).getUri();
                } else if (data.getData() != null) out = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(out); fileCallback = null;
        } else if (request == APK_REQUEST && result == RESULT_OK && data != null) installApk(data.getData());
    }

    public class AnimaBridge {
        @JavascriptInterface public String getPlatformInfo() { return "ANIMA_NATIVE_ANDROID"; }
        @JavascriptInterface public void openSystemSettings() { runOnUiThread(() -> startActivity(new Intent(Settings.ACTION_SETTINGS))); }
        @JavascriptInterface public void openHomeSettings() {
            runOnUiThread(() -> {
                try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            });
        }
        @JavascriptInterface public void chooseApk() { runOnUiThread(() -> MainActivity.this.chooseApk()); }
        @JavascriptInterface public String getInstalledApps() {
            JSONArray arr = new JSONArray();
            PackageManager pm = getPackageManager();
            for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
                try {
                    JSONObject o = new JSONObject();
                    o.put("name", pm.getApplicationLabel(ai).toString());
                    o.put("package", ai.packageName);
                    arr.put(o);
                } catch (Exception ignored) {}
            }
            return arr.toString();
        }
        @JavascriptInterface public void launchApp(String packageName) {
            runOnUiThread(() -> {
                try {
                    Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (i == null) { toast("App cannot be launched"); return; }
                    startActivity(i);
                } catch (Exception e) { toast("Unable to launch app"); }
            });
        }
        @JavascriptInterface public void openTermux() { launchApp("com.termux"); }
        @JavascriptInterface public void showMessage(String message) { runOnUiThread(() -> toast(message == null ? "" : message)); }
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
