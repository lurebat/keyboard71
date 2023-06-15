package com.jormy.nin

@Api
class NINView {
    companion object {
        @Api
        @JvmStatic
        fun getDevicePPI(): Float {
            return com.lurebat.keyboard71.NINView.devicePPI

        }
        @Api
        @JvmStatic
        fun getDevicePortraitWidth(): Float {
            return com.lurebat.keyboard71.NINView.devicePortraitWidth
        }

        @Api
        @JvmStatic
        fun adjustWantedScaling(scaling: Float) {
            return com.lurebat.keyboard71.NINView.adjustWantedScaling(scaling)
        }

        @Api
        @JvmStatic
        fun onRoenSignalDirty() {
            return com.lurebat.keyboard71.NINView.onRoenSignalDirty()
        }

        @Api
        @JvmStatic
        fun onRoenFrozennessChange(truth: Boolean) {
            return com.lurebat.keyboard71.NINView.onRoenFrozennessChange(truth)
        }

        @Api
        @JvmStatic
        fun adjustKeyboardDimensions(wantedRoenHeight: Float, fullscreen: Boolean) {
            return com.lurebat.keyboard71.NINView.adjustKeyboardDimensions(wantedRoenHeight, fullscreen)
        }
    }
}
