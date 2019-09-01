package com.android.systemui;

import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import java.util.Arrays;

public class ForegroundServiceControllerImpl implements ForegroundServiceController {
    private final Object mMutex = new Object();
    private final SparseArray<UserServices> mUserServices = new SparseArray<>();

    private static class UserServices {
        private ArrayMap<String, ArraySet<String>> mNotifications;
        private String[] mRunning;

        private UserServices() {
            this.mRunning = null;
            this.mNotifications = new ArrayMap<>(1);
        }

        public void setRunningServices(String[] pkgs) {
            this.mRunning = pkgs != null ? (String[]) Arrays.copyOf(pkgs, pkgs.length) : null;
        }

        public void addNotification(String pkg, String key) {
            if (this.mNotifications.get(pkg) == null) {
                this.mNotifications.put(pkg, new ArraySet());
            }
            this.mNotifications.get(pkg).add(key);
        }

        public boolean removeNotification(String pkg, String key) {
            ArraySet<String> keys = this.mNotifications.get(pkg);
            if (keys == null) {
                return false;
            }
            boolean found = keys.remove(key);
            if (keys.size() != 0) {
                return found;
            }
            this.mNotifications.remove(pkg);
            return found;
        }

        public boolean isDungeonNeeded() {
            if (this.mRunning != null) {
                for (String pkg : this.mRunning) {
                    ArraySet<String> set = this.mNotifications.get(pkg);
                    if (set == null || set.size() == 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public ForegroundServiceControllerImpl(Context context) {
    }

    public boolean isDungeonNeededForUser(int userId) {
        synchronized (this.mMutex) {
            UserServices services = this.mUserServices.get(userId);
            if (services == null) {
                return false;
            }
            boolean isDungeonNeeded = services.isDungeonNeeded();
            return isDungeonNeeded;
        }
    }

    public void addNotification(StatusBarNotification sbn, int importance) {
        updateNotification(sbn, importance);
    }

    public boolean removeNotification(StatusBarNotification sbn) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(sbn.getUserId());
            if (userServices == null) {
                return false;
            }
            if (isDungeonNotification(sbn)) {
                userServices.setRunningServices(null);
                return true;
            }
            boolean removeNotification = userServices.removeNotification(sbn.getPackageName(), sbn.getKey());
            return removeNotification;
        }
    }

    public void updateNotification(StatusBarNotification sbn, int newImportance) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(sbn.getUserId());
            if (userServices == null) {
                userServices = new UserServices();
                this.mUserServices.put(sbn.getUserId(), userServices);
            }
            if (isDungeonNotification(sbn)) {
                Bundle extras = sbn.getNotification().extras;
                if (extras != null) {
                    userServices.setRunningServices(extras.getStringArray("android.foregroundApps"));
                }
            } else {
                userServices.removeNotification(sbn.getPackageName(), sbn.getKey());
                if ((sbn.getNotification().flags & 64) != 0 && newImportance > 1) {
                    userServices.addNotification(sbn.getPackageName(), sbn.getKey());
                }
            }
        }
    }

    public boolean isDungeonNotification(StatusBarNotification sbn) {
        return sbn.getId() == 40 && sbn.getTag() == null && sbn.getPackageName().equals("android");
    }
}
