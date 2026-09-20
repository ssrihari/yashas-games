package com.yashas.quiztv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String QUIZ_URL =
            "https://ssrihari.github.io/yashas-games/quiz.html";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

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
        settings.setUserAgentString(settings.getUserAgentString() + " Android TV QuizApp");

        setContentView(webView);
        webView.loadUrl(QUIZ_URL);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            String key = keyFor(event.getKeyCode());
            if (key != null) {
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
                return "ArrowLeft";
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return "ArrowRight";
            case KeyEvent.KEYCODE_DPAD_UP:
                return "ArrowUp";
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return "ArrowDown";
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                return "Enter";
            case KeyEvent.KEYCODE_SPACE:
                return " ";
            default:
                return null;
        }
    }

    private void sendBrowserKey(String key) {
        String escapedKey = key.equals(" ") ? " " : key;
        String code = key.equals(" ") ? "Space" : key;
        String javascript = "window.dispatchEvent(new KeyboardEvent('keydown', "
                + "{key:'" + escapedKey + "', code:'" + code + "', bubbles:true}));";
        webView.evaluateJavascript(javascript, null);
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
