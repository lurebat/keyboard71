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
struct SCShortcut {
    unsigned char field[8];
};
struct Moopad {
unsigned char field[8];
};
struct KeyboardPage {
    unsigned char field[8];
};
struct GlobalRoenGLInner {
    unsigned char _padding[0xc20];
    KeyboardPage* keyboardPage;
};

struct GlobalRoenGL {
    GlobalRoenGLInner* inner;
};

#pragma pack(pop)

extern "C" GlobalRoenGL* _ZN6RoenGL15getGlobalRoenGLEv();
extern "C" Moopad* _ZN12KeyboardPage9getMoopadEv(KeyboardPage* a);



extern "C" void* _ZN5Emkey10SCShortcut10makeActionERKSs(SCShortcut* a, std::string *b);
extern "C" void* _ZN5Emkey14SpaceCompleter13performActionERKNS_10SCShortcutE(SCShortcut *a, Moopad *b);

extern "C" JNIEXPORT jstring JNICALL Java_com_lurebat_keyboard71_Native_echo(JNIEnv* env,
                                                 jclass,
                                                 jstring echo) {

    auto globalRoenGL = _ZN6RoenGL15getGlobalRoenGLEv();
    auto keyboardPage = globalRoenGL->inner->keyboardPage;
    auto moopad = _ZN12KeyboardPage9getMoopadEv(keyboardPage);

    SCShortcut shortcut;
    std::string action = "linebksp";
    _ZN5Emkey10SCShortcut10makeActionERKSs(&shortcut, &action);
    _ZN5Emkey14SpaceCompleter13performActionERKNS_10SCShortcutE(&shortcut, moopad);


    return echo;

}
