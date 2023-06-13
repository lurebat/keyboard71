package com.jormy.nin;

import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
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

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class Utils {
    private static AssetManager leassetmanager;
    private static ContextWrapper wra_global;
    static SoundPool sp = null;
    static Map<String, Integer> soundeffect_map = null;

    Utils() {
    }

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
        }
        return "";
    }

    public static void openNinPlayStoreLink() {
        try {
            Intent theintent = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=com.jormy.nin"));
            theintent.setFlags(268435456);
            con().startActivity(theintent);
        } catch (ActivityNotFoundException e) {
            Intent theintent2 = new Intent("android.intent.action.VIEW", Uri.parse("http://play.google.com/store/apps/details?id=com.jormy.nin"));
            theintent2.setFlags(268435456);
            con().startActivity(theintent2);
        }
    }

    public static void testLength(String lestr) {
        System.out.println("jormoust testing length : |" + lestr + "| @ " + lestr.length());
    }

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

    static ContextWrapper wra() {
        if (wra_global == null) {
            wra_global = new ContextWrapper(con());
        }
        return wra_global;
    }

    public static String homeDirectory() {
        return con().getFilesDir().getAbsolutePath();
    }

    public static String cacheDirectory() {
        return con().getCacheDir().getAbsolutePath();
    }

    public static AssetManager assetManager() {
        if (leassetmanager == null) {
            leassetmanager = con().getAssets();
        }
        return leassetmanager;
    }

    public static void initRoenSoundPool() {
        if (sp == null) {
            sp = new SoundPool(10, 3, 0);
            soundeffect_map = new HashMap();
        }
    }

    public static Integer roenObtainSound(String soundname) {
        initRoenSoundPool();
        Integer theid = soundeffect_map.get(soundname);
        if (theid == null) {
            int realid = -1;
            try {
                AssetFileDescriptor descriptor = assetManager().openFd(soundname + ".ogg");
                realid = sp.load(descriptor, 1);
            } catch (IOException e) {
                prin("Cannot load the sound : " + soundname);
            }
            Integer theid2 = new Integer(realid);
            soundeffect_map.put(soundname, theid2);
            return theid2;
        }
        return theid;
    }

    public static void roenPreloadSound(String soundname) {
        Integer theid = roenObtainSound(soundname);
        int intval = theid.intValue();
        if (intval >= 0) {
            sp.play(intval, 0.02f, 0.02f, 0, 0, 1.0f);
        }
    }

    public static void roenCallPlaySound(String soundname, float pitch, float volume) {
        initRoenSoundPool();
        Integer theid = roenObtainSound(soundname);
        int intval = theid.intValue();
        if (intval >= 0) {
            sp.play(intval, volume, volume, 0, 0, pitch);
        }
    }

    public static boolean androidScreenLocked() {
        KeyguardManager myKM = (KeyguardManager) SoftKeyboard.globalcontext.getSystemService("keyguard");
        return myKM.inKeyguardRestrictedInputMode();
    }

    public static boolean listAssetFiles(String path) {
        try {
            String[] list = assetManager().list(path);
            if (list.length > 0) {
                prin("It's a folder! \"" + path + "\" :: " + Integer.toString(list.length));
                int counta = 0;
                for (String file : list) {
                    counta++;
                    if (counta % 10 == 0) {
                        prin(file + " --- " + Integer.toString(counta));
                    }
                }
            } else {
                prin(path + " -- file");
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void copyToClipboard(String value) {
        ClipboardManager clipboard = (ClipboardManager) con().getSystemService("clipboard");
        ClipData clip = ClipData.newPlainText("some text", value);
        clipboard.setPrimaryClip(clip);
    }

    public static String getStringFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) con().getSystemService("clipboard");
        if (!clipboard.hasPrimaryClip()) {
            return null;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip.getDescription().hasMimeType("text/plain")) {
            clip.getItemAt(0).getText().toString();
        }
        String textToPaste = clip.getItemAt(0).coerceToText(con()).toString();
        return textToPaste;
    }
}
