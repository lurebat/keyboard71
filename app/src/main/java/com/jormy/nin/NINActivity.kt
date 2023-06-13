package com.jormy.nin

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
import android.widget.ImageView
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class NINActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val context = applicationContext
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        805306374
        val wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE , "Nintype:WakeLock")
        wakeLock.acquire()
        wakeLock.release()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(FLAG_SHOW_WHEN_LOCKED)
        super.onCreate(savedInstanceState)

        val image = ImageView(context)
        try {
            val filename = "androidnin_splashpage.png";

            val ims = assets.open(filename)
            val drawable = Drawable.createFromStream(ims, null)
            image.setImageDrawable(drawable)

        } catch (ex: IOException) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            ex.printStackTrace(pw)
            println("jormoust crash : $pw")
        }


        image.setOnClickListener {
            this@NINActivity.startActivityForResult(
                Intent("android.settings.INPUT_METHOD_SETTINGS"),
                0
            )
        }

        setContentView(image)
    }
}
