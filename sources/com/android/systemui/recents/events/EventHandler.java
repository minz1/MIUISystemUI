package com.android.systemui.recents.events;

/* compiled from: RecentsEventBus */
class EventHandler {
    EventHandlerMethod method;
    int priority;
    Subscriber subscriber;

    EventHandler(Subscriber subscriber2, EventHandlerMethod method2, int priority2) {
        this.subscriber = subscriber2;
        this.method = method2;
        this.priority = priority2;
    }

    public String toString() {
        return this.subscriber.toString(this.priority) + " " + this.method.toString();
    }
}
