package dev.ktr.shot;

import android.content.Context;
import android.content.SharedPreferences;

final class Config {
    static synchronized SharedPreferences prefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("shot", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("controllerSafetyResetV3", false)) {
            prefs.edit().putBoolean("enabled", false).putBoolean("filter", false)
                    .putBoolean("raw", true).putBoolean("single", false)
                    .putInt("first", 172).putInt("second", 310)
                    .putBoolean("controllerSafetyResetV3", true).apply();
        }
                if (!prefs.getBoolean("accessibilityMenuSelectV4", false)) {
                    prefs.edit().putBoolean("enabled", true).putBoolean("filter", false)
                        .putBoolean("raw", false).putBoolean("single", false)
                        .putInt("first", 139).putInt("second", 314)
                        .putBoolean("accessibilityMenuSelectV4", true).apply();
                }
        return prefs;
    }

    static String keyName(int code) {
        if (code == 139) return "MENU (139)";
        if (code == 172) return "FUNC (172)";
        if (code == 310) return "L1 (310)";
        if (code == 314) return "SELECT (314)";
        if (code == 315) return "START (315)";
        return "KEY " + code;
    }
}