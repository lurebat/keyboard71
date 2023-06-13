package com.jormy.nin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import com.jormy.nin.EXSurfaceView;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class NINView extends EXSurfaceView {
    private static final boolean DEBUG = false;
    static ContextFactory globalcontextfactory;
    public static NINView globalview;
    public static ConcurrentLinkedQueue<RelayTouchInfo> moventqueue;
    boolean desired_roen_fullscreen;
    float desired_roen_pixel_height;
    float desired_roen_pixel_width;
    private Renderer myRenderer;
    float x_viewscaling;
    float y_viewscaling;
    private static String TAG = "NINView";
    public static float device_ppi = 326.0f;
    public static float device_portraitwidth = 640.0f;
    public static float desired_scaling = 1.0f;
    public static float last_desired_portrait = 640.0f;
    public static int heighttestcounta = 0;

    public NINView(Context context) {
        super(context);
        this.x_viewscaling = 1.0f;
        this.y_viewscaling = 1.0f;
        this.desired_roen_pixel_height = 800.0f;
        this.desired_roen_pixel_width = 640.0f;
        this.desired_roen_fullscreen = false;
        init(true, 0, 0);
    }

    public NINView(Context context, boolean translucent, int depth, int stencil) {
        super(context);
        this.x_viewscaling = 1.0f;
        this.y_viewscaling = 1.0f;
        this.desired_roen_pixel_height = 800.0f;
        this.desired_roen_pixel_width = 640.0f;
        this.desired_roen_fullscreen = false;
        init(translucent, depth, stencil);
    }

    private void init(boolean translucent, int depth, int stencil) {
        moventqueue = new ConcurrentLinkedQueue<>();
        NINLib.syncTiming(System.currentTimeMillis());
        if (translucent) {
            getHolder().setFormat(-3);
        }
        if (globalcontextfactory == null) {
            globalcontextfactory = new ContextFactory();
        }
        setEGLContextFactory(globalcontextfactory);
        setEGLConfigChooser(translucent ? new ConfigChooser(8, 8, 8, 8, depth, stencil) : new ConfigChooser(5, 6, 5, 0, depth, stencil));
        this.myRenderer = new Renderer();
        setRenderer(this.myRenderer);
        setPreserveEGLContextOnPause(true);
        globalview = this;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        device_ppi = metrics.xdpi;
        device_portraitwidth = Math.min(metrics.widthPixels, metrics.heightPixels);
        onResume();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ContextFactory implements EXSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 12440;
        public static EGLContext le_context = null;
        public static EGLDisplay le_display = null;

        private ContextFactory() {
        }

        @Override // com.jormy.nin.EXSurfaceView.EGLContextFactory
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            if (le_context == null) {
                Log.w(NINView.TAG, "creating OpenGL ES 2.0 context");
                NINView.checkEglError("Before eglCreateContext", egl);
                int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, 12344};
                EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
                NINView.checkEglError("After eglCreateContext", egl);
                le_context = context;
                le_display = display;
                return context;
            }
            if (display != le_display) {
                Utils.prin("BUT THE DISPLAY IS FUCKING DIFFERENT!");
                Thread.currentThread();
                Thread.dumpStack();
                System.exit(1);
            }
            return le_context;
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

    /* JADX INFO: Access modifiers changed from: private */
    public static void checkEglError(String prompt, EGL10 egl) {
        while (true) {
            int error = egl.eglGetError();
            if (error != 12288) {
                Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, Integer.valueOf(error)));
            } else {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ConfigChooser implements EXSurfaceView.EGLConfigChooser {
        private static int EGL_OPENGL_ES2_BIT = 4;
        private static int[] s_configAttribs2 = {12324, 4, 12323, 4, 12322, 4, 12352, EGL_OPENGL_ES2_BIT, 12344};
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
            egl.eglChooseConfig(display, s_configAttribs2, null, 0, num_config);
            int numConfigs = num_config[0];
            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }
            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, s_configAttribs2, configs, numConfigs, num_config);
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
                int defaultValue2 = this.mValue[0];
                return defaultValue2;
            }
            return defaultValue;
        }

        private void printConfigs(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            int numConfigs = configs.length;
            Log.w(NINView.TAG, String.format("%d configurations", Integer.valueOf(numConfigs)));
            for (int i = 0; i < numConfigs; i++) {
                Log.w(NINView.TAG, String.format("Configuration %d:\n", Integer.valueOf(i)));
                printConfig(egl, display, configs[i]);
            }
        }

        private void printConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] attributes = {12320, 12321, 12322, 12323, 12324, 12325, 12326, 12327, 12328, 12329, 12330, 12331, 12332, 12333, 12334, 12335, 12336, 12337, 12338, 12339, 12340, 12343, 12342, 12341, 12345, 12346, 12347, 12348, 12349, 12350, 12351, 12352, 12354};
            String[] names = {"EGL_BUFFER_SIZE", "EGL_ALPHA_SIZE", "EGL_BLUE_SIZE", "EGL_GREEN_SIZE", "EGL_RED_SIZE", "EGL_DEPTH_SIZE", "EGL_STENCIL_SIZE", "EGL_CONFIG_CAVEAT", "EGL_CONFIG_ID", "EGL_LEVEL", "EGL_MAX_PBUFFER_HEIGHT", "EGL_MAX_PBUFFER_PIXELS", "EGL_MAX_PBUFFER_WIDTH", "EGL_NATIVE_RENDERABLE", "EGL_NATIVE_VISUAL_ID", "EGL_NATIVE_VISUAL_TYPE", "EGL_PRESERVED_RESOURCES", "EGL_SAMPLES", "EGL_SAMPLE_BUFFERS", "EGL_SURFACE_TYPE", "EGL_TRANSPARENT_TYPE", "EGL_TRANSPARENT_RED_VALUE", "EGL_TRANSPARENT_GREEN_VALUE", "EGL_TRANSPARENT_BLUE_VALUE", "EGL_BIND_TO_TEXTURE_RGB", "EGL_BIND_TO_TEXTURE_RGBA", "EGL_MIN_SWAP_INTERVAL", "EGL_MAX_SWAP_INTERVAL", "EGL_LUMINANCE_SIZE", "EGL_ALPHA_MASK_SIZE", "EGL_COLOR_BUFFER_TYPE", "EGL_RENDERABLE_TYPE", "EGL_CONFORMANT"};
            int[] value = new int[1];
            for (int i = 0; i < attributes.length; i++) {
                int attribute = attributes[i];
                String name = names[i];
                if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                    Log.w(NINView.TAG, String.format("  %s: %d\n", name, Integer.valueOf(value[0])));
                } else {
                    do {
                    } while (egl.eglGetError() != 12288);
                }
            }
        }
    }

    public static String actionToString(int action) {
        switch (action) {
            case EXSurfaceView.RENDERMODE_WHEN_DIRTY /* 0 */:
                return "Down";
            case 1:
                return "Up";
            case EXSurfaceView.DEBUG_LOG_GL_CALLS /* 2 */:
                return "Move";
            case 3:
                return "Cancel";
            case 4:
                return "Outside";
            case 5:
                return "Pointer Down";
            case 6:
                return "Pointer Up";
            default:
                return "";
        }
    }

    public static int actionToJormyAction(int action) {
        switch (action) {
            case EXSurfaceView.RENDERMODE_WHEN_DIRTY /* 0 */:
            case 5:
                return 0;
            case 1:
                return 2;
            case EXSurfaceView.DEBUG_LOG_GL_CALLS /* 2 */:
                return 1;
            case 3:
                return 2;
            case 4:
                return -1;
            case 6:
                return 2;
            default:
                return -1;
        }
    }

    public static float getDevicePPI() {
        return device_ppi;
    }

    public static float getDevicePortraitWidth() {
        return device_portraitwidth;
    }

    public static float getLastDesiredPortraitWidth() {
        return last_desired_portrait;
    }

    float getDesiredPixelWidth() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float wwww = metrics.widthPixels;
        float hhhh = metrics.heightPixels;
        float ppi = metrics.xdpi;
        float f = metrics.ydpi;
        float portrait_pixels = Math.min(wwww, hhhh);
        float portrait_inches = portrait_pixels / ppi;
        float pixelperfect_roen_pixel_width = this.desired_roen_pixel_width * (portrait_inches / 1.9631902f);
        float desired_portrait = pixelperfect_roen_pixel_width / desired_scaling;
        last_desired_portrait = desired_portrait;
        return wwww <= hhhh ? desired_portrait : desired_portrait * (wwww / hhhh);
    }

    public static void heightTestFunc() {
        heighttestcounta++;
        if (heighttestcounta > 10) {
            heighttestcounta = 0;
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.jormy.nin.NINView.1
            @Override // java.lang.Runnable
            public void run() {
                NINView.globalview.requestLayout();
            }
        });
    }

    public static void adjustWantedScaling(float thewantedscaling) {
        desired_scaling = thewantedscaling;
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.jormy.nin.NINView.2
            @Override // java.lang.Runnable
            public void run() {
                NINView.globalview.requestLayout();
            }
        });
    }

    public static void onRoenSignalDirty() {
        globalview.requestRender();
    }

    public static void onRoenFrozennessChange(boolean truth) {
        if (!truth) {
            globalview.requestRender();
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        int actionid = event.getActionMasked();
        int pointercount = event.getPointerCount();
        for (int i = 0; i < pointercount; i++) {
            RelayTouchInfo topush = new RelayTouchInfo();
            topush.touchid = event.getPointerId(i);
            topush.xPos = event.getX(i) / this.x_viewscaling;
            topush.yPos = event.getY(i) / this.y_viewscaling;
            topush.pressureValue = event.getPressure(i);
            topush.areaValue = event.getSize(i);
            topush.timestamp_long = System.currentTimeMillis();
            topush.jormactionid = actionToJormyAction(actionid);
            if ((actionid != 6 && actionid != 5) || i == event.getActionIndex()) {
                moventqueue.add(topush);
            }
        }
        globalview.requestRender();
        return true;
    }

    public static void adjustKeyboardDimensions(float wanted_roenheight, boolean fullmode) {
        globalview.desired_roen_pixel_height = 2.0f * wanted_roenheight;
        globalview.desired_roen_fullscreen = fullmode;
        Utils.prin("::::::::::: onAdjustKeyboardDimension : " + wanted_roenheight + " // " + fullmode);
        if (fullmode) {
            DisplayMetrics metrics = globalview.getResources().getDisplayMetrics();
            float wwww = metrics.widthPixels;
            float height = metrics.heightPixels - 240;
            float wantedratio = height / wwww;
            Utils.tracedims("fullmode metrics, after cut", wwww, height);
            float desiredpixwidth = globalview.getDesiredPixelWidth();
            globalview.desired_roen_pixel_height = desiredpixwidth * wantedratio;
            Utils.prin("Desired pixwidth : " + desiredpixwidth);
        }
        Utils.prin("what is scaling: " + desired_scaling);
        if (!fullmode) {
            Utils.prin("Roenpixheight : " + globalview.desired_roen_pixel_height + " from " + wanted_roenheight);
        } else {
            Utils.prin("Roenpixheight : " + globalview.desired_roen_pixel_height);
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.jormy.nin.NINView.3
            @Override // java.lang.Runnable
            public void run() {
                NINView.globalview.requestLayout();
            }
        });
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
        float rawRoenDesiredHeight = this.desired_roen_pixel_height;
        if (!this.desired_roen_fullscreen) {
            rawRoenDesiredHeight = this.desired_roen_pixel_height;
        }
        Utils.prin("rawRoenDesiredHeight : " + rawRoenDesiredHeight);
        Utils.prin("actual scaling : " + actual_scale);
        float desiredHeight = Math.min(rawRoenDesiredHeight, (height / wwww) * desiredWidth);
        Utils.prin("desiredHeight : " + desiredHeight);
        float f = wwww / desiredWidth;
        this.y_viewscaling = f;
        this.x_viewscaling = f;
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
        public void onDrawFrame(GL10 gl) {
            while (true) {
                RelayTouchInfo rti = NINView.moventqueue.poll();
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
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            DisplayMetrics metrics = NINView.globalview.getResources().getDisplayMetrics();
            int wwww = metrics.widthPixels;
            int hhhh = metrics.heightPixels;
            NINLib.init(width, height, wwww, hhhh);
        }

        @Override // com.jormy.nin.EXSurfaceView.Renderer
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }
    }
}
