package com.jormy.nin;

import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    private static AssetManager assetManager;
    static SoundPool soundPool = null;
    static Map<String, Integer> soundeffectMap = null;

    Utils() {
    }

    @Api
    public static String getIPAddressIPV4() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(58) < 0;
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("NIN", "Cannot get IP address", e);
        }
        return "";
    }

    @Api
    public static void openNinPlayStoreLink() {
        try {
            Intent theintent = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.jormy.nin"));
            theintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            con().startActivity(theintent);
        } catch (ActivityNotFoundException e) {
            Intent theintent2 = new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=com.jormy.nin"));
            theintent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            con().startActivity(theintent2);
        }
    }

    @Api
    public static void vibrate(int millisecs) {
        Log.d("NIN", "vibrate " + millisecs);
        Vibrator v = (Vibrator) con().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(millisecs);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void tracedims(String desc, float xdim, float ydim) {
        System.out.println("jormoust/jav:" + desc + " : " + xdim + " x " + ydim + " (" + (ydim / xdim) + " )");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void prin(String lestr) {
        System.out.println("jormoust/jav:" + lestr);
    }

    static Context con() {
        return SoftKeyboard.globalcontext;
    }

    @Api
    public static String homeDirectory() {
        return con().getFilesDir().getAbsolutePath();
    }

    @Api
    public static String cacheDirectory() {
        return con().getCacheDir().getAbsolutePath();
    }

    @Api
    public static AssetManager assetManager() {
        if (assetManager == null) {
            assetManager = con().getAssets();
        }
        return assetManager;
    }

    public static void initRoenSoundPool() {
        if (soundPool == null) {
            soundPool = new SoundPool(10, 3, 0);
            soundeffectMap = new HashMap<>();
        }
    }

    public static Integer roenObtainSound(String soundname) {
        initRoenSoundPool();
        Integer theid = soundeffectMap.get(soundname);
        if (theid == null) {
            int realid = -1;
            try {
                AssetFileDescriptor descriptor = assetManager().openFd(soundname + ".ogg");
                realid = soundPool.load(descriptor, 1);
            } catch (IOException e) {
                prin("Cannot load the sound : " + soundname);
            }
            Integer theid2 = realid;
            soundeffectMap.put(soundname, theid2);
            return theid2;
        }
        return theid;
    }

    @Api
    public static void roenPreloadSound(String soundname) {
        int intval = roenObtainSound(soundname);
        if (intval >= 0) {
            soundPool.play(intval, 0.02f, 0.02f, 0, 0, 1.0f);
        }
    }

    @Api
    public static void roenCallPlaySound(String soundname, float pitch, float volume) {
        initRoenSoundPool();
        int intval = roenObtainSound(soundname);
        if (intval >= 0) {
            soundPool.play(intval, volume, volume, 0, 0, pitch);
        }
    }

    public static boolean isScreenLocked() {
        KeyguardManager myKM = (KeyguardManager) SoftKeyboard.globalcontext.getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();
    }

    @Api
    public static void copyToClipboard(String value) {
        ClipboardManager clipboard = (ClipboardManager) con().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("some text", value);
        clipboard.setPrimaryClip(clip);
    }

    @Api
    public static String getStringFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) con().getSystemService(Context.CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip()) {
            return null;
        }
        ClipData clip = clipboard.getPrimaryClip();
        return clip != null ? clip.getItemAt(0).coerceToText(con()).toString() : null;
    }
}
