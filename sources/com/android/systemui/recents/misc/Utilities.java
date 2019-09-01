package com.android.systemui.recents.misc;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.RectEvaluator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandleCompat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IntProperty;
import android.util.Property;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.TaskViewTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utilities {
    public static final Property<Drawable, Integer> DRAWABLE_ALPHA = new IntProperty<Drawable>("drawableAlpha") {
        public void setValue(Drawable object, int alpha) {
            object.setAlpha(alpha);
        }

        public Integer get(Drawable object) {
            return Integer.valueOf(object.getAlpha());
        }
    };
    public static final Property<Drawable, Rect> DRAWABLE_RECT = new Property<Drawable, Rect>(Rect.class, "drawableBounds") {
        public void set(Drawable object, Rect bounds) {
            object.setBounds(bounds);
        }

        public Rect get(Drawable object) {
            return object.getBounds();
        }
    };
    public static final Rect EMPTY_RECT = new Rect();
    public static final RectFEvaluator RECTF_EVALUATOR = new RectFEvaluator();
    public static final RectEvaluator RECT_EVALUATOR = new RectEvaluator(new Rect());

    public static <T> ArraySet<T> arrayToSet(T[] array, ArraySet<T> setOut) {
        setOut.clear();
        if (array != null) {
            Collections.addAll(setOut, array);
        }
        return setOut;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public static float mapRange(float value, float min, float max) {
        return ((max - min) * value) + min;
    }

    public static void scaleRectAboutCenter(RectF r, float scale) {
        if (scale != 1.0f) {
            float cx = r.centerX();
            float cy = r.centerY();
            r.offset(-cx, -cy);
            r.left *= scale;
            r.top *= scale;
            r.right *= scale;
            r.bottom *= scale;
            r.offset(cx, cy);
        }
    }

    public static float computeContrastBetweenColors(int bg, int fg) {
        float fgB;
        float fgR;
        float bgR = ((float) Color.red(bg)) / 255.0f;
        float bgG = ((float) Color.green(bg)) / 255.0f;
        float bgB = ((float) Color.blue(bg)) / 255.0f;
        float bgL = (0.2126f * (bgR < 0.03928f ? bgR / 12.92f : (float) Math.pow((double) ((bgR + 0.055f) / 1.055f), 2.4000000953674316d))) + (0.7152f * (bgG < 0.03928f ? bgG / 12.92f : (float) Math.pow((double) ((bgG + 0.055f) / 1.055f), 2.4000000953674316d))) + (0.0722f * (bgB < 0.03928f ? bgB / 12.92f : (float) Math.pow((double) ((bgB + 0.055f) / 1.055f), 2.4000000953674316d)));
        float fgR2 = ((float) Color.red(fg)) / 255.0f;
        float fgG = ((float) Color.green(fg)) / 255.0f;
        float fgB2 = ((float) Color.blue(fg)) / 255.0f;
        if (fgR2 < 0.03928f) {
            fgR = fgR2 / 12.92f;
            fgB = fgB2;
        } else {
            fgB = fgB2;
            fgR = (float) Math.pow((double) ((fgR2 + 0.055f) / 1.055f), 2.4000000953674316d);
        }
        return Math.abs(((((0.2126f * fgR) + (0.7152f * (fgG < 0.03928f ? fgG / 12.92f : (float) Math.pow((double) ((fgG + 0.055f) / 1.055f), 2.4000000953674316d)))) + (0.0722f * (fgB < 0.03928f ? fgB / 12.92f : (float) Math.pow((double) ((fgB + 0.055f) / 1.055f), 2.4000000953674316d)))) + 0.05f) / (0.05f + bgL));
    }

    public static int getColorWithOverlay(int baseColor, int overlayColor, float overlayAlpha) {
        return Color.rgb((int) ((((float) Color.red(baseColor)) * overlayAlpha) + ((1.0f - overlayAlpha) * ((float) Color.red(overlayColor)))), (int) ((((float) Color.green(baseColor)) * overlayAlpha) + ((1.0f - overlayAlpha) * ((float) Color.green(overlayColor)))), (int) ((((float) Color.blue(baseColor)) * overlayAlpha) + ((1.0f - overlayAlpha) * ((float) Color.blue(overlayColor)))));
    }

    public static void cancelAnimationWithoutCallbacks(Animator animator) {
        if (animator != null && animator.isStarted()) {
            removeAnimationListenersRecursive(animator);
            animator.cancel();
        }
    }

    public static void removeAnimationListenersRecursive(Animator animator) {
        if (animator instanceof AnimatorSet) {
            ArrayList<Animator> animators = ((AnimatorSet) animator).getChildAnimations();
            for (int i = animators.size() - 1; i >= 0; i--) {
                removeAnimationListenersRecursive(animators.get(i));
            }
        }
        animator.removeAllListeners();
    }

    public static void setViewFrameFromTranslation(View v) {
        RectF taskViewRect = new RectF((float) v.getLeft(), (float) v.getTop(), (float) v.getRight(), (float) v.getBottom());
        taskViewRect.offset(v.getTranslationX(), v.getTranslationY());
        v.setTranslationX(0.0f);
        v.setTranslationY(0.0f);
        v.setLeftTopRightBottom((int) taskViewRect.left, (int) taskViewRect.top, (int) taskViewRect.right, (int) taskViewRect.bottom);
    }

    public static ViewStub findViewStubById(View v, int stubId) {
        return (ViewStub) v.findViewById(stubId);
    }

    public static ViewStub findViewStubById(Activity a, int stubId) {
        return (ViewStub) a.findViewById(stubId);
    }

    public static void matchTaskListSize(List<Task> tasks, List<TaskViewTransform> transforms) {
        int taskTransformCount = transforms.size();
        int taskCount = tasks.size();
        if (taskTransformCount < taskCount) {
            for (int i = taskTransformCount; i < taskCount; i++) {
                transforms.add(new TaskViewTransform());
            }
        } else if (taskTransformCount > taskCount) {
            transforms.subList(taskCount, taskTransformCount).clear();
        }
    }

    public static float dpToPx(Resources res, float dp) {
        return TypedValue.applyDimension(1, dp, res.getDisplayMetrics());
    }

    public static boolean isDescendentAccessibilityFocused(View v) {
        if (v.isAccessibilityFocused()) {
            return true;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (isDescendentAccessibilityFocused(vg.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Configuration getAppConfiguration(Context context) {
        return context.getApplicationContext().getResources().getConfiguration();
    }

    public static String dumpRect(Rect r) {
        if (r == null) {
            return "N:0,0-0,0";
        }
        return r.left + "," + r.top + "-" + r.right + "," + r.bottom;
    }

    public static boolean isAndroidNorNewer() {
        return Build.VERSION.SDK_INT >= 24;
    }

    public static boolean isAndroidPorNewer() {
        return Build.VERSION.SDK_INT >= 28;
    }

    public static boolean supportsMultiWindow() {
        return Process.myUserHandle().equals(UserHandleCompat.SYSTEM);
    }

    public static boolean isSlideCoverDevice() {
        return "perseus".equals(miui.os.Build.DEVICE);
    }

    public static boolean isPackageEnabled(Context context, String packageName) {
        if (context == null && TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isShowRecentsRecommend() {
        return miui.os.Build.IS_INTERNATIONAL_BUILD;
    }
}
