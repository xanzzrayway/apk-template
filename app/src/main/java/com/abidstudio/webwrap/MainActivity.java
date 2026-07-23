package com.abidstudio.webwrap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int REQ_RUNTIME_PERMS = 1001;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());

        // Auto-grant permintaan izin dari konten web (kamera/mic buat getUserMedia)
        // kalau izin native-nya udah di-grant user lewat runtime permission.
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        // Jembatan JS <-> Android buat fitur yang gak bisa lewat web API biasa (overlay bubble)
        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");

        requestRuntimePermissionsIfDeclared();

        String targetUrl = getString(R.string.target_url);
        if (targetUrl != null && !targetUrl.trim().isEmpty()) {
            webView.loadUrl(targetUrl);
        } else {
            webView.loadUrl("file:///android_asset/www/index.html");
        }
    }

    /**
     * Minta permission runtime (dangerous permissions) yang emang udah dideklarasiin
     * di AndroidManifest.xml. Kalau suatu permission gak ada di manifest, Android
     * bakal otomatis nolak diem-diem tanpa nge-crash, jadi aman untuk selalu dicoba di sini.
     */
    private void requestRuntimePermissionsIfDeclared() {
        String[] candidates = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.BODY_SENSORS,
        };

        java.util.List<String> toRequest = new java.util.ArrayList<>();
        for (String perm : candidates) {
            if (isPermissionDeclared(perm) && ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(perm);
            }
        }
        if (Build.VERSION.SDK_INT >= 33 && isPermissionDeclared(Manifest.permission.POST_NOTIFICATIONS)
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), REQ_RUNTIME_PERMS);
        }
    }

    private boolean isPermissionDeclared(String permission) {
        try {
            String[] declared = getPackageManager()
                    .getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
            if (declared == null) return false;
            for (String p : declared) {
                if (permission.equals(p)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /** Dipanggil dari JS di halaman web lewat window.AndroidBridge.xxx() */
    public static class AndroidBridge {
        private final Context ctx;

        AndroidBridge(Context ctx) {
            this.ctx = ctx;
        }

        @JavascriptInterface
        public void requestOverlayPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + ctx.getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            }
        }

        @JavascriptInterface
        public boolean canDrawOverlay() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Settings.canDrawOverlays(ctx);
            }
            return true;
        }

        @JavascriptInterface
        public void showOverlay() {
            Intent intent = new Intent(ctx, OverlayService.class);
            intent.setAction(OverlayService.ACTION_SHOW);
            ctx.startService(intent);
        }

        @JavascriptInterface
        public void hideOverlay() {
            Intent intent = new Intent(ctx, OverlayService.class);
            intent.setAction(OverlayService.ACTION_HIDE);
            ctx.startService(intent);
        }
    }
}
