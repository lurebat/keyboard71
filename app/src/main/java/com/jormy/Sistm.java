package com.jormy;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/* loaded from: classes.dex */
public class Sistm {
    static final /* synthetic */ boolean $assertionsDisabled;
    static Activity current_activity;
    static Context current_app_context;
    static ClipboardManager current_clipboard_manager;

    static {
        $assertionsDisabled = !Sistm.class.desiredAssertionStatus();
        current_app_context = null;
        current_activity = null;
        current_clipboard_manager = null;
    }

    static void prin(String lestr) {
        java.lang.System.out.println("jormoust/sistm:" + lestr);
    }

    public static void assignActivity(Activity leact) {
        current_activity = leact;
    }

    public static void assignAppContext(Context lecon) {
        if (current_app_context == null) {
            current_app_context = lecon;
            if (!$assertionsDisabled && lecon == null) {
                throw new AssertionError();
            }
            ClipboardManager clipman = (ClipboardManager) lecon.getSystemService("clipboard");
            current_clipboard_manager = clipman;
        }
    }

    public static Context appcon() {
        if ($assertionsDisabled || current_app_context != null) {
            return current_app_context;
        }
        throw new AssertionError("App context not assigned yet! We need this for everything");
    }

    public static Activity appact() {
        if ($assertionsDisabled || current_activity != null) {
            return current_activity;
        }
        throw new AssertionError("App activity not assigned yet!");
    }

    public static ClipboardManager clipman() {
        if ($assertionsDisabled || current_clipboard_manager != null) {
            return current_clipboard_manager;
        }
        throw new AssertionError("No clipboard manager has been assigned yet");
    }

    public static void copyToClipboard(String value) {
        ClipboardManager clipboard = clipman();
        ClipData clip = ClipData.newPlainText("text from nintype", value);
        clipboard.setPrimaryClip(clip);
    }

    public static String getStringFromClipboard() {
        String textToPaste = "";
        ClipboardManager clipboard = clipman();
        if (clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip.getDescription().hasMimeType("text/plain")) {
                clip.getItemAt(0).getText().toString();
            }
            textToPaste = clip.getItemAt(0).coerceToText(appcon()).toString();
        }
        if (textToPaste == null) {
            return "";
        }
        return textToPaste;
    }

    public static String async_getStringFromClipboard() {
        Executors.newSingleThreadExecutor();
        FutureTask<String> letask = new FutureTask<>(new Callable<String>() { // from class: com.jormy.Sistm.1
            @Override // java.util.concurrent.Callable
            public String call() throws Exception {
                return Sistm.getStringFromClipboard();
            }
        });
        appact().runOnUiThread(letask);
        try {
            String ret = letask.get();
            return ret;
        } catch (Exception e) {
            prin("async fail : " + e.toString());
            return null;
        }
    }
}
