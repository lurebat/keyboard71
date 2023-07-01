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
struct SCShortcuts {
    unsigned char field[8];
};
struct Moopads {
unsigned char field[8];
};
struct KeyboardPages {
    unsigned char field[8];
};
struct GlobalRoenGLInner {
    unsigned char _padding[0xc20];
    KeyboardPages* keyboardPage;
};

struct GlobalRoenGL {
    GlobalRoenGLInner* inner;
};

#pragma pack(pop)

extern "C" {
    namespace RoenGL {
        GlobalRoenGL* getGlobalRoenGL();
    }
    namespace KeyboardPage {
        Moopads* getMoopad(KeyboardPages* keyboardPage);
    }
    namespace Emkey {
        namespace SCShortcut {
            void* makeAction(SCShortcuts* shortcut, std::string* action);
            void* performAction(SCShortcuts* shortcut, Moopads* moopad);
        }
    }
}




extern "C" JNIEXPORT jstring JNICALL Java_com_lurebat_keyboard71_Native_echo(JNIEnv* env,
                                                 jclass,
                                                 jstring echo) {
    auto globalRoenGL = RoenGL::getGlobalRoenGL();
    auto keyboardPage = globalRoenGL->inner->keyboardPage;
    auto moopad = KeyboardPage::getMoopad(keyboardPage);

    SCShortcuts shortcut = {};
    std::string action = "linebksp";
    Emkey::SCShortcut::makeAction(&shortcut, &action);
    Emkey::SCShortcut::performAction(&shortcut, moopad);

    return echo;

}
