package miui.external;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.IBinder;
import java.lang.reflect.Field;
import miui.external.SdkConstants;

final class SdkErrorInstrumentation extends Instrumentation implements SdkConstants {
    private SdkConstants.SdkError mError;

    private SdkErrorInstrumentation(SdkConstants.SdkError error) {
        this.mError = error;
    }

    static void handleSdkError(SdkConstants.SdkError error) {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object at = activityThreadClass.getMethod("currentActivityThread", new Class[0]).invoke(null, new Object[0]);
            Field instField = getDeclaredField(activityThreadClass, at, (Instrumentation) activityThreadClass.getMethod("getInstrumentation", new Class[0]).invoke(at, new Object[0]), null, null);
            Instrumentation oldInst = (Instrumentation) instField.get(at);
            Instrumentation newInst = new SdkErrorInstrumentation(error);
            for (Class<Instrumentation> cls = Instrumentation.class; cls != null; cls = cls.getSuperclass()) {
                for (Field field : cls.getDeclaredFields()) {
                    field.setAccessible(true);
                    field.set(newInst, field.get(oldInst));
                }
            }
            instField.set(at, newInst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Field getDeclaredField(Class<?> clazz, Object holder, Object value, String name, Class<?> type) throws NoSuchFieldException {
        Field[] fields = clazz.getDeclaredFields();
        if (!(holder == null || value == null)) {
            int length = fields.length;
            int i = 0;
            while (i < length) {
                Field field = fields[i];
                field.setAccessible(true);
                try {
                    if (field.get(holder) == value) {
                        return field;
                    }
                    i++;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e2) {
                    e2.printStackTrace();
                }
            }
        }
        if (name != null) {
            for (Field field2 : fields) {
                if (field2.getName().equals(name)) {
                    field2.setAccessible(true);
                    return field2;
                }
            }
        }
        Field candidate = null;
        if (type == null) {
            for (Field field3 : fields) {
                if (field3.getType() == type || field3.getType().isInstance(type)) {
                    if (candidate == null) {
                        candidate = field3;
                    } else {
                        throw new NoSuchFieldException("More than one matched field found: " + candidate.getName() + " and " + field3.getName());
                    }
                }
            }
            if (candidate != null) {
                candidate.setAccessible(true);
            } else {
                throw new NoSuchFieldException("No such field found of value " + value);
            }
        }
        return candidate;
    }

    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id, Object lastNonConfigurationInstance) throws InstantiationException, IllegalAccessException {
        SdkErrorInstrumentation sdkErrorInstrumentation;
        Intent intent2;
        Class cls;
        if (!clazz.getSimpleName().startsWith("SdkError")) {
            cls = SdkErrorActivity.class;
            if (intent == null) {
                intent2 = new Intent();
            } else {
                intent2 = intent;
            }
            sdkErrorInstrumentation = this;
            intent2.putExtra("com.miui.sdk.error", sdkErrorInstrumentation.mError);
        } else {
            sdkErrorInstrumentation = this;
            cls = clazz;
            intent2 = intent;
        }
        return super.newActivity(cls, context, token, application, intent2, info, title, parent, id, lastNonConfigurationInstance);
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (!className.startsWith("SdkError")) {
            className = SdkErrorActivity.class.getName();
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra("com.miui.sdk.error", this.mError);
        }
        return super.newActivity(cl, className, intent);
    }
}
