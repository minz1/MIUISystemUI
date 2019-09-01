package com.android.systemui.miui.statusbar.notification;

import android.util.Log;
import android.view.View;
import java.lang.reflect.Field;

public class HookViewHelper {

    private static class OnClickListenerProxy implements View.OnClickListener {
        private View.OnClickListener mListener;
        private Runnable mRunnable;

        public OnClickListenerProxy(View.OnClickListener listener, Runnable runnable) {
            this.mListener = listener;
            this.mRunnable = runnable;
        }

        public void onClick(View v) {
            if (this.mListener != null) {
                hook();
                this.mListener.onClick(v);
            }
        }

        private void hook() {
            if (this.mRunnable != null) {
                this.mRunnable.run();
            }
        }
    }

    public static void hookView(View view, Runnable runnable) {
        View.OnClickListener oldOnClickListener = getClickListener(view);
        if (oldOnClickListener != null && !(oldOnClickListener instanceof OnClickListenerProxy)) {
            view.setOnClickListener(new OnClickListenerProxy(oldOnClickListener, runnable));
        }
    }

    private static View.OnClickListener getClickListener(View view) {
        try {
            Field field = Class.forName("android.view.View").getDeclaredField("mListenerInfo");
            Object listenerInfo = null;
            if (field != null) {
                field.setAccessible(true);
                listenerInfo = field.get(view);
            }
            Field declaredField = Class.forName("android.view.View$ListenerInfo").getDeclaredField("mOnClickListener");
            if (declaredField == null || listenerInfo == null) {
                return null;
            }
            return (View.OnClickListener) declaredField.get(listenerInfo);
        } catch (ClassNotFoundException e) {
            Log.e("HookViewManager", "ClassNotFoundException", e);
            return null;
        } catch (NoSuchFieldException e2) {
            Log.e("HookViewManager", "NoSuchFieldException", e2);
            return null;
        } catch (IllegalAccessException e3) {
            Log.e("HookViewManager", "IllegalAccessException", e3);
            return null;
        }
    }
}
