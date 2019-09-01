package android.support.v4.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.compat.R;
import android.support.v4.os.BuildCompat;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.view.WindowManager;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewCompat {
    private static boolean sAccessibilityDelegateCheckFailed = false;
    private static Field sAccessibilityDelegateField;
    private static Field sMinHeightField;
    private static boolean sMinHeightFieldFetched;
    private static Field sMinWidthField;
    private static boolean sMinWidthFieldFetched;
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    private static ThreadLocal<Rect> sThreadLocalRect;
    private static WeakHashMap<View, String> sTransitionNameMap;
    private static WeakHashMap<View, ViewPropertyAnimatorCompat> sViewPropertyAnimatorMap = null;

    public interface OnUnhandledKeyEventListenerCompat {
        boolean onUnhandledKeyEvent(View view, KeyEvent keyEvent);
    }

    static class UnhandledKeyEventManager {
        private static final ArrayList<WeakReference<View>> sViewsWithListeners = new ArrayList<>();
        private SparseBooleanArray mCapturedKeys = null;
        private WeakReference<View> mCurrentReceiver = null;
        private WeakHashMap<View, Boolean> mViewsContainingListeners = null;

        UnhandledKeyEventManager() {
        }

        private SparseBooleanArray getCapturedKeys() {
            if (this.mCapturedKeys == null) {
                this.mCapturedKeys = new SparseBooleanArray();
            }
            return this.mCapturedKeys;
        }

        static UnhandledKeyEventManager at(View root) {
            UnhandledKeyEventManager manager = (UnhandledKeyEventManager) root.getTag(R.id.tag_unhandled_key_event_manager);
            if (manager != null) {
                return manager;
            }
            UnhandledKeyEventManager manager2 = new UnhandledKeyEventManager();
            root.setTag(R.id.tag_unhandled_key_event_manager, manager2);
            return manager2;
        }

        private void updateCaptureState(KeyEvent event) {
            if (event.getAction() == 0) {
                getCapturedKeys().append(event.getKeyCode(), true);
            } else if (event.getAction() == 1) {
                getCapturedKeys().delete(event.getKeyCode());
            }
        }

        /* access modifiers changed from: package-private */
        public boolean dispatch(View root, KeyEvent event) {
            updateCaptureState(event);
            boolean z = true;
            if (this.mCurrentReceiver != null) {
                View target = (View) this.mCurrentReceiver.get();
                if (getCapturedKeys().size() == 0) {
                    this.mCurrentReceiver = null;
                }
                if (target == null || !ViewCompat.isAttachedToWindow(target)) {
                    return true;
                }
                return onUnhandledKeyEvent(target, event);
            }
            if (event.getAction() == 0 && getCapturedKeys().size() == 1) {
                recalcViewsWithUnhandled();
            }
            View consumer = dispatchInOrder(root, event);
            if (consumer != null) {
                this.mCurrentReceiver = new WeakReference<>(consumer);
            }
            if (consumer == null) {
                z = false;
            }
            return z;
        }

        private View dispatchInOrder(View view, KeyEvent event) {
            if (this.mViewsContainingListeners == null || !this.mViewsContainingListeners.containsKey(view)) {
                return null;
            }
            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    View consumer = dispatchInOrder(vg.getChildAt(i), event);
                    if (consumer != null) {
                        return consumer;
                    }
                }
            }
            if (onUnhandledKeyEvent(view, event)) {
                return view;
            }
            return null;
        }

        /* access modifiers changed from: package-private */
        public boolean hasFocus() {
            return this.mCurrentReceiver != null;
        }

        /* access modifiers changed from: package-private */
        public boolean onUnhandledKeyEvent(View v, KeyEvent event) {
            ArrayList<OnUnhandledKeyEventListenerCompat> viewListeners = (ArrayList) v.getTag(R.id.tag_unhandled_key_listeners);
            if (viewListeners != null) {
                for (int i = viewListeners.size() - 1; i >= 0; i--) {
                    if (viewListeners.get(i).onUnhandledKeyEvent(v, event)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void recalcViewsWithUnhandled() {
            if (this.mViewsContainingListeners != null) {
                this.mViewsContainingListeners.clear();
            }
            if (!sViewsWithListeners.isEmpty()) {
                synchronized (sViewsWithListeners) {
                    if (this.mViewsContainingListeners == null) {
                        this.mViewsContainingListeners = new WeakHashMap<>();
                    }
                    for (int i = sViewsWithListeners.size() - 1; i >= 0; i--) {
                        View v = (View) sViewsWithListeners.get(i).get();
                        if (v == null) {
                            sViewsWithListeners.remove(i);
                        } else {
                            this.mViewsContainingListeners.put(v, Boolean.TRUE);
                            for (ViewParent nxt = v.getParent(); nxt instanceof View; nxt = nxt.getParent()) {
                                this.mViewsContainingListeners.put((View) nxt, Boolean.TRUE);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Rect getEmptyTempRect() {
        if (sThreadLocalRect == null) {
            sThreadLocalRect = new ThreadLocal<>();
        }
        Rect rect = sThreadLocalRect.get();
        if (rect == null) {
            rect = new Rect();
            sThreadLocalRect.set(rect);
        }
        rect.setEmpty();
        return rect;
    }

    public static void setAccessibilityDelegate(View v, AccessibilityDelegateCompat delegate) {
        v.setAccessibilityDelegate(delegate == null ? null : delegate.getBridge());
    }

    @SuppressLint({"InlinedApi"})
    public static int getImportantForAutofill(View v) {
        if (Build.VERSION.SDK_INT >= 26) {
            return v.getImportantForAutofill();
        }
        return 0;
    }

    public static void setImportantForAutofill(View v, int mode) {
        if (Build.VERSION.SDK_INT >= 26) {
            v.setImportantForAutofill(mode);
        }
    }

    public static boolean hasAccessibilityDelegate(View v) {
        boolean z = false;
        if (sAccessibilityDelegateCheckFailed) {
            return false;
        }
        if (sAccessibilityDelegateField == null) {
            try {
                sAccessibilityDelegateField = View.class.getDeclaredField("mAccessibilityDelegate");
                sAccessibilityDelegateField.setAccessible(true);
            } catch (Throwable th) {
                sAccessibilityDelegateCheckFailed = true;
                return false;
            }
        }
        try {
            if (sAccessibilityDelegateField.get(v) != null) {
                z = true;
            }
            return z;
        } catch (Throwable th2) {
            sAccessibilityDelegateCheckFailed = true;
            return false;
        }
    }

    public static boolean hasTransientState(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            return view.hasTransientState();
        }
        return false;
    }

    public static void postInvalidateOnAnimation(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.postInvalidateOnAnimation();
        } else {
            view.postInvalidate();
        }
    }

    public static void postInvalidateOnAnimation(View view, int left, int top, int right, int bottom) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.postInvalidateOnAnimation(left, top, right, bottom);
        } else {
            view.postInvalidate(left, top, right, bottom);
        }
    }

    public static void postOnAnimation(View view, Runnable action) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.postOnAnimation(action);
        } else {
            view.postDelayed(action, ValueAnimator.getFrameDelay());
        }
    }

    public static void postOnAnimationDelayed(View view, Runnable action, long delayMillis) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.postOnAnimationDelayed(action, delayMillis);
        } else {
            view.postDelayed(action, ValueAnimator.getFrameDelay() + delayMillis);
        }
    }

    public static int getImportantForAccessibility(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            return view.getImportantForAccessibility();
        }
        return 0;
    }

    public static void setImportantForAccessibility(View view, int mode) {
        if (Build.VERSION.SDK_INT >= 19) {
            view.setImportantForAccessibility(mode);
        } else if (Build.VERSION.SDK_INT >= 16) {
            if (mode == 4) {
                mode = 2;
            }
            view.setImportantForAccessibility(mode);
        }
    }

    public static void setLayerPaint(View view, Paint paint) {
        if (Build.VERSION.SDK_INT >= 17) {
            view.setLayerPaint(paint);
            return;
        }
        view.setLayerType(view.getLayerType(), paint);
        view.invalidate();
    }

    public static int getLayoutDirection(View view) {
        if (Build.VERSION.SDK_INT >= 17) {
            return view.getLayoutDirection();
        }
        return 0;
    }

    public static ViewParent getParentForAccessibility(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            return view.getParentForAccessibility();
        }
        return view.getParent();
    }

    @Deprecated
    public static float getTranslationY(View view) {
        return view.getTranslationY();
    }

    public static int getMinimumWidth(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            return view.getMinimumWidth();
        }
        if (!sMinWidthFieldFetched) {
            try {
                sMinWidthField = View.class.getDeclaredField("mMinWidth");
                sMinWidthField.setAccessible(true);
            } catch (NoSuchFieldException e) {
            }
            sMinWidthFieldFetched = true;
        }
        if (sMinWidthField != null) {
            try {
                return ((Integer) sMinWidthField.get(view)).intValue();
            } catch (Exception e2) {
            }
        }
        return 0;
    }

    public static int getMinimumHeight(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            return view.getMinimumHeight();
        }
        if (!sMinHeightFieldFetched) {
            try {
                sMinHeightField = View.class.getDeclaredField("mMinHeight");
                sMinHeightField.setAccessible(true);
            } catch (NoSuchFieldException e) {
            }
            sMinHeightFieldFetched = true;
        }
        if (sMinHeightField != null) {
            try {
                return ((Integer) sMinHeightField.get(view)).intValue();
            } catch (Exception e2) {
            }
        }
        return 0;
    }

    public static ViewPropertyAnimatorCompat animate(View view) {
        if (sViewPropertyAnimatorMap == null) {
            sViewPropertyAnimatorMap = new WeakHashMap<>();
        }
        ViewPropertyAnimatorCompat vpa = sViewPropertyAnimatorMap.get(view);
        if (vpa != null) {
            return vpa;
        }
        ViewPropertyAnimatorCompat vpa2 = new ViewPropertyAnimatorCompat(view);
        sViewPropertyAnimatorMap.put(view, vpa2);
        return vpa2;
    }

    public static void setElevation(View view, float elevation) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setElevation(elevation);
        }
    }

    public static float getElevation(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getElevation();
        }
        return 0.0f;
    }

    public static void setTransitionName(View view, String transitionName) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setTransitionName(transitionName);
            return;
        }
        if (sTransitionNameMap == null) {
            sTransitionNameMap = new WeakHashMap<>();
        }
        sTransitionNameMap.put(view, transitionName);
    }

    public static String getTransitionName(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getTransitionName();
        }
        if (sTransitionNameMap == null) {
            return null;
        }
        return sTransitionNameMap.get(view);
    }

    public static int getWindowSystemUiVisibility(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            return view.getWindowSystemUiVisibility();
        }
        return 0;
    }

    public static void requestApplyInsets(View view) {
        if (Build.VERSION.SDK_INT >= 20) {
            view.requestApplyInsets();
        } else if (Build.VERSION.SDK_INT >= 16) {
            view.requestFitSystemWindows();
        }
    }

    public static boolean getFitsSystemWindows(View v) {
        if (Build.VERSION.SDK_INT >= 16) {
            return v.getFitsSystemWindows();
        }
        return false;
    }

    public static void setOnApplyWindowInsetsListener(View v, final OnApplyWindowInsetsListener listener) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (listener == null) {
                v.setOnApplyWindowInsetsListener(null);
                return;
            }
            v.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    return (WindowInsets) WindowInsetsCompat.unwrap(listener.onApplyWindowInsets(view, WindowInsetsCompat.wrap(insets)));
                }
            });
        }
    }

    public static WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
        if (Build.VERSION.SDK_INT < 21) {
            return insets;
        }
        WindowInsets unwrapped = (WindowInsets) WindowInsetsCompat.unwrap(insets);
        WindowInsets result = view.onApplyWindowInsets(unwrapped);
        if (result != unwrapped) {
            unwrapped = new WindowInsets(result);
        }
        return WindowInsetsCompat.wrap(unwrapped);
    }

    public static WindowInsetsCompat dispatchApplyWindowInsets(View view, WindowInsetsCompat insets) {
        if (Build.VERSION.SDK_INT < 21) {
            return insets;
        }
        WindowInsets unwrapped = (WindowInsets) WindowInsetsCompat.unwrap(insets);
        WindowInsets result = view.dispatchApplyWindowInsets(unwrapped);
        if (result != unwrapped) {
            unwrapped = new WindowInsets(result);
        }
        return WindowInsetsCompat.wrap(unwrapped);
    }

    public static boolean hasOverlappingRendering(View view) {
        if (Build.VERSION.SDK_INT >= 16) {
            return view.hasOverlappingRendering();
        }
        return true;
    }

    public static void setBackground(View view, Drawable background) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.setBackground(background);
        } else {
            view.setBackgroundDrawable(background);
        }
    }

    public static ColorStateList getBackgroundTintList(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getBackgroundTintList();
        }
        return view instanceof TintableBackgroundView ? ((TintableBackgroundView) view).getSupportBackgroundTintList() : null;
    }

    public static void setBackgroundTintList(View view, ColorStateList tintList) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setBackgroundTintList(tintList);
            if (Build.VERSION.SDK_INT == 21) {
                Drawable background = view.getBackground();
                boolean hasTint = (view.getBackgroundTintList() == null && view.getBackgroundTintMode() == null) ? false : true;
                if (background != null && hasTint) {
                    if (background.isStateful()) {
                        background.setState(view.getDrawableState());
                    }
                    view.setBackground(background);
                }
            }
        } else if (view instanceof TintableBackgroundView) {
            ((TintableBackgroundView) view).setSupportBackgroundTintList(tintList);
        }
    }

    public static PorterDuff.Mode getBackgroundTintMode(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getBackgroundTintMode();
        }
        return view instanceof TintableBackgroundView ? ((TintableBackgroundView) view).getSupportBackgroundTintMode() : null;
    }

    public static void setBackgroundTintMode(View view, PorterDuff.Mode mode) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.setBackgroundTintMode(mode);
            if (Build.VERSION.SDK_INT == 21) {
                Drawable background = view.getBackground();
                boolean hasTint = (view.getBackgroundTintList() == null && view.getBackgroundTintMode() == null) ? false : true;
                if (background != null && hasTint) {
                    if (background.isStateful()) {
                        background.setState(view.getDrawableState());
                    }
                    view.setBackground(background);
                }
            }
        } else if (view instanceof TintableBackgroundView) {
            ((TintableBackgroundView) view).setSupportBackgroundTintMode(mode);
        }
    }

    public static boolean isNestedScrollingEnabled(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.isNestedScrollingEnabled();
        }
        if (view instanceof NestedScrollingChild) {
            return ((NestedScrollingChild) view).isNestedScrollingEnabled();
        }
        return false;
    }

    public static void stopNestedScroll(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            view.stopNestedScroll();
        } else if (view instanceof NestedScrollingChild) {
            ((NestedScrollingChild) view).stopNestedScroll();
        }
    }

    public static boolean isLaidOut(View view) {
        if (Build.VERSION.SDK_INT >= 19) {
            return view.isLaidOut();
        }
        return view.getWidth() > 0 && view.getHeight() > 0;
    }

    public static float getZ(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            return view.getZ();
        }
        return 0.0f;
    }

    public static void offsetTopAndBottom(View view, int offset) {
        if (Build.VERSION.SDK_INT >= 23) {
            view.offsetTopAndBottom(offset);
        } else if (Build.VERSION.SDK_INT >= 21) {
            Rect parentRect = getEmptyTempRect();
            boolean needInvalidateWorkaround = false;
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                View p = (View) parent;
                parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
                needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            }
            compatOffsetTopAndBottom(view, offset);
            if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(parentRect);
            }
        } else {
            compatOffsetTopAndBottom(view, offset);
        }
    }

    private static void compatOffsetTopAndBottom(View view, int offset) {
        view.offsetTopAndBottom(offset);
        if (view.getVisibility() == 0) {
            tickleInvalidationFlag(view);
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                tickleInvalidationFlag((View) parent);
            }
        }
    }

    public static void offsetLeftAndRight(View view, int offset) {
        if (Build.VERSION.SDK_INT >= 23) {
            view.offsetLeftAndRight(offset);
        } else if (Build.VERSION.SDK_INT >= 21) {
            Rect parentRect = getEmptyTempRect();
            boolean needInvalidateWorkaround = false;
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                View p = (View) parent;
                parentRect.set(p.getLeft(), p.getTop(), p.getRight(), p.getBottom());
                needInvalidateWorkaround = !parentRect.intersects(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            }
            compatOffsetLeftAndRight(view, offset);
            if (needInvalidateWorkaround && parentRect.intersect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom())) {
                ((View) parent).invalidate(parentRect);
            }
        } else {
            compatOffsetLeftAndRight(view, offset);
        }
    }

    private static void compatOffsetLeftAndRight(View view, int offset) {
        view.offsetLeftAndRight(offset);
        if (view.getVisibility() == 0) {
            tickleInvalidationFlag(view);
            ViewParent parent = view.getParent();
            if (parent instanceof View) {
                tickleInvalidationFlag((View) parent);
            }
        }
    }

    private static void tickleInvalidationFlag(View view) {
        float y = view.getTranslationY();
        view.setTranslationY(1.0f + y);
        view.setTranslationY(y);
    }

    public static boolean isAttachedToWindow(View view) {
        if (Build.VERSION.SDK_INT >= 19) {
            return view.isAttachedToWindow();
        }
        return view.getWindowToken() != null;
    }

    public static boolean hasOnClickListeners(View view) {
        if (Build.VERSION.SDK_INT >= 15) {
            return view.hasOnClickListeners();
        }
        return false;
    }

    public static void setScrollIndicators(View view, int indicators, int mask) {
        if (Build.VERSION.SDK_INT >= 23) {
            view.setScrollIndicators(indicators, mask);
        }
    }

    public static Display getDisplay(View view) {
        if (Build.VERSION.SDK_INT >= 17) {
            return view.getDisplay();
        }
        if (isAttachedToWindow(view)) {
            return ((WindowManager) view.getContext().getSystemService("window")).getDefaultDisplay();
        }
        return null;
    }

    public static boolean dispatchUnhandledKeyEventPre(View root, KeyEvent evt) {
        boolean z = false;
        if (BuildCompat.isAtLeastP()) {
            return false;
        }
        if (UnhandledKeyEventManager.at(root).hasFocus() && UnhandledKeyEventManager.at(root).dispatch(root, evt)) {
            z = true;
        }
        return z;
    }

    public static boolean dispatchUnhandledKeyEventPost(View root, KeyEvent evt) {
        if (BuildCompat.isAtLeastP()) {
            return false;
        }
        return UnhandledKeyEventManager.at(root).dispatch(root, evt);
    }
}
