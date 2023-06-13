package com.jormy.nin

data class RelayTouchInfo(
    @JvmField val touchid: Int,
    @JvmField val xPos: Float,
    @JvmField val yPos: Float,
    @JvmField val pressureValue: Float,
    @JvmField val areaValue: Float,
    @JvmField val timestamp_long: Long,
    @JvmField val jormactionid: Int,
)
