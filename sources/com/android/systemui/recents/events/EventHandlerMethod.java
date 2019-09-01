package com.android.systemui.recents.events;

import com.android.systemui.recents.events.RecentsEventBus;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* compiled from: RecentsEventBus */
class EventHandlerMethod {
    Class<? extends RecentsEventBus.Event> eventType;
    private Method mMethod;

    EventHandlerMethod(Method method, Class<? extends RecentsEventBus.Event> eventType2) {
        this.mMethod = method;
        this.mMethod.setAccessible(true);
        this.eventType = eventType2;
    }

    public void invoke(Object target, RecentsEventBus.Event event) throws InvocationTargetException, IllegalAccessException {
        this.mMethod.invoke(target, new Object[]{event});
    }

    public String toString() {
        return this.mMethod.getName() + "(" + this.eventType.getSimpleName() + ")";
    }
}
