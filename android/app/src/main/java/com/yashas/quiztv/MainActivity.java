package com.yashas.quiztv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String TAG = "QuizTV";
    private static final String QUIZ_URL =
            "https://ssrihari.github.io/yashas-games/quiz.html";

    private WebView webView;
    // The OK press used to launch the TV app can be delivered to the new
    // Activity as well. Ignore it briefly so it cannot skip the start screen.
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
        // The quiz is hosted live; do not let WebView keep an old navigation script.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setUserAgentString(settings.getUserAgentString() + " Android TV QuizApp");

        setContentView(webView);
        webView.loadUrl(QUIZ_URL);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            String key = keyFor(event.getKeyCode());
            if (key != null) {
                boolean isSelect = "select".equals(key);
                if (isSelect && SystemClock.uptimeMillis() < ignoreSelectUntil) {
                    Log.d(TAG, "Ignoring launch OK press");
                    return true;
                }
                sendBrowserKey(key);
                return true;
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                // The page closes an open picker/results screen first. If nothing
                // is open, finish the wrapper app.
                webView.evaluateJavascript(
                        "String(window.quizRemote && window.quizRemote.back && window.quizRemote.back());",
                        value -> {
                            if ("\"false\"".equals(value) || value == null) finish();
                        });
                return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_UP && isForwardedKey(event.getKeyCode())) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean isForwardedKey(int keyCode) {
        return keyFor(keyCode) != null;
    }

    private String keyFor(int keyCode) {
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
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_SPACE:
                return "select";
            default:
                return null;
        }
    }

    private void sendBrowserKey(String action) {
        Log.d(TAG, "Forwarding remote action: " + action);
        webView.evaluateJavascript(
                "window.quizRemote && window.quizRemote." + action + "();",
                null);
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
