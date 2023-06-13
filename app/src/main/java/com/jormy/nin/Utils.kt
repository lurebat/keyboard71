package com.jormy.nin

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.media.SoundPool
import android.net.Uri
import android.os.Vibrator
import android.util.Log
import java.io.IOException
import java.net.NetworkInterface

private const val DEFAULT_LEFT_VOLUME = 0.02f

private const val DEFAULT_PITCH = 1.0f

object Utils {
    private val soundPool by lazy { SoundPool.Builder().setMaxStreams(10).build() }
    private var soundEffectMap: MutableMap<String, Int> = HashMap()
    private val assetManager by lazy { con().assets }

    @Api
    @JvmStatic
    fun getIPAddressIPV4(): String {
        val interfaces = NetworkInterface.getNetworkInterfaces()

        val ip = interfaces
            .asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filter { !it.isLoopbackAddress }
            .filter { it is java.net.Inet4Address }
            .mapNotNull { it.hostAddress }
            .firstOrNull()

        if (ip == null) {
            Log.e("NIN", "Cannot get IP address")
            return ""
        }

        return ip
    }

    @Api
    @JvmStatic
    fun openNinPlayStoreLink() {
        fun startIntent(u: String) = con().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        try {
            startIntent("market://details?id=com.jormy.nin")
        } catch (e: ActivityNotFoundException) {
            startIntent("http://play.google.com/store/apps/details?id=com.jormy.nin")
        }
    }

    @Api
    @JvmStatic
    fun vibrate(millisecs: Int) {
        val v = con().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(millisecs.toLong())
    }

    @JvmStatic
    fun tracedims(desc: String, xdim: Float, ydim: Float) {
        println("jormoust/jav:" + desc + " : " + xdim + " x " + ydim + " (" + ydim / xdim + " )")
    }

    @JvmStatic
    fun prin(lestr: String) {
        println("jormoust/jav:$lestr")
    }

    fun con(): Context {
        return SoftKeyboard.globalcontext!!
    }

    @Api
    @JvmStatic
    fun homeDirectory(): String {
        return con().filesDir.absolutePath
    }

    @Api
    @JvmStatic
    fun cacheDirectory(): String {
        return con().cacheDir.absolutePath
    }

    @Api
    @JvmStatic
    fun assetManager(): AssetManager {
        return assetManager
    }

    fun roenObtainSound(soundname: String): Int? {
        val mappedID = soundEffectMap[soundname]
        if (mappedID != null) {
            return mappedID
        }

        var realID = -1
        try {
            val descriptor = assetManager().openFd("$soundname.ogg")
            realID = soundPool.load(descriptor, 1)
        } catch (e: IOException) {
            prin("Cannot load the sound : $soundname")
        }

        if (realID < 0) {
            return null
        }

        soundEffectMap[soundname] = realID
        return realID
    }

    @Api
    @JvmStatic
    fun roenPreloadSound(soundname: String) {
        val soundID = roenObtainSound(soundname) ?: return
        soundPool!!.play(soundID, DEFAULT_LEFT_VOLUME, DEFAULT_LEFT_VOLUME, 0, 0, DEFAULT_PITCH)
    }

    @Api
    @JvmStatic
    fun roenCallPlaySound(soundname: String, pitch: Float, volume: Float) {
        val intval = roenObtainSound(soundname) ?: return
        soundPool!!.play(intval, volume, volume, 0, 0, pitch)
    }

    @JvmStatic
    fun isScreenLocked(): Boolean {
        val myKM = con().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return myKM.isKeyguardLocked
    }

    @Api
    @JvmStatic
    fun copyToClipboard(value: String?) {
        val clipboard = con().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Nintype data", value)
        clipboard.setPrimaryClip(clip)
    }

    @JvmStatic
    @Api
    fun getStringFromClipboard(): String? {
        val clipboard = con().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) {
            return null
        }
        val clip = clipboard.primaryClip
        return clip?.getItemAt(0)?.coerceToText(con())?.toString()
    }
}
