package com.jormy.nin

/*
    public static String actionToString(int action) {
        switch (action) {
            case 0:
                return "Down";
            case 1:
                return "Up";
            case 2:
                return "Move";
            case 3:
                return "Cancel";
            case 4:
                return "Outside";
            case 5:
                return "Pointer Down";
            case 6:
                return "Pointer Up";
            default:
                return "";
        }
    }

    public static int actionToJormyAction(int action) {
        switch (action) {
            case 0:
            case 5:
                return 0;
            case 1:
                return 2;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return -1;
            case 6:
                return 2;
            default:
                return -1;
        }
    }
 */
enum class JormyAction(val value: Int) {
    Action0(0),
    Action1(1),
    Action2(2),
    ActionNegative1(-1)
}
