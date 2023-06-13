package com.jormy.nin;

/* compiled from: NINView.java */
/* loaded from: classes.dex */
class RelayTouchInfo {
    @Override
    public String toString() {
        return "RelayTouchInfo{" +
                "areaValue=" + areaValue +
                ", jormactionid=" + jormactionid +
                ", pressureValue=" + pressureValue +
                ", timestamp_long=" + timestamp_long +
                ", touchid=" + touchid +
                ", xPos=" + xPos +
                ", yPos=" + yPos +
                '}';
    }

    public float areaValue;
    public int jormactionid;
    public float pressureValue;
    public long timestamp_long;
    public int touchid;
    public float xPos;
    public float yPos;
}
