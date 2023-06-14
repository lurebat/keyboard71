//from NINView

/*
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
 */

/*
    public NINView(Context context, boolean translucent, int depth, int stencil) {
        super(context);
        this.x_viewscaling = 1.0f;
        this.y_viewscaling = 1.0f;
        this.desired_roen_pixel_height = 800.0f;
        this.desired_roen_pixel_width = 640.0f;
        this.desired_roen_fullscreen = false;
        init(translucent, depth, stencil);
    }
    public NINView(Context context) {
        super(context);
        this.x_viewscaling = 1.0f;
        this.y_viewscaling = 1.0f;
        this.desired_roen_pixel_height = 800.0f;
        this.desired_roen_pixel_width = 640.0f;
        this.desired_roen_fullscreen = false;
        init(true, 0, 0);
    }

    private void init(boolean translucent, int depth, int stencil) {
        pref = new Pref(this.getContext());

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                super.onLongPress(e);
                if (pref.hapticFeedbackBlocking()) {
                    NINView.this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
            }
        });

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
 */

/*
    static class LogWriter extends Writer {
        private StringBuilder mBuilder = new StringBuilder();

        LogWriter() {
        }

        @Override // java.io.Writer, java.io.Closeable, java.lang.AutoCloseable
        public void close() {
            flushBuilder();
        }

        @Override // java.io.Writer, java.io.Flushable
        public void flush() {
            flushBuilder();
        }

        @Override // java.io.Writer
        public void write(char[] buf, int offset, int count) {
            for (int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if (c == '\n') {
                    flushBuilder();
                } else {
                    this.mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (this.mBuilder.length() > 0) {
                Log.v(EXSurfaceView.TAG, this.mBuilder.toString());
                this.mBuilder.delete(0, this.mBuilder.length());
            }
        }
    }
 */

/*
        @Override // com.jormy.nin.EXSurfaceView.Renderer
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        }

 */
