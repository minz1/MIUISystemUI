package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class DessertCaseView extends FrameLayout {
    private static final float[] ALPHA_MASK = {0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    private static final float[] MASK = {0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private static final int NUM_PASTRIES = (((PASTRIES.length + RARE_PASTRIES.length) + XRARE_PASTRIES.length) + XXRARE_PASTRIES.length);
    private static final int[] PASTRIES = {R.drawable.dessert_kitkat, R.drawable.dessert_android};
    private static final int[] RARE_PASTRIES = {R.drawable.dessert_cupcake, R.drawable.dessert_donut, R.drawable.dessert_eclair, R.drawable.dessert_froyo, R.drawable.dessert_gingerbread, R.drawable.dessert_honeycomb, R.drawable.dessert_ics, R.drawable.dessert_jellybean};
    private static final String TAG = DessertCaseView.class.getSimpleName();
    private static final float[] WHITE_MASK = {0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, 0.0f, 0.0f, 0.0f, 0.0f, 255.0f, -1.0f, 0.0f, 0.0f, 0.0f, 255.0f};
    private static final int[] XRARE_PASTRIES = {R.drawable.dessert_petitfour, R.drawable.dessert_donutburger, R.drawable.dessert_flan, R.drawable.dessert_keylimepie};
    private static final int[] XXRARE_PASTRIES = {R.drawable.dessert_zombiegingerbread, R.drawable.dessert_dandroid, R.drawable.dessert_jandycane};
    float[] hsv;
    private int mCellSize;
    private View[] mCells;
    private int mColumns;
    private SparseArray<Drawable> mDrawables;
    private final Set<Point> mFreeList;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private int mHeight;
    /* access modifiers changed from: private */
    public final Runnable mJuggle;
    private int mRows;
    /* access modifiers changed from: private */
    public boolean mStarted;
    private int mWidth;
    private final HashSet<View> tmpSet;

    public static class RescalingContainer extends FrameLayout {
        private DessertCaseView mView;

        public RescalingContainer(Context context) {
            super(context);
            setSystemUiVisibility(5638);
        }

        public void setView(DessertCaseView v) {
            addView(v);
            this.mView = v;
        }

        /* access modifiers changed from: protected */
        public void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int i = left;
            int i2 = top;
            float w = (float) (right - i);
            float h = (float) (bottom - i2);
            DessertCaseView dessertCaseView = this.mView;
            int w2 = (int) ((w / 0.25f) / 2.0f);
            DessertCaseView dessertCaseView2 = this.mView;
            int h2 = (int) ((h / 0.25f) / 2.0f);
            int cx = (int) (((float) i) + (w * 0.5f));
            int cy = (int) (((float) i2) + (0.5f * h));
            this.mView.layout(cx - w2, cy - h2, cx + w2, cy + h2);
        }
    }

    public DessertCaseView(Context context) {
        this(context, null);
    }

    public DessertCaseView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public DessertCaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDrawables = new SparseArray<>(NUM_PASTRIES);
        this.mFreeList = new HashSet();
        this.mHandler = new Handler();
        this.mJuggle = new Runnable() {
            public void run() {
                int N = DessertCaseView.this.getChildCount();
                for (int i = 0; i < 1; i++) {
                    DessertCaseView.this.place(DessertCaseView.this.getChildAt((int) (Math.random() * ((double) N))), true);
                }
                DessertCaseView.this.fillFreeList();
                if (DessertCaseView.this.mStarted) {
                    DessertCaseView.this.mHandler.postDelayed(DessertCaseView.this.mJuggle, 2000);
                }
            }
        };
        this.hsv = new float[]{0.0f, 1.0f, 0.85f};
        this.tmpSet = new HashSet<>();
        Resources res = getResources();
        this.mStarted = false;
        this.mCellSize = res.getDimensionPixelSize(R.dimen.dessert_case_cell_size);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        if (this.mCellSize < 512) {
            opts.inSampleSize = 2;
        }
        opts.inMutable = true;
        Bitmap loaded = null;
        int[][] iArr = {PASTRIES, RARE_PASTRIES, XRARE_PASTRIES, XXRARE_PASTRIES};
        int length = iArr.length;
        int i = 0;
        while (i < length) {
            Bitmap loaded2 = loaded;
            for (int resid : iArr[i]) {
                opts.inBitmap = loaded2;
                loaded2 = BitmapFactory.decodeResource(res, resid, opts);
                BitmapDrawable d = new BitmapDrawable(res, convertToAlphaMask(loaded2));
                d.setColorFilter(new ColorMatrixColorFilter(ALPHA_MASK));
                d.setBounds(0, 0, this.mCellSize, this.mCellSize);
                this.mDrawables.append(resid, d);
            }
            i++;
            loaded = loaded2;
        }
    }

    private static Bitmap convertToAlphaMask(Bitmap b) {
        Bitmap a = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(a);
        Paint pt = new Paint();
        pt.setColorFilter(new ColorMatrixColorFilter(MASK));
        c.drawBitmap(b, 0.0f, 0.0f, pt);
        return a;
    }

    public void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            fillFreeList(2000);
        }
        this.mHandler.postDelayed(this.mJuggle, 5000);
    }

    public void stop() {
        this.mStarted = false;
        this.mHandler.removeCallbacks(this.mJuggle);
    }

    /* access modifiers changed from: package-private */
    public int pick(int[] a) {
        return a[(int) (Math.random() * ((double) a.length))];
    }

    /* access modifiers changed from: package-private */
    public int random_color() {
        this.hsv[0] = ((float) irand(0, 12)) * 30.0f;
        return Color.HSVToColor(this.hsv);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0082, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void onSizeChanged(int r7, int r8, int r9, int r10) {
        /*
            r6 = this;
            monitor-enter(r6)
            super.onSizeChanged(r7, r8, r9, r10)     // Catch:{ all -> 0x0083 }
            int r0 = r6.mWidth     // Catch:{ all -> 0x0083 }
            if (r0 != r7) goto L_0x000e
            int r0 = r6.mHeight     // Catch:{ all -> 0x0083 }
            if (r0 != r8) goto L_0x000e
            monitor-exit(r6)
            return
        L_0x000e:
            boolean r0 = r6.mStarted     // Catch:{ all -> 0x0083 }
            if (r0 == 0) goto L_0x0015
            r6.stop()     // Catch:{ all -> 0x0083 }
        L_0x0015:
            r6.mWidth = r7     // Catch:{ all -> 0x0083 }
            r6.mHeight = r8     // Catch:{ all -> 0x0083 }
            r1 = 0
            r6.mCells = r1     // Catch:{ all -> 0x0083 }
            r6.removeAllViewsInLayout()     // Catch:{ all -> 0x0083 }
            java.util.Set<android.graphics.Point> r1 = r6.mFreeList     // Catch:{ all -> 0x0083 }
            r1.clear()     // Catch:{ all -> 0x0083 }
            int r1 = r6.mHeight     // Catch:{ all -> 0x0083 }
            int r2 = r6.mCellSize     // Catch:{ all -> 0x0083 }
            int r1 = r1 / r2
            r6.mRows = r1     // Catch:{ all -> 0x0083 }
            int r1 = r6.mWidth     // Catch:{ all -> 0x0083 }
            int r2 = r6.mCellSize     // Catch:{ all -> 0x0083 }
            int r1 = r1 / r2
            r6.mColumns = r1     // Catch:{ all -> 0x0083 }
            int r1 = r6.mRows     // Catch:{ all -> 0x0083 }
            int r2 = r6.mColumns     // Catch:{ all -> 0x0083 }
            int r1 = r1 * r2
            android.view.View[] r1 = new android.view.View[r1]     // Catch:{ all -> 0x0083 }
            r6.mCells = r1     // Catch:{ all -> 0x0083 }
            r1 = 1048576000(0x3e800000, float:0.25)
            r6.setScaleX(r1)     // Catch:{ all -> 0x0083 }
            r6.setScaleY(r1)     // Catch:{ all -> 0x0083 }
            int r2 = r6.mWidth     // Catch:{ all -> 0x0083 }
            int r3 = r6.mCellSize     // Catch:{ all -> 0x0083 }
            int r4 = r6.mColumns     // Catch:{ all -> 0x0083 }
            int r3 = r3 * r4
            int r2 = r2 - r3
            float r2 = (float) r2     // Catch:{ all -> 0x0083 }
            r3 = 1056964608(0x3f000000, float:0.5)
            float r2 = r2 * r3
            float r2 = r2 * r1
            r6.setTranslationX(r2)     // Catch:{ all -> 0x0083 }
            int r2 = r6.mHeight     // Catch:{ all -> 0x0083 }
            int r4 = r6.mCellSize     // Catch:{ all -> 0x0083 }
            int r5 = r6.mRows     // Catch:{ all -> 0x0083 }
            int r4 = r4 * r5
            int r2 = r2 - r4
            float r2 = (float) r2     // Catch:{ all -> 0x0083 }
            float r3 = r3 * r2
            float r3 = r3 * r1
            r6.setTranslationY(r3)     // Catch:{ all -> 0x0083 }
            r1 = 0
            r2 = r1
        L_0x0063:
            int r3 = r6.mRows     // Catch:{ all -> 0x0083 }
            if (r2 >= r3) goto L_0x007c
            r3 = r1
        L_0x0068:
            int r4 = r6.mColumns     // Catch:{ all -> 0x0083 }
            if (r3 >= r4) goto L_0x0079
            java.util.Set<android.graphics.Point> r4 = r6.mFreeList     // Catch:{ all -> 0x0083 }
            android.graphics.Point r5 = new android.graphics.Point     // Catch:{ all -> 0x0083 }
            r5.<init>(r3, r2)     // Catch:{ all -> 0x0083 }
            r4.add(r5)     // Catch:{ all -> 0x0083 }
            int r3 = r3 + 1
            goto L_0x0068
        L_0x0079:
            int r2 = r2 + 1
            goto L_0x0063
        L_0x007c:
            if (r0 == 0) goto L_0x0081
            r6.start()     // Catch:{ all -> 0x0083 }
        L_0x0081:
            monitor-exit(r6)
            return
        L_0x0083:
            r7 = move-exception
            monitor-exit(r6)
            throw r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.DessertCaseView.onSizeChanged(int, int, int, int):void");
    }

    public void fillFreeList() {
        fillFreeList(500);
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x00a8  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x00be  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x000e A[SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void fillFreeList(int r14) {
        /*
            r13 = this;
            monitor-enter(r13)
            android.content.Context r0 = r13.getContext()     // Catch:{ all -> 0x00f7 }
            android.widget.FrameLayout$LayoutParams r1 = new android.widget.FrameLayout$LayoutParams     // Catch:{ all -> 0x00f7 }
            int r2 = r13.mCellSize     // Catch:{ all -> 0x00f7 }
            int r3 = r13.mCellSize     // Catch:{ all -> 0x00f7 }
            r1.<init>(r2, r3)     // Catch:{ all -> 0x00f7 }
        L_0x000e:
            java.util.Set<android.graphics.Point> r2 = r13.mFreeList     // Catch:{ all -> 0x00f7 }
            boolean r2 = r2.isEmpty()     // Catch:{ all -> 0x00f7 }
            if (r2 != 0) goto L_0x00f5
            java.util.Set<android.graphics.Point> r2 = r13.mFreeList     // Catch:{ all -> 0x00f7 }
            java.util.Iterator r2 = r2.iterator()     // Catch:{ all -> 0x00f7 }
            java.lang.Object r2 = r2.next()     // Catch:{ all -> 0x00f7 }
            android.graphics.Point r2 = (android.graphics.Point) r2     // Catch:{ all -> 0x00f7 }
            java.util.Set<android.graphics.Point> r3 = r13.mFreeList     // Catch:{ all -> 0x00f7 }
            r3.remove(r2)     // Catch:{ all -> 0x00f7 }
            int r3 = r2.x     // Catch:{ all -> 0x00f7 }
            int r4 = r2.y     // Catch:{ all -> 0x00f7 }
            android.view.View[] r5 = r13.mCells     // Catch:{ all -> 0x00f7 }
            int r6 = r13.mColumns     // Catch:{ all -> 0x00f7 }
            int r6 = r6 * r4
            int r6 = r6 + r3
            r5 = r5[r6]     // Catch:{ all -> 0x00f7 }
            if (r5 == 0) goto L_0x0036
            goto L_0x000e
        L_0x0036:
            android.widget.ImageView r5 = new android.widget.ImageView     // Catch:{ all -> 0x00f7 }
            r5.<init>(r0)     // Catch:{ all -> 0x00f7 }
            com.android.systemui.DessertCaseView$2 r6 = new com.android.systemui.DessertCaseView$2     // Catch:{ all -> 0x00f7 }
            r6.<init>(r5)     // Catch:{ all -> 0x00f7 }
            r5.setOnClickListener(r6)     // Catch:{ all -> 0x00f7 }
            int r6 = r13.random_color()     // Catch:{ all -> 0x00f7 }
            r5.setBackgroundColor(r6)     // Catch:{ all -> 0x00f7 }
            float r7 = frand()     // Catch:{ all -> 0x00f7 }
            r8 = 973279855(0x3a03126f, float:5.0E-4)
            int r8 = (r7 > r8 ? 1 : (r7 == r8 ? 0 : -1))
            r9 = 1056964608(0x3f000000, float:0.5)
            if (r8 >= 0) goto L_0x0066
            android.util.SparseArray<android.graphics.drawable.Drawable> r8 = r13.mDrawables     // Catch:{ all -> 0x00f7 }
            int[] r10 = XXRARE_PASTRIES     // Catch:{ all -> 0x00f7 }
            int r10 = r13.pick(r10)     // Catch:{ all -> 0x00f7 }
            java.lang.Object r8 = r8.get(r10)     // Catch:{ all -> 0x00f7 }
            android.graphics.drawable.Drawable r8 = (android.graphics.drawable.Drawable) r8     // Catch:{ all -> 0x00f7 }
        L_0x0065:
            goto L_0x00a6
        L_0x0066:
            r8 = 1000593162(0x3ba3d70a, float:0.005)
            int r8 = (r7 > r8 ? 1 : (r7 == r8 ? 0 : -1))
            if (r8 >= 0) goto L_0x007c
            android.util.SparseArray<android.graphics.drawable.Drawable> r8 = r13.mDrawables     // Catch:{ all -> 0x00f7 }
            int[] r10 = XRARE_PASTRIES     // Catch:{ all -> 0x00f7 }
            int r10 = r13.pick(r10)     // Catch:{ all -> 0x00f7 }
            java.lang.Object r8 = r8.get(r10)     // Catch:{ all -> 0x00f7 }
            android.graphics.drawable.Drawable r8 = (android.graphics.drawable.Drawable) r8     // Catch:{ all -> 0x00f7 }
            goto L_0x0065
        L_0x007c:
            int r8 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1))
            if (r8 >= 0) goto L_0x008f
            android.util.SparseArray<android.graphics.drawable.Drawable> r8 = r13.mDrawables     // Catch:{ all -> 0x00f7 }
            int[] r10 = RARE_PASTRIES     // Catch:{ all -> 0x00f7 }
            int r10 = r13.pick(r10)     // Catch:{ all -> 0x00f7 }
            java.lang.Object r8 = r8.get(r10)     // Catch:{ all -> 0x00f7 }
            android.graphics.drawable.Drawable r8 = (android.graphics.drawable.Drawable) r8     // Catch:{ all -> 0x00f7 }
            goto L_0x0065
        L_0x008f:
            r8 = 1060320051(0x3f333333, float:0.7)
            int r8 = (r7 > r8 ? 1 : (r7 == r8 ? 0 : -1))
            if (r8 >= 0) goto L_0x00a5
            android.util.SparseArray<android.graphics.drawable.Drawable> r8 = r13.mDrawables     // Catch:{ all -> 0x00f7 }
            int[] r10 = PASTRIES     // Catch:{ all -> 0x00f7 }
            int r10 = r13.pick(r10)     // Catch:{ all -> 0x00f7 }
            java.lang.Object r8 = r8.get(r10)     // Catch:{ all -> 0x00f7 }
            android.graphics.drawable.Drawable r8 = (android.graphics.drawable.Drawable) r8     // Catch:{ all -> 0x00f7 }
            goto L_0x0065
        L_0x00a5:
            r8 = 0
        L_0x00a6:
            if (r8 == 0) goto L_0x00af
            android.view.ViewOverlay r10 = r5.getOverlay()     // Catch:{ all -> 0x00f7 }
            r10.add(r8)     // Catch:{ all -> 0x00f7 }
        L_0x00af:
            int r10 = r13.mCellSize     // Catch:{ all -> 0x00f7 }
            r1.height = r10     // Catch:{ all -> 0x00f7 }
            r1.width = r10     // Catch:{ all -> 0x00f7 }
            r13.addView(r5, r1)     // Catch:{ all -> 0x00f7 }
            r10 = 0
            r13.place(r5, r2, r10)     // Catch:{ all -> 0x00f7 }
            if (r14 <= 0) goto L_0x00f3
            r10 = 33554434(0x2000002, float:9.403957E-38)
            java.lang.Object r10 = r5.getTag(r10)     // Catch:{ all -> 0x00f7 }
            java.lang.Integer r10 = (java.lang.Integer) r10     // Catch:{ all -> 0x00f7 }
            int r10 = r10.intValue()     // Catch:{ all -> 0x00f7 }
            float r10 = (float) r10     // Catch:{ all -> 0x00f7 }
            float r11 = r9 * r10
            r5.setScaleX(r11)     // Catch:{ all -> 0x00f7 }
            float r9 = r9 * r10
            r5.setScaleY(r9)     // Catch:{ all -> 0x00f7 }
            r9 = 0
            r5.setAlpha(r9)     // Catch:{ all -> 0x00f7 }
            android.view.ViewPropertyAnimator r9 = r5.animate()     // Catch:{ all -> 0x00f7 }
            android.view.ViewPropertyAnimator r9 = r9.withLayer()     // Catch:{ all -> 0x00f7 }
            android.view.ViewPropertyAnimator r9 = r9.scaleX(r10)     // Catch:{ all -> 0x00f7 }
            android.view.ViewPropertyAnimator r9 = r9.scaleY(r10)     // Catch:{ all -> 0x00f7 }
            r11 = 1065353216(0x3f800000, float:1.0)
            android.view.ViewPropertyAnimator r9 = r9.alpha(r11)     // Catch:{ all -> 0x00f7 }
            long r11 = (long) r14     // Catch:{ all -> 0x00f7 }
            r9.setDuration(r11)     // Catch:{ all -> 0x00f7 }
        L_0x00f3:
            goto L_0x000e
        L_0x00f5:
            monitor-exit(r13)
            return
        L_0x00f7:
            r14 = move-exception
            monitor-exit(r13)
            throw r14
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.DessertCaseView.fillFreeList(int):void");
    }

    public void place(View v, boolean animate) {
        place(v, new Point(irand(0, this.mColumns), irand(0, this.mRows)), animate);
    }

    private final Animator.AnimatorListener makeHardwareLayerListener(final View v) {
        return new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                if (v.isAttachedToWindow()) {
                    v.setLayerType(2, null);
                    v.buildLayer();
                }
            }

            public void onAnimationEnd(Animator animator) {
                v.setLayerType(0, null);
            }
        };
    }

    public synchronized void place(View v, Point pt, boolean animate) {
        Object obj;
        View view = v;
        Point point = pt;
        synchronized (this) {
            int i = point.x;
            int j = point.y;
            float rnd = frand();
            if (view.getTag(33554433) != null) {
                for (Point oc : getOccupied(v)) {
                    this.mFreeList.add(oc);
                    this.mCells[(oc.y * this.mColumns) + oc.x] = null;
                }
            }
            int scale = 1;
            if (rnd < 0.01f) {
                if (i < this.mColumns - 3 && j < this.mRows - 3) {
                    scale = 4;
                }
            } else if (rnd < 0.1f) {
                if (i < this.mColumns - 2 && j < this.mRows - 2) {
                    scale = 3;
                }
            } else if (!(rnd >= 0.33f || i == this.mColumns - 1 || j == this.mRows - 1)) {
                scale = 2;
            }
            view.setTag(33554433, point);
            view.setTag(33554434, Integer.valueOf(scale));
            this.tmpSet.clear();
            Point[] occupied = getOccupied(v);
            for (Point oc2 : occupied) {
                View squatter = this.mCells[(oc2.y * this.mColumns) + oc2.x];
                if (squatter != null) {
                    this.tmpSet.add(squatter);
                }
            }
            Iterator<View> it = this.tmpSet.iterator();
            while (it.hasNext()) {
                final View squatter2 = it.next();
                Point[] occupied2 = getOccupied(squatter2);
                int length = occupied2.length;
                int i2 = 0;
                while (i2 < length) {
                    Point sq = occupied2[i2];
                    this.mFreeList.add(sq);
                    this.mCells[(sq.y * this.mColumns) + sq.x] = null;
                    i2++;
                    rnd = rnd;
                    Point point2 = pt;
                }
                float rnd2 = rnd;
                if (squatter2 != view) {
                    obj = null;
                    squatter2.setTag(33554433, null);
                    if (animate) {
                        squatter2.animate().withLayer().scaleX(0.5f).scaleY(0.5f).alpha(0.0f).setDuration(500).setInterpolator(new AccelerateInterpolator()).setListener(new Animator.AnimatorListener() {
                            public void onAnimationStart(Animator animator) {
                            }

                            public void onAnimationEnd(Animator animator) {
                                DessertCaseView.this.removeView(squatter2);
                            }

                            public void onAnimationCancel(Animator animator) {
                            }

                            public void onAnimationRepeat(Animator animator) {
                            }
                        }).start();
                    } else {
                        removeView(squatter2);
                    }
                } else {
                    obj = null;
                }
                Object obj2 = obj;
                rnd = rnd2;
                Point point3 = pt;
            }
            for (Point oc3 : occupied) {
                this.mCells[(oc3.y * this.mColumns) + oc3.x] = view;
                this.mFreeList.remove(oc3);
            }
            float rot = ((float) irand(0, 4)) * 90.0f;
            if (animate) {
                v.bringToFront();
                AnimatorSet set1 = new AnimatorSet();
                set1.playTogether(new Animator[]{ObjectAnimator.ofFloat(view, View.SCALE_X, new float[]{(float) scale}), ObjectAnimator.ofFloat(view, View.SCALE_Y, new float[]{(float) scale})});
                set1.setInterpolator(new AnticipateOvershootInterpolator());
                set1.setDuration(500);
                AnimatorSet set2 = new AnimatorSet();
                set2.playTogether(new Animator[]{ObjectAnimator.ofFloat(view, View.ROTATION, new float[]{rot}), ObjectAnimator.ofFloat(view, View.X, new float[]{(float) ((this.mCellSize * i) + (((scale - 1) * this.mCellSize) / 2))}), ObjectAnimator.ofFloat(view, View.Y, new float[]{(float) ((this.mCellSize * j) + (((scale - 1) * this.mCellSize) / 2))})});
                set2.setInterpolator(new DecelerateInterpolator());
                set2.setDuration(500);
                set1.addListener(makeHardwareLayerListener(v));
                set1.start();
                set2.start();
            } else {
                view.setX((float) ((this.mCellSize * i) + (((scale - 1) * this.mCellSize) / 2)));
                view.setY((float) ((this.mCellSize * j) + (((scale - 1) * this.mCellSize) / 2)));
                view.setScaleX((float) scale);
                view.setScaleY((float) scale);
                view.setRotation(rot);
            }
        }
    }

    private Point[] getOccupied(View v) {
        int scale = ((Integer) v.getTag(33554434)).intValue();
        Point pt = (Point) v.getTag(33554433);
        if (pt == null || scale == 0) {
            return new Point[0];
        }
        Point[] result = new Point[(scale * scale)];
        int p = 0;
        int i = 0;
        while (i < scale) {
            int p2 = p;
            int j = 0;
            while (j < scale) {
                result[p2] = new Point(pt.x + i, pt.y + j);
                j++;
                p2++;
            }
            i++;
            p = p2;
        }
        return result;
    }

    static float frand() {
        return (float) Math.random();
    }

    static float frand(float a, float b) {
        return (frand() * (b - a)) + a;
    }

    static int irand(int a, int b) {
        return (int) frand((float) a, (float) b);
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
    }
}
