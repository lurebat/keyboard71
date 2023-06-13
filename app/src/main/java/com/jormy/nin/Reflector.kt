package com.jormy.nin;

import java.lang.reflect.Method;

/* loaded from: classes.dex */
class Reflector {
    Reflector() {
    }

    public static String analyzeClass(String leclassname) {
        try {
            Class leclass = Class.forName(leclassname);
            StringBuilder bi = new StringBuilder();
            Method[] aClassMethods = leclass.getDeclaredMethods();
            for (Method m : aClassMethods) {
                bi.append(m.toGenericString());
                bi.append("\n");
            }
            return bi.toString();
        } catch (ClassNotFoundException e) {
            return "-- class not found!";
        } catch (Exception e2) {
            return "-- exception : " + e2.toString();
        }
    }
}
