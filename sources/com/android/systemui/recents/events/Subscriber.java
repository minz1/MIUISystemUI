package com.android.systemui.recents.events;

import java.lang.ref.WeakReference;

/* compiled from: RecentsEventBus */
class Subscriber {
    private WeakReference<Object> mSubscriber;
    long registrationTime;

    Subscriber(Object subscriber, long registrationTime2) {
        this.mSubscriber = new WeakReference<>(subscriber);
        this.registrationTime = registrationTime2;
    }

    public String toString(int priority) {
        Object sub = this.mSubscriber.get();
        if (sub == null) {
            return "the Subscriber has been gc because of WeakReference";
        }
        String id = Integer.toHexString(System.identityHashCode(sub));
        return sub.getClass().getSimpleName() + " [0x" + id + ", P" + priority + "]";
    }

    public Object getReference() {
        return this.mSubscriber.get();
    }
}
