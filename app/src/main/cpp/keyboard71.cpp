// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("keyboard72");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("keyboard72")
//      }
//    }

#import <jni.h>
#import <string>

#pragma pack(push, 1)

struct theirstring {
    int length;
    char padding[0x8];
    char string[0x20];
};
struct SCShortcut {
    void *unk;
    char *category;
    char *action;
};

struct Moopad {
    unsigned char field[8];
};

struct MoopadWrapper {
    unsigned char _padding[0x18];
    Moopad *shortcut_ptr;
};

struct KeyboardPage {
    unsigned char field[8];
};
struct GlobalRoenGLInner {
    unsigned char _padding[0xc20];
    KeyboardPage *keyboardPage;
};

struct GlobalRoenGL {
    GlobalRoenGLInner *inner;
};

struct StringR {
    unsigned char _padding[0x20];
};

#pragma pack(pop)

extern "C" GlobalRoenGL *_ZN6RoenGL15getGlobalRoenGLEv();
extern "C" Moopad *_ZN12KeyboardPage9getMoopadEv(KeyboardPage *a);
extern "C" void _ZN5Emkey10SCShortcutC2Ev(SCShortcut *a);


extern "C" void *_ZN5Emkey10SCShortcut10makeActionERKSs(SCShortcut *a, void *b);
extern "C" void *_ZN5Emkey14SpaceCompleter13performActionERKNS_10SCShortcutE(void *a, void *b);
extern "C" void _ZN5Shing7StringRC2EPKci(StringR *a, const char *s, int len);
extern "C" theirstring *_ZNK5Shing7StringR8toStringEv(StringR *a, int *type);
extern "C" void _ZN5Shing7StringRC2Ev(StringR *a);
extern "C" void _ZNK5Shing7StringR6appendEh(StringR *a, char b);
extern "C" JNIEXPORT void JNICALL Java_com_lurebat_keyboard71_Native_run_1shortcut(JNIEnv *env,
                                                                                   jclass,
                                                                                   jchar category_jstring,
                                                                                   jstring action_jstring) {

    auto globalRoenGL = _ZN6RoenGL15getGlobalRoenGLEv();
    auto keyboardPage = globalRoenGL->inner->keyboardPage;
    auto moopad = _ZN12KeyboardPage9getMoopadEv(keyboardPage);
    auto *shortcut = new SCShortcut();

    auto *category = new theirstring();
    category->length = 1;
    category->string[0] = category_jstring;
    shortcut->category = category->string;

    auto *action = new theirstring();
    action->length = env->GetStringLength(action_jstring);
    memcpy(action->string, env->GetStringUTFChars(action_jstring, nullptr), action->length);
    shortcut->action = action->string;

    MoopadWrapper wrapper = {0};
    wrapper.shortcut_ptr = moopad;

    _ZN5Emkey14SpaceCompleter13performActionERKNS_10SCShortcutE(&wrapper, shortcut);

    delete action;
    delete category;
    delete shortcut;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_jormy_nin_NINLib_getUnicodeBackIndex(JNIEnv *env, jclass clazz, jstring str, jint i);
extern "C"
JNIEXPORT jint JNICALL
Java_com_jormy_nin_NINLib_getUnicodeFrontIndex(JNIEnv *env, jclass clazz, jstring str, jint i);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_init(JNIEnv *env, jclass clazz, jint i, jint i2, jint i3, jint i4);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_memTestStep(JNIEnv *env, jclass clazz);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_onChangeAppOrTextbox(JNIEnv *env, jclass clazz, jstring str, jstring str2,
                                               jstring str3);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_onEditorChangeTypeClass(JNIEnv *env, jclass clazz, jstring str,
                                                  jstring str2);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_onExternalSelChange(JNIEnv *env, jclass clazz);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_onTextSelection(JNIEnv *env, jclass clazz, jstring str, jstring str2,
                                          jstring str3, jstring str4);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_onTouchEvent(JNIEnv *env, jclass clazz, jint i, jint i2, jfloat f,
                                       jfloat f2, jfloat f3, jfloat f4, jlong j);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_onWordDestruction(JNIEnv *env, jclass clazz, jstring str, jstring str2);
extern "C"
JNIEXPORT jint JNICALL
Java_com_jormy_nin_NINLib_processBackspaceAllowance(JNIEnv *env, jclass clazz, jstring str,
                                                    jstring str2, jint i);
extern "C"
JNIEXPORT jlong JNICALL
Java_com_jormy_nin_NINLib_processSoftKeyboardCursorMovementLeft(JNIEnv *env, jclass clazz,
                                                                jstring str);
extern "C"
JNIEXPORT jlong JNICALL
Java_com_jormy_nin_NINLib_processSoftKeyboardCursorMovementRight(JNIEnv *env, jclass clazz,
                                                                 jstring str);
extern "C"
JNIEXPORT void JNICALL
Java_com_jormy_nin_NINLib_step(JNIEnv *env, jclass clazz);
extern "C"
JNIEXPORT jlong JNICALL
Java_com_jormy_nin_NINLib_syncTiming(JNIEnv *env, jclass clazz, jlong j);

extern "C"
JNIEXPORT jint JNICALL
Java_com_lurebat_keyboard71_Native_getUnicodeBackIndex(JNIEnv *env, jclass clazz, jstring str,
                                                       jint i) {
    return Java_com_jormy_nin_NINLib_getUnicodeBackIndex(env, clazz, str, i);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_lurebat_keyboard71_Native_getUnicodeFrontIndex(JNIEnv *env, jclass clazz, jstring str,
                                                        jint i) {
    return Java_com_jormy_nin_NINLib_getUnicodeFrontIndex(env, clazz, str, i);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_init(JNIEnv *env, jclass clazz, jint i, jint i2, jint i3,
                                        jint i4) {
    Java_com_jormy_nin_NINLib_init(env, clazz, i, i2, i3, i4);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_memTestStep(JNIEnv *env, jclass clazz) {
    Java_com_jormy_nin_NINLib_memTestStep(env, clazz);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_onChangeAppOrTextbox(JNIEnv *env, jclass clazz, jstring str,
                                                        jstring str2,
                                                        jstring str3) {
    Java_com_jormy_nin_NINLib_onChangeAppOrTextbox(env, clazz, str, str2, str3);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_onEditorChangeTypeClass(JNIEnv *env, jclass clazz, jstring str,
                                                           jstring str2) {
    Java_com_jormy_nin_NINLib_onEditorChangeTypeClass(env, clazz, str, str2);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_onExternalSelChange(JNIEnv *env, jclass clazz) {
    Java_com_jormy_nin_NINLib_onExternalSelChange(env, clazz);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_onTextSelection(JNIEnv *env, jclass clazz, jstring str,
                                                   jstring str2,
                                                   jstring str3, jstring str4) {
    Java_com_jormy_nin_NINLib_onTextSelection(env, clazz, str, str2, str3, str4);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_onTouchEvent(JNIEnv *env, jclass clazz, jint i, jint i2,
                                                jfloat f,
                                                jfloat f2, jfloat f3, jfloat f4, jlong j) {
    Java_com_jormy_nin_NINLib_onTouchEvent(env, clazz, i, i2, f, f2, f3, f4, j);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_onWordDestruction(JNIEnv *env, jclass clazz, jstring str,
                                                     jstring str2) {
    Java_com_jormy_nin_NINLib_onWordDestruction(env, clazz, str, str2);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_lurebat_keyboard71_Native_processBackspaceAllowance(JNIEnv *env, jclass clazz, jstring str,
                                                             jstring str2, jint i) {
    return Java_com_jormy_nin_NINLib_processBackspaceAllowance(env, clazz, str, str2, i);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_lurebat_keyboard71_Native_processSoftKeyboardCursorMovementLeft(JNIEnv *env, jclass clazz,
                                                                         jstring str) {
    return Java_com_jormy_nin_NINLib_processSoftKeyboardCursorMovementLeft(env, clazz, str);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_lurebat_keyboard71_Native_processSoftKeyboardCursorMovementRight(JNIEnv *env, jclass clazz,
                                                                          jstring str) {
    return Java_com_jormy_nin_NINLib_processSoftKeyboardCursorMovementRight(env, clazz, str);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lurebat_keyboard71_Native_step(JNIEnv *env, jclass clazz) {
    Java_com_jormy_nin_NINLib_step(env, clazz);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_lurebat_keyboard71_Native_syncTiming(JNIEnv *env, jclass clazz, jlong j) {
    return Java_com_jormy_nin_NINLib_syncTiming(env, clazz, j);
}
