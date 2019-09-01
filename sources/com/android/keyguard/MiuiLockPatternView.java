package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.Log;
import android.view.DisplayListCanvas;
import android.view.MotionEvent;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.internal.widget.ExploreByTouchHelper;
import com.android.internal.widget.LockPatternView;
import com.android.systemui.R;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MiuiLockPatternView extends View {
    private long mAnimatingPeriodStart;
    private int mAspect;
    private AudioManager mAudioManager;
    /* access modifiers changed from: private */
    public final CellState[][] mCellStates;
    private final Path mCurrentPath;
    /* access modifiers changed from: private */
    public final int mDotSize;
    /* access modifiers changed from: private */
    public final int mDotSizeActivated;
    private boolean mDrawingProfilingStarted;
    private boolean mEnableHapticFeedback;
    private int mErrorColor;
    private PatternExploreByTouchHelper mExploreByTouchHelper;
    /* access modifiers changed from: private */
    public final Interpolator mFastOutSlowInInterpolator;
    /* access modifiers changed from: private */
    public float mHitFactor;
    private float mInProgressX;
    private float mInProgressY;
    private boolean mInStealthMode;
    private boolean mInputEnabled;
    private final Rect mInvalidate;
    private final Interpolator mLinearOutSlowInInterpolator;
    private Drawable mNotSelectedDrawable;
    private OnPatternListener mOnPatternListener;
    private final Paint mPaint;
    private final Paint mPathPaint;
    private final int mPathWidth;
    private final ArrayList<LockPatternView.Cell> mPattern;
    private DisplayMode mPatternDisplayMode;
    /* access modifiers changed from: private */
    public final boolean[][] mPatternDrawLookup;
    /* access modifiers changed from: private */
    public boolean mPatternInProgress;
    private int mRegularColor;
    private Drawable mSelectedDrawable;
    /* access modifiers changed from: private */
    public float mSquareHeight;
    /* access modifiers changed from: private */
    public float mSquareWidth;
    private int mSuccessColor;
    private final Rect mTmpInvalidateRect;
    private boolean mUseLockPatternDrawable;

    public static class CellState {
        float alpha = 1.0f;
        int col;
        boolean hwAnimating;
        CanvasProperty<Float> hwCenterX;
        CanvasProperty<Float> hwCenterY;
        CanvasProperty<Paint> hwPaint;
        CanvasProperty<Float> hwRadius;
        public ValueAnimator lineAnimator;
        public float lineEndX = Float.MIN_VALUE;
        public float lineEndY = Float.MIN_VALUE;
        float radius;
        int row;
        float translationY;
    }

    public enum DisplayMode {
        Correct,
        Animate,
        Wrong
    }

    public interface OnPatternListener {
        void onPatternCellAdded(List<LockPatternView.Cell> list);

        void onPatternCleared();

        void onPatternDetected(List<LockPatternView.Cell> list);

        void onPatternStart();
    }

    private final class PatternExploreByTouchHelper extends ExploreByTouchHelper {
        private HashMap<Integer, VirtualViewContainer> mItems = new HashMap<>();
        private Rect mTempRect = new Rect();

        class VirtualViewContainer {
            CharSequence description;

            public VirtualViewContainer(CharSequence description2) {
                this.description = description2;
            }
        }

        public PatternExploreByTouchHelper(View forView) {
            super(forView);
        }

        /* access modifiers changed from: protected */
        public int getVirtualViewAt(float x, float y) {
            return getVirtualViewIdForHit(x, y);
        }

        /* access modifiers changed from: protected */
        public void getVisibleVirtualViews(IntArray virtualViewIds) {
            if (MiuiLockPatternView.this.mPatternInProgress) {
                for (int i = 1; i < 10; i++) {
                    if (!this.mItems.containsKey(Integer.valueOf(i))) {
                        this.mItems.put(Integer.valueOf(i), new VirtualViewContainer(getTextForVirtualView(i)));
                    }
                    virtualViewIds.add(i);
                }
            }
        }

        /* access modifiers changed from: protected */
        public void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            if (this.mItems.containsKey(Integer.valueOf(virtualViewId))) {
                CharSequence contentDescription = this.mItems.get(Integer.valueOf(virtualViewId)).description;
                event.getText().add(contentDescription);
                event.setContentDescription(contentDescription);
                return;
            }
            event.setContentDescription(MiuiLockPatternView.this.mContext.getResources().getString(R.string.input_pattern_hint_text));
        }

        public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
            MiuiLockPatternView.super.onPopulateAccessibilityEvent(host, event);
            if (!MiuiLockPatternView.this.mPatternInProgress) {
                event.setContentDescription(MiuiLockPatternView.this.getContext().getText(17040200));
            }
        }

        /* access modifiers changed from: protected */
        public void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfo node) {
            node.setText(getTextForVirtualView(virtualViewId));
            node.setContentDescription(getTextForVirtualView(virtualViewId));
            if (MiuiLockPatternView.this.mPatternInProgress) {
                node.setFocusable(true);
                if (isClickable(virtualViewId)) {
                    node.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
                    node.setClickable(isClickable(virtualViewId));
                }
            }
            node.setBoundsInParent(getBoundsForVirtualView(virtualViewId));
        }

        private boolean isClickable(int virtualViewId) {
            if (virtualViewId == Integer.MIN_VALUE) {
                return false;
            }
            return !MiuiLockPatternView.this.mPatternDrawLookup[(virtualViewId - 1) / 3][(virtualViewId - 1) % 3];
        }

        /* access modifiers changed from: protected */
        public boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            if (action != 16) {
                return false;
            }
            return onItemClicked(virtualViewId);
        }

        /* access modifiers changed from: package-private */
        public boolean onItemClicked(int index) {
            invalidateVirtualView(index);
            sendEventForVirtualView(index, 1);
            return true;
        }

        private Rect getBoundsForVirtualView(int virtualViewId) {
            int ordinal = virtualViewId - 1;
            Rect bounds = this.mTempRect;
            int row = ordinal / 3;
            int col = ordinal % 3;
            CellState cellState = MiuiLockPatternView.this.mCellStates[row][col];
            float centerX = MiuiLockPatternView.this.getCenterXForColumn(col);
            float centerY = MiuiLockPatternView.this.getCenterYForRow(row);
            float cellheight = MiuiLockPatternView.this.mSquareHeight * MiuiLockPatternView.this.mHitFactor * 0.5f;
            float cellwidth = MiuiLockPatternView.this.mSquareWidth * MiuiLockPatternView.this.mHitFactor * 0.5f;
            bounds.left = (int) (centerX - cellwidth);
            bounds.right = (int) (centerX + cellwidth);
            bounds.top = (int) (centerY - cellheight);
            bounds.bottom = (int) (centerY + cellheight);
            return bounds;
        }

        private CharSequence getTextForVirtualView(int virtualViewId) {
            return MiuiLockPatternView.this.getResources().getString(R.string.lockscreen_access_pattern_cell_added_verbose, new Object[]{Integer.valueOf(virtualViewId)});
        }

        private int getVirtualViewIdForHit(float x, float y) {
            int rowHit = MiuiLockPatternView.this.getRowHit(y);
            int view = Integer.MIN_VALUE;
            if (rowHit < 0) {
                return Integer.MIN_VALUE;
            }
            int columnHit = MiuiLockPatternView.this.getColumnHit(x);
            if (columnHit < 0) {
                return Integer.MIN_VALUE;
            }
            int dotId = (rowHit * 3) + columnHit + 1;
            if (MiuiLockPatternView.this.mPatternDrawLookup[rowHit][columnHit]) {
                view = dotId;
            }
            return view;
        }
    }

    private static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        private final int mDisplayMode;
        private final boolean mInStealthMode;
        private final boolean mInputEnabled;
        private final String mSerializedPattern;
        private final boolean mTactileFeedbackEnabled;

        private SavedState(Parcelable superState, String serializedPattern, int displayMode, boolean inputEnabled, boolean inStealthMode, boolean tactileFeedbackEnabled) {
            super(superState);
            this.mSerializedPattern = serializedPattern;
            this.mDisplayMode = displayMode;
            this.mInputEnabled = inputEnabled;
            this.mInStealthMode = inStealthMode;
            this.mTactileFeedbackEnabled = tactileFeedbackEnabled;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mSerializedPattern = in.readString();
            this.mDisplayMode = in.readInt();
            this.mInputEnabled = ((Boolean) in.readValue(null)).booleanValue();
            this.mInStealthMode = ((Boolean) in.readValue(null)).booleanValue();
            this.mTactileFeedbackEnabled = ((Boolean) in.readValue(null)).booleanValue();
        }

        public int getDisplayMode() {
            return this.mDisplayMode;
        }

        public boolean isInputEnabled() {
            return this.mInputEnabled;
        }

        public boolean isInStealthMode() {
            return this.mInStealthMode;
        }

        public boolean isTactileFeedbackEnabled() {
            return this.mTactileFeedbackEnabled;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(this.mSerializedPattern);
            dest.writeInt(this.mDisplayMode);
            dest.writeValue(Boolean.valueOf(this.mInputEnabled));
            dest.writeValue(Boolean.valueOf(this.mInStealthMode));
            dest.writeValue(Boolean.valueOf(this.mTactileFeedbackEnabled));
        }
    }

    public MiuiLockPatternView(Context context) {
        this(context, null);
    }

    /* JADX WARNING: type inference failed for: r0v6, types: [com.android.keyguard.MiuiLockPatternView$PatternExploreByTouchHelper, android.view.View$AccessibilityDelegate] */
    public MiuiLockPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawingProfilingStarted = false;
        this.mPaint = new Paint();
        this.mPathPaint = new Paint();
        this.mPattern = new ArrayList<>(9);
        this.mPatternDrawLookup = (boolean[][]) Array.newInstance(boolean.class, new int[]{3, 3});
        this.mInProgressX = -1.0f;
        this.mInProgressY = -1.0f;
        this.mPatternDisplayMode = DisplayMode.Correct;
        this.mInputEnabled = true;
        this.mInStealthMode = false;
        this.mEnableHapticFeedback = true;
        this.mPatternInProgress = false;
        this.mHitFactor = 0.4f;
        this.mCurrentPath = new Path();
        this.mInvalidate = new Rect();
        this.mTmpInvalidateRect = new Rect();
        this.mAspect = 0;
        setClickable(true);
        this.mPathPaint.setAntiAlias(true);
        this.mPathPaint.setDither(true);
        this.mRegularColor = getResources().getColor(R.color.miui_pattern_lockscreen_paint_color);
        this.mErrorColor = getResources().getColor(R.color.pattern_lockscreen_paint_error_color);
        this.mSuccessColor = getResources().getColor(R.color.miui_pattern_lockscreen_heavy_paint_color);
        this.mPathPaint.setColor(this.mRegularColor);
        this.mPathPaint.setAntiAlias(true);
        this.mPathPaint.setDither(true);
        this.mPathPaint.setStyle(Paint.Style.STROKE);
        this.mPathPaint.setStrokeJoin(Paint.Join.ROUND);
        this.mPathPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mPathWidth = 7;
        this.mPathPaint.setStrokeWidth((float) this.mPathWidth);
        this.mDotSize = 18;
        this.mDotSizeActivated = 27;
        this.mCellStates = (CellState[][]) Array.newInstance(CellState.class, new int[]{3, 3});
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.mCellStates[i][j] = new CellState();
                this.mCellStates[i][j].radius = (float) (this.mDotSize / 2);
                this.mCellStates[i][j].row = i;
                this.mCellStates[i][j].col = j;
            }
        }
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563662);
        this.mExploreByTouchHelper = new PatternExploreByTouchHelper(this);
        setAccessibilityDelegate(this.mExploreByTouchHelper);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
    }

    public CellState[][] getCellStates() {
        return this.mCellStates;
    }

    public void setInStealthMode(boolean inStealthMode) {
        this.mInStealthMode = inStealthMode;
    }

    public void setTactileFeedbackEnabled(boolean tactileFeedbackEnabled) {
        this.mEnableHapticFeedback = tactileFeedbackEnabled;
    }

    public void setOnPatternListener(OnPatternListener onPatternListener) {
        this.mOnPatternListener = onPatternListener;
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.mPatternDisplayMode = displayMode;
        if (displayMode == DisplayMode.Animate) {
            if (this.mPattern.size() != 0) {
                this.mAnimatingPeriodStart = SystemClock.elapsedRealtime();
                LockPatternView.Cell first = this.mPattern.get(0);
                this.mInProgressX = getCenterXForColumn(first.getColumn());
                this.mInProgressY = getCenterYForRow(first.getRow());
                clearPatternDrawLookup();
            } else {
                throw new IllegalStateException("you must have a pattern to animate if you want to set the display mode to animate");
            }
        }
        invalidate();
    }

    public void startCellStateAnimation(CellState cellState, float startAlpha, float endAlpha, float startTranslationY, float endTranslationY, float startScale, float endScale, long delay, long duration, Interpolator interpolator, Runnable finishRunnable) {
        if (isHardwareAccelerated()) {
            startCellStateAnimationHw(cellState, startAlpha, endAlpha, startTranslationY, endTranslationY, startScale, endScale, delay, duration, interpolator, finishRunnable);
        } else {
            startCellStateAnimationSw(cellState, startAlpha, endAlpha, startTranslationY, endTranslationY, startScale, endScale, delay, duration, interpolator, finishRunnable);
        }
    }

    private void startCellStateAnimationSw(CellState cellState, float startAlpha, float endAlpha, float startTranslationY, float endTranslationY, float startScale, float endScale, long delay, long duration, Interpolator interpolator, Runnable finishRunnable) {
        CellState cellState2 = cellState;
        float f = startAlpha;
        cellState2.alpha = f;
        float f2 = startTranslationY;
        cellState2.translationY = f2;
        cellState2.radius = ((float) (this.mDotSize / 2)) * startScale;
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setInterpolator(interpolator);
        final CellState cellState3 = cellState2;
        final float f3 = f;
        AnonymousClass1 r10 = r0;
        final float f4 = endAlpha;
        final float f5 = f2;
        final float f6 = endTranslationY;
        final float f7 = startScale;
        final float f8 = endScale;
        AnonymousClass1 r0 = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = Float.valueOf(animation.getAnimatedValue().toString()).floatValue();
                cellState3.alpha = ((1.0f - t) * f3) + (f4 * t);
                cellState3.translationY = ((1.0f - t) * f5) + (f6 * t);
                cellState3.radius = ((float) (MiuiLockPatternView.this.mDotSize / 2)) * (((1.0f - t) * f7) + (f8 * t));
                MiuiLockPatternView.this.invalidate();
            }
        };
        animator.addUpdateListener(r10);
        final Runnable runnable = finishRunnable;
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        animator.start();
    }

    private void startCellStateAnimationHw(CellState cellState, float startAlpha, float endAlpha, float startTranslationY, float endTranslationY, float startScale, float endScale, long delay, long duration, Interpolator interpolator, Runnable finishRunnable) {
        final CellState cellState2 = cellState;
        float f = endTranslationY;
        float f2 = endAlpha;
        cellState2.alpha = f2;
        cellState2.translationY = f;
        cellState2.radius = ((float) (this.mDotSize / 2)) * endScale;
        cellState2.hwAnimating = true;
        cellState2.hwCenterY = CanvasProperty.createFloat(getCenterYForRow(cellState2.row) + startTranslationY);
        cellState2.hwCenterX = CanvasProperty.createFloat(getCenterXForColumn(cellState2.col));
        cellState2.hwRadius = CanvasProperty.createFloat(((float) (this.mDotSize / 2)) * startScale);
        this.mPaint.setColor(getCurrentColor(false));
        this.mPaint.setAlpha((int) (255.0f * startAlpha));
        cellState2.hwPaint = CanvasProperty.createPaint(new Paint(this.mPaint));
        long j = delay;
        long j2 = duration;
        Interpolator interpolator2 = interpolator;
        startRtFloatAnimation(cellState2.hwCenterY, getCenterYForRow(cellState2.row) + f, j, j2, interpolator2);
        startRtFloatAnimation(cellState2.hwRadius, ((float) (this.mDotSize / 2)) * endScale, j, j2, interpolator2);
        final Runnable runnable = finishRunnable;
        startRtAlphaAnimation(cellState2, f2, j, j2, interpolator, new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                cellState2.hwAnimating = false;
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        invalidate();
    }

    private void startRtAlphaAnimation(CellState cellState, float endAlpha, long delay, long duration, Interpolator interpolator, Animator.AnimatorListener listener) {
        RenderNodeAnimator animator = new RenderNodeAnimator(cellState.hwPaint, 1, (float) ((int) (255.0f * endAlpha)));
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setInterpolator(interpolator);
        animator.setTarget(this);
        animator.addListener(listener);
        animator.start();
    }

    private void startRtFloatAnimation(CanvasProperty<Float> property, float endValue, long delay, long duration, Interpolator interpolator) {
        RenderNodeAnimator animator = new RenderNodeAnimator(property, endValue);
        animator.setDuration(duration);
        animator.setStartDelay(delay);
        animator.setInterpolator(interpolator);
        animator.setTarget(this);
        animator.start();
    }

    private void notifyCellAdded() {
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternCellAdded(this.mPattern);
        }
        this.mExploreByTouchHelper.invalidateRoot();
    }

    private void notifyPatternStarted() {
        sendAccessEvent(R.string.lockscreen_access_pattern_start);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternStart();
        }
    }

    private void notifyPatternDetected() {
        sendAccessEvent(R.string.lockscreen_access_pattern_detected);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternDetected(this.mPattern);
        }
    }

    private void notifyPatternCleared() {
        sendAccessEvent(R.string.lockscreen_access_pattern_cleared);
        if (this.mOnPatternListener != null) {
            this.mOnPatternListener.onPatternCleared();
        }
    }

    public void clearPattern() {
        resetPattern();
    }

    /* access modifiers changed from: protected */
    public boolean dispatchHoverEvent(MotionEvent event) {
        return super.dispatchHoverEvent(event) | this.mExploreByTouchHelper.dispatchHoverEvent(event);
    }

    private void resetPattern() {
        this.mPattern.clear();
        clearPatternDrawLookup();
        this.mPatternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    private void clearPatternDrawLookup() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.mPatternDrawLookup[i][j] = false;
            }
        }
    }

    public void disableInput() {
        this.mInputEnabled = false;
    }

    public void enableInput() {
        this.mInputEnabled = true;
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        int width = (w - this.mPaddingLeft) - this.mPaddingRight;
        this.mSquareWidth = ((float) width) / 3.0f;
        int height = (h - this.mPaddingTop) - this.mPaddingBottom;
        this.mSquareHeight = ((float) height) / 3.0f;
        this.mExploreByTouchHelper.invalidateRoot();
        if (this.mUseLockPatternDrawable) {
            this.mNotSelectedDrawable.setBounds(this.mPaddingLeft, this.mPaddingTop, width, height);
            this.mSelectedDrawable.setBounds(this.mPaddingLeft, this.mPaddingTop, width, height);
        }
    }

    private int resolveMeasured(int measureSpec, int desired) {
        int specSize = View.MeasureSpec.getSize(measureSpec);
        int mode = View.MeasureSpec.getMode(measureSpec);
        if (mode == Integer.MIN_VALUE) {
            return Math.max(specSize, desired);
        }
        if (mode != 0) {
            return specSize;
        }
        return desired;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minimumWidth = getSuggestedMinimumWidth();
        int minimumHeight = getSuggestedMinimumHeight();
        int viewWidth = resolveMeasured(widthMeasureSpec, minimumWidth);
        int viewHeight = resolveMeasured(heightMeasureSpec, minimumHeight);
        switch (this.mAspect) {
            case 0:
                int min = Math.min(viewWidth, viewHeight);
                viewHeight = min;
                viewWidth = min;
                break;
            case 1:
                viewHeight = Math.min(viewWidth, viewHeight);
                break;
            case 2:
                viewWidth = Math.min(viewWidth, viewHeight);
                break;
        }
        Log.v("LockPatternView", "LockPatternView dimensions: " + viewWidth + "x" + viewHeight);
        setMeasuredDimension(viewWidth, viewHeight);
    }

    private LockPatternView.Cell detectAndAddHit(float x, float y) {
        LockPatternView.Cell cell = checkForNewHit(x, y);
        if (cell == null) {
            return null;
        }
        LockPatternView.Cell fillInGapCell = null;
        ArrayList<LockPatternView.Cell> pattern = this.mPattern;
        if (!pattern.isEmpty()) {
            LockPatternView.Cell lastCell = pattern.get(pattern.size() - 1);
            int dRow = cell.getRow() - lastCell.getRow();
            int dColumn = cell.getColumn() - lastCell.getColumn();
            int fillInRow = lastCell.getRow();
            int fillInColumn = lastCell.getColumn();
            int i = -1;
            if (Math.abs(dRow) == 2 && Math.abs(dColumn) != 1) {
                fillInRow = lastCell.getRow() + (dRow > 0 ? 1 : -1);
            }
            if (Math.abs(dColumn) == 2 && Math.abs(dRow) != 1) {
                int column = lastCell.getColumn();
                if (dColumn > 0) {
                    i = 1;
                }
                fillInColumn = column + i;
            }
            fillInGapCell = LockPatternView.Cell.of(fillInRow, fillInColumn);
        }
        if (fillInGapCell != null && !this.mPatternDrawLookup[fillInGapCell.getRow()][fillInGapCell.getColumn()]) {
            addCellToPattern(fillInGapCell);
        }
        addCellToPattern(cell);
        if (this.mEnableHapticFeedback) {
            performHapticFeedback(1, 3);
        }
        return cell;
    }

    private void addCellToPattern(LockPatternView.Cell newCell) {
        this.mPatternDrawLookup[newCell.getRow()][newCell.getColumn()] = true;
        this.mPattern.add(newCell);
        if (!this.mInStealthMode) {
            startCellActivatedAnimation(newCell);
        }
        notifyCellAdded();
    }

    private void startCellActivatedAnimation(LockPatternView.Cell cell) {
        final CellState cellState = this.mCellStates[cell.getRow()][cell.getColumn()];
        startRadiusAnimation((float) (this.mDotSize / 2), (float) (this.mDotSizeActivated / 2), 96, this.mLinearOutSlowInInterpolator, cellState, new Runnable() {
            public void run() {
                MiuiLockPatternView.this.startRadiusAnimation((float) (MiuiLockPatternView.this.mDotSizeActivated / 2), (float) (MiuiLockPatternView.this.mDotSize / 2), 192, MiuiLockPatternView.this.mFastOutSlowInInterpolator, cellState, null);
            }
        });
        startLineEndAnimation(cellState, this.mInProgressX, this.mInProgressY, getCenterXForColumn(cell.getColumn()), getCenterYForRow(cell.getRow()));
    }

    private void startLineEndAnimation(final CellState state, float startX, float startY, float targetX, float targetY) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        final CellState cellState = state;
        final float f = startX;
        final float f2 = targetX;
        final float f3 = startY;
        final float f4 = targetY;
        AnonymousClass5 r1 = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = Float.valueOf(animation.getAnimatedValue().toString()).floatValue();
                cellState.lineEndX = ((1.0f - t) * f) + (f2 * t);
                cellState.lineEndY = ((1.0f - t) * f3) + (f4 * t);
                MiuiLockPatternView.this.invalidate();
            }
        };
        valueAnimator.addUpdateListener(r1);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                state.lineAnimator = null;
            }
        });
        valueAnimator.setInterpolator(this.mFastOutSlowInInterpolator);
        valueAnimator.setDuration(100);
        valueAnimator.start();
        state.lineAnimator = valueAnimator;
    }

    /* access modifiers changed from: private */
    public void startRadiusAnimation(float start, float end, long duration, Interpolator interpolator, final CellState state, final Runnable endRunnable) {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{start, end});
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                state.radius = Float.valueOf(animation.getAnimatedValue().toString()).floatValue();
                MiuiLockPatternView.this.invalidate();
            }
        });
        if (endRunnable != null) {
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    endRunnable.run();
                }
            });
        }
        valueAnimator.setInterpolator(interpolator);
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    private LockPatternView.Cell checkForNewHit(float x, float y) {
        int rowHit = getRowHit(y);
        if (rowHit < 0) {
            return null;
        }
        int columnHit = getColumnHit(x);
        if (columnHit >= 0 && !this.mPatternDrawLookup[rowHit][columnHit]) {
            return LockPatternView.Cell.of(rowHit, columnHit);
        }
        return null;
    }

    /* access modifiers changed from: private */
    public int getRowHit(float y) {
        float squareHeight = this.mSquareHeight;
        float hitSize = this.mHitFactor * squareHeight;
        float offset = ((float) this.mPaddingTop) + ((squareHeight - hitSize) / 2.0f);
        for (int i = 0; i < 3; i++) {
            float hitTop = (((float) i) * squareHeight) + offset;
            if (y >= hitTop && y <= hitTop + hitSize) {
                return i;
            }
        }
        return -1;
    }

    /* access modifiers changed from: private */
    public int getColumnHit(float x) {
        float squareWidth = this.mSquareWidth;
        float hitSize = this.mHitFactor * squareWidth;
        float offset = ((float) this.mPaddingLeft) + ((squareWidth - hitSize) / 2.0f);
        for (int i = 0; i < 3; i++) {
            float hitLeft = (((float) i) * squareWidth) + offset;
            if (x >= hitLeft && x <= hitLeft + hitSize) {
                return i;
            }
        }
        return -1;
    }

    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(this.mContext).isTouchExplorationEnabled()) {
            int action = event.getAction();
            if (action != 7) {
                switch (action) {
                    case 9:
                        event.setAction(0);
                        break;
                    case 10:
                        event.setAction(1);
                        break;
                }
            } else {
                event.setAction(2);
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        return super.onHoverEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mInputEnabled || !isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                handleActionDown(event);
                return true;
            case 1:
                handleActionUp();
                return true;
            case 2:
                handleActionMove(event);
                return true;
            case 3:
                if (this.mPatternInProgress) {
                    setPatternInProgress(false);
                    resetPattern();
                    notifyPatternCleared();
                }
                return true;
            default:
                return false;
        }
    }

    private void setPatternInProgress(boolean progress) {
        this.mPatternInProgress = progress;
        this.mExploreByTouchHelper.invalidateRoot();
    }

    private void handleActionMove(MotionEvent event) {
        float radius;
        boolean invalidateNow;
        int historySize;
        MotionEvent motionEvent = event;
        float radius2 = (float) this.mPathWidth;
        int historySize2 = event.getHistorySize();
        this.mTmpInvalidateRect.setEmpty();
        boolean invalidateNow2 = false;
        int i = 0;
        while (i < historySize2 + 1) {
            float x = i < historySize2 ? motionEvent.getHistoricalX(i) : event.getX();
            float y = i < historySize2 ? motionEvent.getHistoricalY(i) : event.getY();
            LockPatternView.Cell hitCell = detectAndAddHit(x, y);
            int patternSize = this.mPattern.size();
            if (hitCell != null && patternSize == 1) {
                setPatternInProgress(true);
                notifyPatternStarted();
            }
            float dx = Math.abs(x - this.mInProgressX);
            float dy = Math.abs(y - this.mInProgressY);
            if (dx > 0.0f || dy > 0.0f) {
                invalidateNow2 = true;
            }
            if (!this.mPatternInProgress || patternSize <= 0) {
                radius = radius2;
                historySize = historySize2;
                invalidateNow = invalidateNow2;
            } else {
                LockPatternView.Cell lastCell = this.mPattern.get(patternSize - 1);
                float lastCellCenterX = getCenterXForColumn(lastCell.getColumn());
                float lastCellCenterY = getCenterYForRow(lastCell.getRow());
                historySize = historySize2;
                float left = Math.min(lastCellCenterX, x) - radius2;
                invalidateNow = invalidateNow2;
                float right = Math.max(lastCellCenterX, x) + radius2;
                float f = x;
                float top = Math.min(lastCellCenterY, y) - radius2;
                float f2 = y;
                float bottom = Math.max(lastCellCenterY, y) + radius2;
                if (hitCell != null) {
                    radius = radius2;
                    float width = this.mSquareWidth * 0.5f;
                    int i2 = patternSize;
                    float height = this.mSquareHeight * 0.5f;
                    float f3 = dx;
                    float dx2 = getCenterXForColumn(hitCell.getColumn());
                    float f4 = dy;
                    float dy2 = getCenterYForRow(hitCell.getRow());
                    LockPatternView.Cell cell = hitCell;
                    left = Math.min(dx2 - width, left);
                    right = Math.max(dx2 + width, right);
                    top = Math.min(dy2 - height, top);
                    bottom = Math.max(dy2 + height, bottom);
                } else {
                    radius = radius2;
                    LockPatternView.Cell cell2 = hitCell;
                    int i3 = patternSize;
                    float f5 = dx;
                    float f6 = dy;
                }
                this.mTmpInvalidateRect.union(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
            }
            i++;
            historySize2 = historySize;
            invalidateNow2 = invalidateNow;
            radius2 = radius;
        }
        int i4 = historySize2;
        this.mInProgressX = event.getX();
        this.mInProgressY = event.getY();
        if (invalidateNow2) {
            this.mInvalidate.union(this.mTmpInvalidateRect);
            invalidate(this.mInvalidate);
            this.mInvalidate.set(this.mTmpInvalidateRect);
        }
    }

    private void sendAccessEvent(int resId) {
        announceForAccessibility(this.mContext.getString(resId));
    }

    private void handleActionUp() {
        if (!this.mPattern.isEmpty()) {
            setPatternInProgress(false);
            cancelLineAnimations();
            notifyPatternDetected();
            invalidate();
        }
    }

    private void cancelLineAnimations() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                CellState state = this.mCellStates[i][j];
                if (state.lineAnimator != null) {
                    state.lineAnimator.cancel();
                    state.lineEndX = Float.MIN_VALUE;
                    state.lineEndY = Float.MIN_VALUE;
                }
            }
        }
    }

    private void handleActionDown(MotionEvent event) {
        resetPattern();
        float x = event.getX();
        float y = event.getY();
        LockPatternView.Cell hitCell = detectAndAddHit(x, y);
        if (hitCell != null) {
            setPatternInProgress(true);
            this.mPatternDisplayMode = DisplayMode.Correct;
            notifyPatternStarted();
        } else if (this.mPatternInProgress) {
            setPatternInProgress(false);
            notifyPatternCleared();
        }
        if (hitCell != null) {
            float startX = getCenterXForColumn(hitCell.getColumn());
            float startY = getCenterYForRow(hitCell.getRow());
            float widthOffset = this.mSquareWidth / 2.0f;
            float heightOffset = this.mSquareHeight / 2.0f;
            invalidate((int) (startX - widthOffset), (int) (startY - heightOffset), (int) (startX + widthOffset), (int) (startY + heightOffset));
        }
        this.mInProgressX = x;
        this.mInProgressY = y;
    }

    /* access modifiers changed from: private */
    public float getCenterXForColumn(int column) {
        return ((float) this.mPaddingLeft) + (((float) column) * this.mSquareWidth) + (this.mSquareWidth / 2.0f);
    }

    /* access modifiers changed from: private */
    public float getCenterYForRow(int row) {
        return ((float) this.mPaddingTop) + (((float) row) * this.mSquareHeight) + (this.mSquareHeight / 2.0f);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        Path currentPath;
        boolean drawPath;
        Path currentPath2;
        Path currentPath3;
        int j;
        Canvas canvas2 = canvas;
        ArrayList<LockPatternView.Cell> pattern = this.mPattern;
        int count = pattern.size();
        boolean[][] drawLookup = this.mPatternDrawLookup;
        if (this.mPatternDisplayMode == DisplayMode.Animate) {
            int oneCycle = (count + 1) * 700;
            int spotInCycle = ((int) (SystemClock.elapsedRealtime() - this.mAnimatingPeriodStart)) % oneCycle;
            int numCircles = spotInCycle / 700;
            clearPatternDrawLookup();
            for (int i = 0; i < numCircles; i++) {
                LockPatternView.Cell cell = pattern.get(i);
                drawLookup[cell.getRow()][cell.getColumn()] = true;
            }
            if (numCircles > 0 && numCircles < count) {
                float percentageOfNextCircle = ((float) (spotInCycle % 700)) / 700.0f;
                LockPatternView.Cell currentCell = pattern.get(numCircles - 1);
                float centerX = getCenterXForColumn(currentCell.getColumn());
                float centerY = getCenterYForRow(currentCell.getRow());
                LockPatternView.Cell nextCell = pattern.get(numCircles);
                int i2 = oneCycle;
                this.mInProgressX = centerX + ((getCenterXForColumn(nextCell.getColumn()) - centerX) * percentageOfNextCircle);
                this.mInProgressY = centerY + ((getCenterYForRow(nextCell.getRow()) - centerY) * percentageOfNextCircle);
            }
            invalidate();
        }
        Path currentPath4 = this.mCurrentPath;
        currentPath4.rewind();
        int i3 = 0;
        while (true) {
            int i4 = i3;
            int i5 = 3;
            if (i4 >= 3) {
                break;
            }
            float centerY2 = getCenterYForRow(i4);
            int j2 = 0;
            while (true) {
                int j3 = j2;
                if (j3 >= i5) {
                    break;
                }
                CellState cellState = this.mCellStates[i4][j3];
                float centerX2 = getCenterXForColumn(j3);
                float translationY = cellState.translationY;
                if (this.mUseLockPatternDrawable) {
                    float f = translationY;
                    float f2 = centerX2;
                    currentPath3 = currentPath4;
                    CellState cellState2 = cellState;
                    drawCellDrawable(canvas2, i4, j3, cellState.radius, drawLookup[i4][j3]);
                } else {
                    float translationY2 = translationY;
                    float centerX3 = centerX2;
                    currentPath3 = currentPath4;
                    CellState cellState3 = cellState;
                    if (!isHardwareAccelerated() || !cellState3.hwAnimating) {
                        j = j3;
                        drawCircle(canvas2, (float) ((int) centerX3), ((float) ((int) centerY2)) + translationY2, cellState3.radius, drawLookup[i4][j3], cellState3.alpha);
                        j2 = j + 1;
                        currentPath4 = currentPath3;
                        i5 = 3;
                    } else {
                        ((DisplayListCanvas) canvas2).drawCircle(cellState3.hwCenterX, cellState3.hwCenterY, cellState3.hwRadius, cellState3.hwPaint);
                    }
                }
                j = j3;
                j2 = j + 1;
                currentPath4 = currentPath3;
                i5 = 3;
            }
            i3 = i4 + 1;
        }
        Path currentPath5 = currentPath4;
        boolean drawPath2 = !this.mInStealthMode;
        if (drawPath2) {
            this.mPathPaint.setColor(getCurrentColor(true));
            boolean anyCircles = false;
            float lastX = 0.0f;
            float lastY = 0.0f;
            int i6 = 0;
            while (true) {
                int i7 = i6;
                if (i7 >= count) {
                    currentPath = currentPath5;
                    break;
                }
                LockPatternView.Cell cell2 = pattern.get(i7);
                if (!drawLookup[cell2.getRow()][cell2.getColumn()]) {
                    boolean z = drawPath2;
                    currentPath = currentPath5;
                    break;
                }
                anyCircles = true;
                float centerX4 = getCenterXForColumn(cell2.getColumn());
                float centerY3 = getCenterYForRow(cell2.getRow());
                if (i7 != 0) {
                    CellState state = this.mCellStates[cell2.getRow()][cell2.getColumn()];
                    currentPath2 = currentPath5;
                    currentPath2.rewind();
                    currentPath2.moveTo(lastX, lastY);
                    if (state.lineEndX == Float.MIN_VALUE || state.lineEndY == Float.MIN_VALUE) {
                        drawPath = drawPath2;
                        currentPath2.lineTo(centerX4, centerY3);
                    } else {
                        drawPath = drawPath2;
                        currentPath2.lineTo(state.lineEndX, state.lineEndY);
                    }
                    canvas2.drawPath(currentPath2, this.mPathPaint);
                } else {
                    drawPath = drawPath2;
                    currentPath2 = currentPath5;
                }
                lastX = centerX4;
                lastY = centerY3;
                i6 = i7 + 1;
                currentPath5 = currentPath2;
                drawPath2 = drawPath;
            }
            if ((this.mPatternInProgress || this.mPatternDisplayMode == DisplayMode.Animate) && anyCircles) {
                currentPath.rewind();
                currentPath.moveTo(lastX, lastY);
                currentPath.lineTo(this.mInProgressX, this.mInProgressY);
                this.mPathPaint.setAlpha((int) (calculateLastSegmentAlpha(this.mInProgressX, this.mInProgressY, lastX, lastY) * 255.0f));
                canvas2.drawPath(currentPath, this.mPathPaint);
                return;
            }
            return;
        }
        Path path = currentPath5;
    }

    private float calculateLastSegmentAlpha(float x, float y, float lastX, float lastY) {
        float diffX = x - lastX;
        float diffY = y - lastY;
        return Math.min(1.0f, Math.max(0.0f, ((((float) Math.sqrt((double) ((diffX * diffX) + (diffY * diffY)))) / this.mSquareWidth) - 0.3f) * 4.0f));
    }

    private int getCurrentColor(boolean partOfPattern) {
        if (!partOfPattern || this.mInStealthMode || this.mPatternInProgress) {
            return this.mRegularColor;
        }
        if (this.mPatternDisplayMode == DisplayMode.Wrong) {
            return this.mErrorColor;
        }
        if (this.mPatternDisplayMode == DisplayMode.Correct || this.mPatternDisplayMode == DisplayMode.Animate) {
            return this.mSuccessColor;
        }
        throw new IllegalStateException("unknown display mode " + this.mPatternDisplayMode);
    }

    private void drawCircle(Canvas canvas, float centerX, float centerY, float radius, boolean partOfPattern, float alpha) {
        this.mPaint.setColor(getCurrentColor(partOfPattern));
        this.mPaint.setAlpha((int) (255.0f * alpha));
        canvas.drawCircle(centerX, centerY, radius, this.mPaint);
    }

    private void drawCellDrawable(Canvas canvas, int i, int j, float radius, boolean partOfPattern) {
        Rect dst = new Rect((int) (((float) this.mPaddingLeft) + (((float) j) * this.mSquareWidth)), (int) (((float) this.mPaddingTop) + (((float) i) * this.mSquareHeight)), (int) (((float) this.mPaddingLeft) + (((float) (j + 1)) * this.mSquareWidth)), (int) (((float) this.mPaddingTop) + (((float) (i + 1)) * this.mSquareHeight)));
        float scale = radius / ((float) (this.mDotSize / 2));
        canvas.save();
        canvas.clipRect(dst);
        canvas.scale(scale, scale, (float) dst.centerX(), (float) dst.centerY());
        if (!partOfPattern || scale > 1.0f) {
            this.mNotSelectedDrawable.draw(canvas);
        } else {
            this.mSelectedDrawable.draw(canvas);
        }
        canvas.restore();
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState(), null, this.mPatternDisplayMode.ordinal(), this.mInputEnabled, this.mInStealthMode, this.mEnableHapticFeedback);
        return savedState;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mPatternDisplayMode = DisplayMode.values()[ss.getDisplayMode()];
        this.mInputEnabled = ss.isInputEnabled();
        this.mInStealthMode = ss.isInStealthMode();
        this.mEnableHapticFeedback = ss.isTactileFeedbackEnabled();
    }
}
