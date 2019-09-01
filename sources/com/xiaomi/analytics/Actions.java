package com.xiaomi.analytics;

public class Actions {
    public static AdAction newAdAction(String actionType) {
        return new AdAction(actionType);
    }
}
