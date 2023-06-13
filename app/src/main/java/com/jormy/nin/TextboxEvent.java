package com.jormy.nin;

/* compiled from: SoftKeyboard.java */
/* loaded from: classes.dex */
class TextboxEvent {
    String arg1;
    String arg2;
    String codemode;
    String mainarg;
    TextboxEventType type;

    /* JADX INFO: Access modifiers changed from: package-private */
    public TextboxEvent(TextboxEventType thetype, String ein, String zwei, String drei) {
        this.type = thetype;
        this.mainarg = ein;
        this.arg1 = zwei;
        this.arg2 = drei;
    }
}
