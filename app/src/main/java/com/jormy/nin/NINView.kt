package com.jormy.nin;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.EGL14;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class NINView extends EXSurfaceView {
    static ContextFactory globalContextFactory;
    public static NINView globalView;
    public static ConcurrentLinkedQueue<RelayTouchInfo> movementEventsQueue;
    boolean desiredRoenFullscreen = false;
    float desiredRoenPixelHeight = 800.0f;
    float desiredRoenPixelWidth = 640.0f;
    float xViewScaling = 1.0f;
    float yViewScaling = 1.0f;
    private static final String TAG = "NINView";
    public static float devicePpi = 326.0f;
    public static float devicePortraitWidth = 640.0f;
    public static float desiredScaling = 1.0f;
    public static float last_desired_portrait = 640.0f;
    private final Preferences preferences = new Preferences(this.getContext());
    private final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            super.onLongPress(e);
            if (preferences.hapticFeedbackBlocking()) {
                NINView.this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        }
    });

    public NINView(Context context) {
        super(context);

        NINLib.syncTiming(System.currentTimeMillis());

        movementEventsQueue = new ConcurrentLinkedQueue<>();

        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        if (globalContextFactory == null) {
            globalContextFactory = new ContextFactory();
        }

        setEGLContextFactory(globalContextFactory);
        setEGLConfigChooser(new ConfigChooser(8, 8, 8, 8, 0, 0));
        setRenderer(new Renderer());
        setPreserveEGLContextOnPause(true);

        globalView = this;
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        devicePpi = metrics.xdpi;
        devicePortraitWidth = Math.min(metrics.widthPixels, metrics.heightPixels);

        onResume();
    }

    public static class ContextFactory implements EXSurfaceView.EGLContextFactory {
        public static EGLContext eglContext = null;
        public static EGLDisplay eglDisplay = null;

        private ContextFactory() {
        }

        @Override // com.jormy.nin.EXSurfaceView.EGLContextFactory
        public EGLContext createContext(EGL10 egl10, EGLDisplay display, EGLConfig eglConfig) {
            if (eglContext == null) {
                Log.w(NINView.TAG, "creating OpenGL ES 2.0 context");
                NINView.checkEglError("Before eglCreateContext", egl10);
                EGLContext context = egl10.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE});
                NINView.checkEglError("After eglCreateContext", egl10);
                eglContext = context;
                eglDisplay = display;
                return context;
            }
            if (display != eglDisplay) {
                Utils.prin("BUT THE DISPLAY IS FUCKING DIFFERENT!");
                Thread.currentThread();
                Thread.dumpStack();
                System.exit(1);
            }
            return eglContext;
        }

        @Override // com.jormy.nin.EXSurfaceView.EGLContextFactory
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            Utils.prin("--------------------- Destroy context, but nope");
            Utils.prin("Exiting, because we need to recreate the OGL Context!");
            egl.eglDestroyContext(display, context);
            Thread.currentThread();
            Thread.dumpStack();
            System.exit(1);
        }
    }

    public static void checkEglError(String prompt, EGL10 egl) {
        while (true) {
            int error = egl.eglGetError();
            if (error != EGL14.EGL_SUCCESS) {
                Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
            } else {
                return;
            }
        }
    }

    public static class ConfigChooser implements EXSurfaceView.EGLConfigChooser {
        private static int[] attributes = {EGL14.EGL_RED_SIZE, 4, EGL14.EGL_GREEN_SIZE, 4, EGL14.EGL_BLUE_SIZE, 4, EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE};
        protected int mAlphaSize;
        protected int mBlueSize;
        protected int mDepthSize;
        protected int mGreenSize;
        protected int mRedSize;
        protected int mStencilSize;
        private int[] mValue = new int[1];

        public ConfigChooser(int r, int g, int b, int a, int depth, int stencil) {
            this.mRedSize = r;
            this.mGreenSize = g;
            this.mBlueSize = b;
            this.mAlphaSize = a;
            this.mDepthSize = depth;
            this.mStencilSize = stencil;
        }

        @Override // com.jormy.nin.EXSurfaceView.EGLConfigChooser
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            egl.eglChooseConfig(display, attributes, null, 0, num_config);
            int numConfigs = num_config[0];
            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, attributes, configs, numConfigs, num_config);
            return chooseConfig(egl, display, configs);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config, 12325, 0);
                int s = findConfigAttrib(egl, display, config, 12326, 0);
                if (d >= this.mDepthSize && s >= this.mStencilSize) {
                    int r = findConfigAttrib(egl, display, config, 12324, 0);
                    int g = findConfigAttrib(egl, display, config, 12323, 0);
                    int b = findConfigAttrib(egl, display, config, 12322, 0);
                    int a = findConfigAttrib(egl, display, config, 12321, 0);
                    if (r == this.mRedSize && g == this.mGreenSize && b == this.mBlueSize && a == this.mAlphaSize) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
            if (egl.eglGetConfigAttrib(display, config, attribute, this.mValue)) {
                return this.mValue[0];
            }
            return defaultValue;
        }
    }

    public static int actionToJormyAction(int action) {
        return switch (action) {
            case MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> 0;
            case MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL ->
                    2;
            case MotionEvent.ACTION_MOVE -> 1;
            default -> -1;
        };
    }

    @Api
    public static float getDevicePPI() {
        return devicePpi;
    }

    @Api
    public static float getDevicePortraitWidth() {
        return devicePortraitWidth;
    }

    float getDesiredPixelWidth() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float width = metrics.widthPixels;
        float height = metrics.heightPixels;
        float ppi = metrics.xdpi;
        float portraitPixels = Math.min(width, height);
        float portraitInches = portraitPixels / ppi;
        float pixelPerfectRoenPixelWidth = this.desiredRoenPixelWidth * (portraitInches / 1.9631902f);
        float desiredPortrait = pixelPerfectRoenPixelWidth / desiredScaling;
        last_desired_portrait = desiredPortrait;
        return (width <= height) ? desiredPortrait : (desiredPortrait * (width / height));
    }

    @Api
    public static void adjustWantedScaling(float scaling) {
        desiredScaling = scaling;
        new Handler(Looper.getMainLooper()).post(() -> NINView.globalView.requestLayout());
    }

    @Api
    public static void onRoenSignalDirty() {
        globalView.requestRender();
    }

    @Api
    public static void onRoenFrozennessChange(boolean truth) {
        if (!truth) {
            globalView.requestRender();
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        int actionId = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        for (int i = 0; i < pointerCount; i++) {
            doHapticFeedback(actionId);

            if ((actionId == MotionEvent.ACTION_POINTER_UP || actionId == MotionEvent.ACTION_POINTER_DOWN) && i != event.getActionIndex()) {
                continue;
            }

            movementEventsQueue.add(new RelayTouchInfo(
                    event.getPointerId(i),
                    event.getX(i) / this.xViewScaling,
                    event.getY(i) / this.yViewScaling,
                    event.getPressure(i),
                    event.getSize(i),
                    System.currentTimeMillis(),
                    actionToJormyAction(actionId)
            ));
        }
        globalView.requestRender();
        return true;
    }

    private void doHapticFeedback(int actionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            return;
        }

        if (!preferences.hapticFeedbackBlocking()) {
            return;
        }

        if (actionId == MotionEvent.ACTION_DOWN || actionId == MotionEvent.ACTION_POINTER_DOWN) {
            this.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    @Api
    public static void adjustKeyboardDimensions(float wantedRoenHeight, boolean fullscreen) {
        globalView.desiredRoenPixelHeight = 2.0f * wantedRoenHeight;
        globalView.desiredRoenFullscreen = fullscreen;
        Utils.prin("::::::::::: onAdjustKeyboardDimension : " + wantedRoenHeight + " // " + fullscreen);
        if (fullscreen) {
            DisplayMetrics metrics = globalView.getResources().getDisplayMetrics();
            float wwww = metrics.widthPixels;
            float height = metrics.heightPixels - 240;
            float wantedratio = height / wwww;
            Utils.tracedims("fullmode metrics, after cut", wwww, height);
            float desiredpixwidth = globalView.getDesiredPixelWidth();
            globalView.desiredRoenPixelHeight = desiredpixwidth * wantedratio;
            Utils.prin("Desired pixwidth : " + desiredpixwidth);
        }
        Utils.prin("what is scaling: " + desiredScaling);
        if (!fullscreen) {
            Utils.prin("Roenpixheight : " + globalView.desiredRoenPixelHeight + " from " + wantedRoenHeight);
        } else {
            Utils.prin("Roenpixheight : " + globalView.desiredRoenPixelHeight);
        }
        // from class: com.jormy.nin.NINView.3
// java.lang.Runnable
        new Handler(Looper.getMainLooper()).post(() -> NINView.globalView.requestLayout());
    }

    @Override // android.view.SurfaceView, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float wwww = metrics.widthPixels + 2;
        float height = metrics.heightPixels + 2;
        Utils.tracedims("::::::::onMeasure called --------------- : ", widthMeasureSpec, heightMeasureSpec);
        Utils.tracedims("metrics : ", metrics.widthPixels, metrics.heightPixels);
        float desiredWidth = getDesiredPixelWidth();
        float actual_scale = desiredWidth / wwww;
        Utils.prin("desired width : " + desiredWidth);
        float rawRoenDesiredHeight = this.desiredRoenPixelHeight;
        if (!this.desiredRoenFullscreen) {
            rawRoenDesiredHeight = this.desiredRoenPixelHeight;
        }
        Utils.prin("rawRoenDesiredHeight : " + rawRoenDesiredHeight);
        Utils.prin("actual scaling : " + actual_scale);
        float desiredHeight = Math.min(rawRoenDesiredHeight, (height / wwww) * desiredWidth);
        Utils.prin("desiredHeight : " + desiredHeight);
        float f = wwww / desiredWidth;
        this.yViewScaling = f;
        this.xViewScaling = f;
        float height2 = (wwww * desiredHeight) / desiredWidth;
        SurfaceHolder holder = getHolder();
        if (holder != null) {
            holder.setFixedSize((int) desiredWidth, (int) desiredHeight);
        }
        Utils.tracedims("setMeasuredDims : ", wwww, height2);
        setMeasuredDimension((int) wwww, (int) height2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Renderer implements EXSurfaceView.Renderer {
        private Renderer() {
        }

        @Override // com.jormy.nin.EXSurfaceView.Renderer
        public void onDrawFrame(GL10 gl10) {
            while (true) {
                RelayTouchInfo rti = NINView.movementEventsQueue.poll();
                if (rti != null) {
                    NINLib.onTouchEvent(rti.touchid, rti.jormactionid, rti.xPos, rti.yPos, rti.pressureValue, rti.areaValue, rti.timestamp_long);
                } else {
                    SoftKeyboard.relayDelayedEvents();
                    NINLib.step();
                    return;
                }
            }
        }

        @Override // com.jormy.nin.EXSurfaceView.Renderer
        public void onSurfaceChanged(GL10 gl10, int width, int height) {
            DisplayMetrics metrics = NINView.globalView.getResources().getDisplayMetrics();
            int wwww = metrics.widthPixels;
            int hhhh = metrics.heightPixels;
            NINLib.init(width, height, wwww, hhhh);
        }

        @Override // com.jormy.nin.EXSurfaceView.Renderer
        public void onSurfaceCreated(GL10 gl10, EGLConfig config) {
        }
    }
}
