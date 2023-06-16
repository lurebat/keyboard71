package com.jormy.nin;

@Api
public class NINView {
    @Api
    public static float getDevicePPI() {
        return com.lurebat.keyboard71.NINView.Companion.getDevicePPI();

    }
    @Api
    public static float getDevicePortraitWidth() {
        return com.lurebat.keyboard71.NINView.Companion.getDevicePortraitWidth();
    }

    @Api
    public static void adjustWantedScaling(float scaling) {
        com.lurebat.keyboard71.NINView.Companion.adjustWantedScaling(scaling);
    }

    @Api
    public static void onRoenSignalDirty() {
        com.lurebat.keyboard71.NINView.Companion.onRoenSignalDirty();
    }

    @Api
    public static void onRoenFrozennessChange(boolean truth) {
        com.lurebat.keyboard71.NINView.Companion.onRoenFrozennessChange(truth);
    }

    @Api
    public static void adjustKeyboardDimensions(float wantedRoenHeight, boolean fullscreen) {
        com.lurebat.keyboard71.NINView.Companion.adjustKeyboardDimensions(wantedRoenHeight, fullscreen);
    }
}
