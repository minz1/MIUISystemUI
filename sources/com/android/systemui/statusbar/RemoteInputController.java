package com.android.systemui.statusbar;

import android.util.ArrayMap;
import android.util.Pair;
import com.android.internal.util.Preconditions;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RemoteInputController {
    private final ArrayList<Callback> mCallbacks = new ArrayList<>(3);
    private final HeadsUpManager mHeadsUpManager;
    private final ArrayList<Pair<WeakReference<NotificationData.Entry>, Object>> mOpen = new ArrayList<>();
    private final ArrayMap<String, Object> mSpinning = new ArrayMap<>();

    public interface Callback {
        void onRemoteInputActive(boolean z);

        void onRemoteInputSent(NotificationData.Entry entry);
    }

    public RemoteInputController(HeadsUpManager headsUpManager) {
        addCallback((Callback) Dependency.get(StatusBarWindowManager.class));
        this.mHeadsUpManager = headsUpManager;
    }

    public void addRemoteInput(NotificationData.Entry entry, Object token) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(token);
        if (!pruneWeakThenRemoveAndContains(entry, null, token)) {
            this.mOpen.add(new Pair(new WeakReference(entry), token));
        }
        apply(entry);
    }

    public void removeRemoteInput(NotificationData.Entry entry, Object token) {
        Preconditions.checkNotNull(entry);
        pruneWeakThenRemoveAndContains(null, entry, token);
        apply(entry);
    }

    public void addSpinning(String key, Object token) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(token);
        this.mSpinning.put(key, token);
    }

    public void removeSpinning(String key, Object token) {
        Preconditions.checkNotNull(key);
        if (token == null || this.mSpinning.get(key) == token) {
            this.mSpinning.remove(key);
        }
    }

    public boolean isSpinning(String key) {
        return this.mSpinning.containsKey(key);
    }

    private void apply(NotificationData.Entry entry) {
        this.mHeadsUpManager.setRemoteInputActive(entry, isRemoteInputActive(entry));
        boolean remoteInputActive = isRemoteInputActive();
        int N = this.mCallbacks.size();
        for (int i = 0; i < N; i++) {
            this.mCallbacks.get(i).onRemoteInputActive(remoteInputActive);
        }
    }

    public boolean isRemoteInputActive(NotificationData.Entry entry) {
        return pruneWeakThenRemoveAndContains(entry, null, null);
    }

    public boolean isRemoteInputActive() {
        pruneWeakThenRemoveAndContains(null, null, null);
        return !this.mOpen.isEmpty();
    }

    private boolean pruneWeakThenRemoveAndContains(NotificationData.Entry contains, NotificationData.Entry remove, Object removeToken) {
        boolean found = false;
        for (int i = this.mOpen.size() - 1; i >= 0; i--) {
            NotificationData.Entry item = (NotificationData.Entry) ((WeakReference) this.mOpen.get(i).first).get();
            Object itemToken = this.mOpen.get(i).second;
            boolean removeTokenMatches = removeToken == null || itemToken == removeToken;
            if (item == null || (item == remove && removeTokenMatches)) {
                this.mOpen.remove(i);
            } else if (item == contains) {
                if (removeToken == null || removeToken == itemToken) {
                    found = true;
                } else {
                    this.mOpen.remove(i);
                }
            }
        }
        return found;
    }

    public void addCallback(Callback callback) {
        Preconditions.checkNotNull(callback);
        this.mCallbacks.add(callback);
    }

    public void remoteInputSent(NotificationData.Entry entry) {
        int N = this.mCallbacks.size();
        for (int i = 0; i < N; i++) {
            this.mCallbacks.get(i).onRemoteInputSent(entry);
        }
    }

    public void closeRemoteInputs() {
        if (this.mOpen.size() != 0) {
            ArrayList<NotificationData.Entry> list = new ArrayList<>(this.mOpen.size());
            for (int i = this.mOpen.size() - 1; i >= 0; i--) {
                NotificationData.Entry item = (NotificationData.Entry) ((WeakReference) this.mOpen.get(i).first).get();
                if (!(item == null || item.row == null)) {
                    list.add(item);
                }
            }
            for (int i2 = list.size() - 1; i2 >= 0; i2--) {
                NotificationData.Entry item2 = list.get(i2);
                if (item2.row != null) {
                    item2.row.closeRemoteInput();
                }
            }
        }
    }
}
