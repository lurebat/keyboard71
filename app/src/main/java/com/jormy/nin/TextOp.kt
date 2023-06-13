package com.jormy.nin;

/* compiled from: SoftKeyboard.java */
/* loaded from: classes.dex */
public class TextOp {
    String a1;
    String a2;
    String a3;
    boolean boolarg;
    boolean boolarg2;
    int intarg1;
    int intarg2;
    String strarg;
    char type;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TextOp(char letype, int leintarg1, int leintarg2, boolean leboolarg, boolean leboolarg2, String lestrarg) {
        this.type = letype;
        this.intarg1 = leintarg1;
        this.intarg2 = leintarg2;
        this.strarg = lestrarg;
        this.boolarg = leboolarg;
        this.boolarg2 = leboolarg2;
    }

    @Override
    public String toString() {
        return "TextOp{" +
                "a1='" + a1 + '\'' +
                ", a2='" + a2 + '\'' +
                ", a3='" + a3 + '\'' +
                ", boolarg=" + boolarg +
                ", boolarg2=" + boolarg2 +
                ", intarg1=" + intarg1 +
                ", intarg2=" + intarg2 +
                ", strarg='" + strarg + '\'' +
                ", type=" + type +
                '}';
    }
}
