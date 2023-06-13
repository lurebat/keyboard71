package com.jormy.nin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import com.jormy.Sistm;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

/* loaded from: classes.dex */
public class NINActivity extends Activity {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static Context globalcontext;
    private static PowerManager.WakeLock wakeLock;
    ImageView mView;

    static {
        $assertionsDisabled = !NINActivity.class.desiredAssertionStatus();
    }

    public static Context getAppContext() {
        if ($assertionsDisabled || globalcontext != null) {
            return globalcontext;
        }
        throw new AssertionError();
    }

    static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override // android.app.Activity
    protected void onCreate(Bundle icicle) {
        globalcontext = getApplicationContext();
        Sistm.assignActivity(this);
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService("power");
        wakeLock = pm.newWakeLock(805306374, "okTag:wew");
        wakeLock.acquire();
        wakeLock.release();
        Window window = getWindow();
        window.addFlags(4194304);
        super.onCreate(icicle);
        this.mView = new ImageView(globalcontext);
        try {
            String filename = "androidnin_splashpage.png";
            InputStream esophagus = getAssets().open("esophagus.list");
            if (esophagus != null) {
                String contents = convertStreamToString(esophagus);
                if (contents.length() > 5) {
                    filename = "androidnin_trialsplashpage.png";
                }
            }
            InputStream ims = getAssets().open(filename);
            Drawable d = Drawable.createFromStream(ims, null);
            this.mView.setImageDrawable(d);
        } catch (IOException ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            System.out.println("jormoust crash : " + pw);
        }
        this.mView.setOnClickListener(new View.OnClickListener() { // from class: com.jormy.nin.NINActivity.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                NINActivity.this.startActivityForResult(new Intent("android.settings.INPUT_METHOD_SETTINGS"), 0);
            }
        });
        setContentView(this.mView);
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
    }
}
