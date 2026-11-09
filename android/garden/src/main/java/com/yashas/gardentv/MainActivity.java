package com.yashas.gardentv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Android TV wrapper for GARDEN GUARD (garden.html). Same shape as the quiz
 * wrapper: the game stays HTML and is loaded live from GitHub Pages; this
 * class turns remote buttons into calls on window.gardenRemote.
 */
public class MainActivity extends Activity {
    private static final String TAG = "GardenTV";
    private static final String GARDEN_URL =
            "https://ssrihari.github.io/yashas-games/garden.html";

    private WebView webView;
    // The OK press used to launch the TV app can be delivered to the new
    // Activity as well. Ignore it briefly so it cannot pick a level by itself.
    private long ignoreSelectUntil;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ignoreSelectUntil = SystemClock.uptimeMillis() + 1500;
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        WebView.setWebContentsDebuggingEnabled(true);
        webView = new WebView(this);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        // The game is hosted live; do not let WebView keep an old copy.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setUserAgentString(settings.getUserAgentString() + " Android TV GardenApp");

        setContentView(webView);
        webView.loadUrl(startUrl());
    }

    // Debug builds accept a "url" extra so a local copy can be tried first:
    // am start -n com.yashas.gardentv/.MainActivity --es url http://10.0.2.2:8000/garden.html
    private String startUrl() {
        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        String override = getIntent().getStringExtra("url");
        if (debuggable && override != null && !override.isEmpty()) {
            Log.d(TAG, "Loading override URL " + override);
            return override;
        }
        return GARDEN_URL;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        String action = actionFor(keyCode);
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // Holding a D-pad direction keeps moving the cursor; every other
            // button fires once per press.
            boolean repeatable = isDirection(keyCode);
            if (action != null && (event.getRepeatCount() == 0 || repeatable)) {
                if ("select".equals(action) && SystemClock.uptimeMillis() < ignoreSelectUntil) {
                    Log.d(TAG, "Ignoring launch OK press");
                    return true;
                }
                sendRemote(action);
                return true;
            }
            if (action != null) return true;
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                // The page closes a pop-up or leaves the game first. Only on
                // the level pick (or if the page never loaded) does Back exit.
                webView.evaluateJavascript(
                        "String(window.gardenRemote && window.gardenRemote.back && window.gardenRemote.back());",
                        value -> {
                            if (!"\"true\"".equals(value)) finish();
                        });
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) return true;
        }
        if (event.getAction() == KeyEvent.ACTION_UP
                && (action != null || keyCode == KeyEvent.KEYCODE_BACK)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean isDirection(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    private String actionFor(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return "left";
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return "right";
            case KeyEvent.KEYCODE_DPAD_UP:
                return "up";
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return "down";
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                return "select";
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_SPACE:
                return "playPause";
            default:
                return null;
        }
    }

    private void sendRemote(String action) {
        Log.d(TAG, "Forwarding remote action: " + action);
        webView.evaluateJavascript(
                "window.gardenRemote && window.gardenRemote." + action + "();",
                null);
    }

    @Override
    protected void onPause() {
        // Leaving the app (Home button, screensaver) pauses a running wave.
        if (webView != null) {
            webView.evaluateJavascript(
                    "window.gardenRemote && window.gardenRemote.hold && window.gardenRemote.hold();",
                    null);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
