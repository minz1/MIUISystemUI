package com.android.systemui.recents.events;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.util.MutableBoolean;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class RecentsEventBus extends BroadcastReceiver {
    public static boolean DEBUG_TRACE_ALL = true;
    private static final Comparator<EventHandler> EVENT_HANDLER_COMPARATOR = new Comparator<EventHandler>() {
        public int compare(EventHandler h1, EventHandler h2) {
            if (h1.priority != h2.priority) {
                return h2.priority - h1.priority;
            }
            return Long.compare(h2.subscriber.registrationTime, h1.subscriber.registrationTime);
        }
    };
    private static volatile RecentsEventBus sDefaultBus;
    private static final Object sLock = new Object();
    private int mCallCount;
    private long mCallDurationMicros;
    private HashMap<Class<? extends Event>, ArrayList<EventHandler>> mEventTypeMap = new HashMap<>();
    private Handler mHandler;
    private HashMap<String, Class<? extends InterprocessEvent>> mInterprocessEventNameMap = new HashMap<>();
    private HashMap<Class<? extends Object>, ArrayList<EventHandlerMethod>> mSubscriberTypeMap = new HashMap<>();
    private ArrayList<Subscriber> mSubscribers = new ArrayList<>();

    public static class AnimatedEvent extends Event {
        private final ReferenceCountedTrigger mTrigger = new ReferenceCountedTrigger();

        protected AnimatedEvent() {
        }

        public ReferenceCountedTrigger getAnimationTrigger() {
            return this.mTrigger;
        }

        public void addPostAnimationCallback(Runnable r) {
            this.mTrigger.addLastDecrementRunnable(r);
        }

        /* access modifiers changed from: package-private */
        public void onPreDispatch() {
            this.mTrigger.increment();
        }

        /* access modifiers changed from: package-private */
        public void onPostDispatch() {
            this.mTrigger.decrement();
        }

        /* access modifiers changed from: protected */
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }

    public static class Event implements Cloneable {
        boolean cancelled;
        boolean requiresPost;
        boolean trace = false;

        protected Event() {
        }

        /* access modifiers changed from: package-private */
        public void onPreDispatch() {
        }

        /* access modifiers changed from: package-private */
        public void onPostDispatch() {
        }

        /* access modifiers changed from: protected */
        public Object clone() throws CloneNotSupportedException {
            Event evt = (Event) super.clone();
            evt.cancelled = false;
            return evt;
        }

        public String description() {
            return null;
        }
    }

    public static class InterprocessEvent extends Event {
    }

    public static class ReusableEvent extends Event {
        private int mDispatchCount;

        protected ReusableEvent() {
        }

        /* access modifiers changed from: package-private */
        public void onPostDispatch() {
            super.onPostDispatch();
            this.mDispatchCount++;
        }

        /* access modifiers changed from: protected */
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }

    private RecentsEventBus(Looper looper) {
        this.mHandler = new Handler(looper);
    }

    public static RecentsEventBus getDefault() {
        if (sDefaultBus == null) {
            synchronized (sLock) {
                if (sDefaultBus == null) {
                    if (DEBUG_TRACE_ALL) {
                        logWithPid("New EventBus");
                    }
                    sDefaultBus = new RecentsEventBus(Looper.getMainLooper());
                }
            }
        }
        return sDefaultBus;
    }

    public void register(Object subscriber) {
        registerSubscriber(subscriber, 1, null);
    }

    public void register(Object subscriber, int priority) {
        registerSubscriber(subscriber, priority, null);
    }

    public void unregister(Object subscriber) {
        if (DEBUG_TRACE_ALL) {
            logWithPid("unregister()");
        }
        if (Thread.currentThread().getId() != this.mHandler.getLooper().getThread().getId()) {
            throw new RuntimeException("Can not unregister() a subscriber from a non-main thread.");
        } else if (findRegisteredSubscriber(subscriber, true)) {
            ArrayList<EventHandlerMethod> subscriberMethods = this.mSubscriberTypeMap.get(subscriber.getClass());
            if (subscriberMethods != null) {
                Iterator<EventHandlerMethod> it = subscriberMethods.iterator();
                while (it.hasNext()) {
                    ArrayList<EventHandler> eventHandlers = this.mEventTypeMap.get(it.next().eventType);
                    for (int i = eventHandlers.size() - 1; i >= 0; i--) {
                        if (eventHandlers.get(i).subscriber.getReference() == subscriber) {
                            eventHandlers.remove(i);
                        }
                    }
                }
            }
        }
    }

    public void send(Event event) {
        String str;
        if (Thread.currentThread().getId() == this.mHandler.getLooper().getThread().getId()) {
            if (DEBUG_TRACE_ALL) {
                StringBuilder sb = new StringBuilder();
                sb.append("send(");
                sb.append(event.getClass().getSimpleName());
                if (event.description() != null) {
                    str = "[" + event.description() + "]";
                } else {
                    str = "";
                }
                sb.append(str);
                sb.append(")");
                logWithPid(sb.toString());
            }
            event.requiresPost = false;
            event.cancelled = false;
            queueEvent(event);
            return;
        }
        throw new RuntimeException("Can not send() a message from a non-main thread.");
    }

    public void post(Event event) {
        String str;
        if (DEBUG_TRACE_ALL) {
            StringBuilder sb = new StringBuilder();
            sb.append("post(");
            sb.append(event.getClass().getSimpleName());
            if (event.description() != null) {
                str = "[" + event.description() + "]";
            } else {
                str = "";
            }
            sb.append(str);
            sb.append(")");
            logWithPid(sb.toString());
        }
        event.requiresPost = true;
        event.cancelled = false;
        queueEvent(event);
    }

    public void sendOntoMainThread(Event event) {
        if (Thread.currentThread().getId() != this.mHandler.getLooper().getThread().getId()) {
            post(event);
        } else {
            send(event);
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (DEBUG_TRACE_ALL) {
            logWithPid("onReceive(" + intent.getAction() + ", user " + UserHandle.myUserId() + ")");
        }
        Bundle eventBundle = intent.getBundleExtra("interprocess_event_bundle");
        try {
            send((Event) this.mInterprocessEventNameMap.get(intent.getAction()).getConstructor(new Class[]{Bundle.class}).newInstance(new Object[]{eventBundle}));
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Log.e("EventBus", "Failed to create InterprocessEvent", e.getCause());
        }
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.println(dumpInternal(prefix));
    }

    public String dumpInternal(String prefix) {
        String innerPrefix = prefix + "  ";
        String innerInnerPrefix = innerPrefix + "  ";
        StringBuilder output = new StringBuilder();
        output.append(prefix);
        output.append("Registered class types:");
        output.append("\n");
        ArrayList<Class<?>> subsciberTypes = new ArrayList<>(this.mSubscriberTypeMap.keySet());
        Collections.sort(subsciberTypes, new Comparator<Class<?>>() {
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });
        for (int i = 0; i < subsciberTypes.size(); i++) {
            output.append(innerPrefix);
            output.append(subsciberTypes.get(i).getSimpleName());
            output.append("\n");
        }
        output.append(prefix);
        output.append("Event map:");
        output.append("\n");
        ArrayList<Class<?>> classes = new ArrayList<>(this.mEventTypeMap.keySet());
        Collections.sort(classes, new Comparator<Class<?>>() {
            public int compare(Class<?> o1, Class<?> o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });
        for (int i2 = 0; i2 < classes.size(); i2++) {
            Class<?> clz = classes.get(i2);
            output.append(innerPrefix);
            output.append(clz.getSimpleName());
            output.append(" -> ");
            output.append("\n");
            Iterator<EventHandler> it = this.mEventTypeMap.get(clz).iterator();
            while (it.hasNext()) {
                Object subscriber = it.next().subscriber.getReference();
                if (subscriber != null) {
                    String id = Integer.toHexString(System.identityHashCode(subscriber));
                    output.append(innerInnerPrefix);
                    output.append(subscriber.getClass().getSimpleName());
                    output.append(" [0x" + id + ", #" + handler.priority + "]");
                    output.append("\n");
                }
            }
        }
        return output.toString();
    }

    /* JADX WARNING: type inference failed for: r0v6 */
    /* JADX WARNING: type inference failed for: r0v7, types: [boolean] */
    /* JADX WARNING: type inference failed for: r0v11 */
    private void registerSubscriber(Object subscriber, int priority, MutableBoolean hasInterprocessEventsChangedOut) {
        int i;
        Method[] methods;
        Class<?> subscriberType;
        boolean z;
        ArrayList<EventHandler> eventTypeHandlers;
        RecentsEventBus recentsEventBus = this;
        Object obj = subscriber;
        int i2 = priority;
        MutableBoolean mutableBoolean = hasInterprocessEventsChangedOut;
        long callingThreadId = Thread.currentThread().getId();
        if (callingThreadId == recentsEventBus.mHandler.getLooper().getThread().getId()) {
            ? r0 = 0;
            if (!recentsEventBus.findRegisteredSubscriber(obj, false)) {
                long t1 = 0;
                if (DEBUG_TRACE_ALL) {
                    t1 = SystemClock.currentTimeMicro();
                    logWithPid("registerSubscriber(" + subscriber.getClass().getSimpleName() + ")");
                }
                Subscriber sub = new Subscriber(obj, SystemClock.uptimeMillis());
                Class<?> subscriberType2 = subscriber.getClass();
                ArrayList<EventHandlerMethod> subscriberMethods = recentsEventBus.mSubscriberTypeMap.get(subscriberType2);
                if (subscriberMethods != null) {
                    if (DEBUG_TRACE_ALL) {
                        logWithPid("Subscriber class type already registered");
                    }
                    Iterator<EventHandlerMethod> it = subscriberMethods.iterator();
                    while (it.hasNext()) {
                        EventHandlerMethod method = it.next();
                        ArrayList<EventHandler> eventTypeHandlers2 = recentsEventBus.mEventTypeMap.get(method.eventType);
                        eventTypeHandlers2.add(new EventHandler(sub, method, i2));
                        recentsEventBus.sortEventHandlersByPriority(eventTypeHandlers2);
                    }
                    recentsEventBus.mSubscribers.add(sub);
                    return;
                }
                if (DEBUG_TRACE_ALL) {
                    logWithPid("Subscriber class type requires registration");
                }
                ArrayList arrayList = new ArrayList();
                recentsEventBus.mSubscriberTypeMap.put(subscriberType2, arrayList);
                recentsEventBus.mSubscribers.add(sub);
                MutableBoolean isInterprocessEvent = new MutableBoolean(false);
                Method[] methods2 = subscriberType2.getMethods();
                int length = methods2.length;
                int i3 = 0;
                while (i3 < length) {
                    long callingThreadId2 = callingThreadId;
                    Method m = methods2[i3];
                    Class<? extends Event>[] parameterTypes = m.getParameterTypes();
                    isInterprocessEvent.value = r0;
                    if (recentsEventBus.isValidEventBusHandlerMethod(m, parameterTypes, isInterprocessEvent)) {
                        subscriberType = subscriberType2;
                        Class<? extends Event> eventType = parameterTypes[r0];
                        ArrayList<EventHandler> eventTypeHandlers3 = recentsEventBus.mEventTypeMap.get(eventType);
                        if (eventTypeHandlers3 == null) {
                            ArrayList<EventHandler> arrayList2 = eventTypeHandlers3;
                            ArrayList<EventHandler> eventTypeHandlers4 = new ArrayList<>();
                            methods = methods2;
                            recentsEventBus.mEventTypeMap.put(eventType, eventTypeHandlers4);
                            eventTypeHandlers = eventTypeHandlers4;
                        } else {
                            methods = methods2;
                            eventTypeHandlers = eventTypeHandlers3;
                        }
                        if (isInterprocessEvent.value) {
                            i = length;
                            try {
                                eventType.getConstructor(new Class[]{Bundle.class});
                                recentsEventBus.mInterprocessEventNameMap.put(eventType.getName(), eventType);
                                if (mutableBoolean != null) {
                                    mutableBoolean.value = true;
                                }
                            } catch (NoSuchMethodException e) {
                                throw new RuntimeException("Expected InterprocessEvent to have a Bundle constructor");
                            }
                        } else {
                            i = length;
                        }
                        EventHandlerMethod method2 = new EventHandlerMethod(m, eventType);
                        eventTypeHandlers.add(new EventHandler(sub, method2, i2));
                        arrayList.add(method2);
                        recentsEventBus.sortEventHandlersByPriority(eventTypeHandlers);
                        if (DEBUG_TRACE_ALL) {
                            EventHandlerMethod eventHandlerMethod = method2;
                            StringBuilder sb = new StringBuilder();
                            sb.append("  * Method: ");
                            sb.append(m.getName());
                            sb.append(" event: ");
                            z = false;
                            sb.append(parameterTypes[0].getSimpleName());
                            sb.append(" interprocess? ");
                            sb.append(isInterprocessEvent.value);
                            logWithPid(sb.toString());
                        } else {
                            z = false;
                        }
                    } else {
                        z = r0;
                        subscriberType = subscriberType2;
                        methods = methods2;
                        i = length;
                    }
                    i3++;
                    r0 = z;
                    callingThreadId = callingThreadId2;
                    subscriberType2 = subscriberType;
                    methods2 = methods;
                    length = i;
                    recentsEventBus = this;
                    i2 = priority;
                }
                Class<?> cls = subscriberType2;
                Method[] methodArr = methods2;
                if (DEBUG_TRACE_ALL) {
                    logWithPid("Registered " + subscriber.getClass().getSimpleName() + " in " + (SystemClock.currentTimeMicro() - t1) + " microseconds");
                }
                return;
            }
            return;
        }
        throw new RuntimeException("Can not register() a subscriber from a non-main thread.");
    }

    private void queueEvent(final Event event) {
        ArrayList<EventHandler> eventHandlers = this.mEventTypeMap.get(event.getClass());
        if (eventHandlers != null) {
            boolean hasPostedEvent = false;
            event.onPreDispatch();
            ArrayList<EventHandler> eventHandlers2 = (ArrayList) eventHandlers.clone();
            int eventHandlerCount = eventHandlers2.size();
            for (int i = 0; i < eventHandlerCount; i++) {
                final EventHandler eventHandler = eventHandlers2.get(i);
                if (eventHandler.subscriber.getReference() != null) {
                    if (event.requiresPost) {
                        this.mHandler.post(new Runnable() {
                            public void run() {
                                RecentsEventBus.this.processEvent(eventHandler, event);
                            }
                        });
                        hasPostedEvent = true;
                    } else {
                        processEvent(eventHandler, event);
                    }
                }
            }
            if (hasPostedEvent) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        event.onPostDispatch();
                    }
                });
            } else {
                event.onPostDispatch();
            }
        }
    }

    /* access modifiers changed from: private */
    public void processEvent(EventHandler eventHandler, Event event) {
        if (event.cancelled) {
            if (event.trace || DEBUG_TRACE_ALL) {
                logWithPid("Event dispatch cancelled");
            }
            return;
        }
        try {
            if (event.trace || DEBUG_TRACE_ALL) {
                logWithPid(" -> " + eventHandler.toString());
            }
            Object sub = eventHandler.subscriber.getReference();
            if (sub != null) {
                long t1 = 0;
                if (DEBUG_TRACE_ALL) {
                    t1 = SystemClock.currentTimeMicro();
                }
                eventHandler.method.invoke(sub, event);
                if (DEBUG_TRACE_ALL) {
                    this.mCallDurationMicros += SystemClock.currentTimeMicro() - t1;
                    this.mCallCount++;
                    logWithPid(eventHandler.method.toString() + " duration: " + duration + " microseconds, avg: " + (this.mCallDurationMicros / ((long) this.mCallCount)));
                }
            } else {
                Log.e("EventBus", "Failed to deliver event to null subscriber");
            }
        } catch (IllegalAccessException e) {
            Log.e("EventBus", "Failed to invoke method", e.getCause());
        } catch (InvocationTargetException e2) {
            throw new RuntimeException(e2.getCause());
        }
    }

    private boolean findRegisteredSubscriber(Object subscriber, boolean removeFoundSubscriber) {
        for (int i = this.mSubscribers.size() - 1; i >= 0; i--) {
            if (this.mSubscribers.get(i).getReference() == subscriber) {
                if (removeFoundSubscriber) {
                    this.mSubscribers.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isValidEventBusHandlerMethod(Method method, Class<?>[] parameterTypes, MutableBoolean isInterprocessEventOut) {
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || !Modifier.isFinal(modifiers) || !method.getReturnType().equals(Void.TYPE) || parameterTypes.length != 1) {
            if (DEBUG_TRACE_ALL) {
                if (!Modifier.isPublic(modifiers)) {
                    logWithPid("  Expected method to be public: " + method.getName());
                } else if (!Modifier.isFinal(modifiers)) {
                    logWithPid("  Expected method to be final: " + method.getName());
                } else if (!method.getReturnType().equals(Void.TYPE)) {
                    logWithPid("  Expected method to return null: " + method.getName());
                }
            }
        } else if (InterprocessEvent.class.isAssignableFrom(parameterTypes[0]) && method.getName().startsWith("onInterprocessBusEvent")) {
            isInterprocessEventOut.value = true;
            return true;
        } else if (Event.class.isAssignableFrom(parameterTypes[0]) && method.getName().startsWith("onBusEvent")) {
            isInterprocessEventOut.value = false;
            return true;
        } else if (DEBUG_TRACE_ALL) {
            if (!Event.class.isAssignableFrom(parameterTypes[0])) {
                logWithPid("  Expected method take an Event-based parameter: " + method.getName());
            } else if (!method.getName().startsWith("onInterprocessBusEvent") && !method.getName().startsWith("onBusEvent")) {
                logWithPid("  Expected method start with method prefix: " + method.getName());
            }
        }
        return false;
    }

    private void sortEventHandlersByPriority(List<EventHandler> eventHandlers) {
        Collections.sort(eventHandlers, EVENT_HANDLER_COMPARATOR);
    }

    private static void logWithPid(String text) {
        Log.d("EventBus", "[" + Process.myPid() + ", u" + UserHandle.myUserId() + "] " + text);
    }
}
