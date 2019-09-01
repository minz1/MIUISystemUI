package com.android.systemui.egg;

import android.animation.LayoutTransition;
import android.animation.TimeAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.Iterator;

public class MLand extends FrameLayout {
    static final int[] ANTENNAE = {R.drawable.mm_antennae, R.drawable.mm_antennae2};
    static final int[] CACTI = {R.drawable.cactus1, R.drawable.cactus2, R.drawable.cactus3};
    public static final boolean DEBUG = Log.isLoggable("MLand", 3);
    public static final boolean DEBUG_IDDQD = Log.isLoggable("MLand.iddqd", 3);
    static final int[] EYES = {R.drawable.mm_eyes, R.drawable.mm_eyes2};
    static final int[] MOUNTAINS = {R.drawable.mountain1, R.drawable.mountain2, R.drawable.mountain3};
    static final int[] MOUTHS = {R.drawable.mm_mouth1, R.drawable.mm_mouth2, R.drawable.mm_mouth3, R.drawable.mm_mouth4};
    /* access modifiers changed from: private */
    public static Params PARAMS;
    private static final int[][] SKIES = {new int[]{-4144897, -6250241}, new int[]{-16777200, -16777216}, new int[]{-16777152, -16777200}, new int[]{-6258656, -14663552}};
    private static float dp = 1.0f;
    static final float[] hsv = {0.0f, 0.0f, 0.0f};
    static final Rect sTmpRect = new Rect();
    private float dt;
    private TimeAnimator mAnim;
    private boolean mAnimating;
    private final AudioAttributes mAudioAttrs;
    private AudioManager mAudioManager;
    /* access modifiers changed from: private */
    public int mCountdown;
    /* access modifiers changed from: private */
    public int mCurrentPipeId;
    private boolean mFlipped;
    /* access modifiers changed from: private */
    public boolean mFrozen;
    private ArrayList<Integer> mGameControllers;
    /* access modifiers changed from: private */
    public int mHeight;
    private float mLastPipeTime;
    private ArrayList<Obstacle> mObstaclesInPlay;
    private Paint mPlayerTracePaint;
    private ArrayList<Player> mPlayers;
    private boolean mPlaying;
    private int mScene;
    private ViewGroup mScoreFields;
    /* access modifiers changed from: private */
    public View mSplash;
    private int mTaps;
    private int mTimeOfDay;
    private Paint mTouchPaint;
    private Vibrator mVibrator;
    private int mWidth;
    private float t;

    private class Building extends Scenery {
        public Building(Context context) {
            super(context);
            this.w = MLand.irand(MLand.PARAMS.BUILDING_WIDTH_MIN, MLand.PARAMS.BUILDING_WIDTH_MAX);
            this.h = 0;
        }
    }

    private class Cactus extends Building {
        public Cactus(Context context) {
            super(context);
            setBackgroundResource(MLand.pick(MLand.CACTI));
            int irand = MLand.irand(MLand.PARAMS.BUILDING_WIDTH_MAX / 4, MLand.PARAMS.BUILDING_WIDTH_MAX / 2);
            this.h = irand;
            this.w = irand;
        }
    }

    private class Cloud extends Scenery {
        public Cloud(Context context) {
            super(context);
            setBackgroundResource(MLand.frand() < 0.01f ? R.drawable.cloud_off : R.drawable.cloud);
            getBackground().setAlpha(64);
            int irand = MLand.irand(MLand.PARAMS.CLOUD_SIZE_MIN, MLand.PARAMS.CLOUD_SIZE_MAX);
            this.h = irand;
            this.w = irand;
            this.z = 0.0f;
            this.v = MLand.frand(0.15f, 0.5f);
        }
    }

    private interface GameView {
        void step(long j, long j2, float f, float f2);
    }

    private class Mountain extends Building {
        public Mountain(Context context) {
            super(context);
            setBackgroundResource(MLand.pick(MLand.MOUNTAINS));
            int irand = MLand.irand(MLand.PARAMS.BUILDING_WIDTH_MAX / 2, MLand.PARAMS.BUILDING_WIDTH_MAX);
            this.h = irand;
            this.w = irand;
            this.z = 0.0f;
        }
    }

    private class Obstacle extends View implements GameView {
        public float h;
        public final Rect hitRect = new Rect();

        public Obstacle(Context context, float h2) {
            super(context);
            setBackgroundColor(-65536);
            this.h = h2;
        }

        public boolean intersects(Player p) {
            int N = p.corners.length / 2;
            for (int i = 0; i < N; i++) {
                if (this.hitRect.contains((int) p.corners[i * 2], (int) p.corners[(i * 2) + 1])) {
                    return true;
                }
            }
            return false;
        }

        public boolean cleared(Player p) {
            int N = p.corners.length / 2;
            for (int i = 0; i < N; i++) {
                if (this.hitRect.right >= ((int) p.corners[i * 2])) {
                    return false;
                }
            }
            return true;
        }

        public void step(long t_ms, long dt_ms, float t, float dt) {
            setTranslationX(getTranslationX() - (MLand.PARAMS.TRANSLATION_PER_SEC * dt));
            getHitRect(this.hitRect);
        }
    }

    private static class Params {
        public int BOOST_DV;
        public int BUILDING_HEIGHT_MIN;
        public int BUILDING_WIDTH_MAX;
        public int BUILDING_WIDTH_MIN;
        public int CLOUD_SIZE_MAX;
        public int CLOUD_SIZE_MIN;
        public int G;
        public float HUD_Z;
        public int MAX_V;
        public int OBSTACLE_GAP;
        public int OBSTACLE_MIN;
        public int OBSTACLE_PERIOD = ((int) (((float) this.OBSTACLE_SPACING) / this.TRANSLATION_PER_SEC));
        public int OBSTACLE_SPACING;
        public int OBSTACLE_STEM_WIDTH;
        public int OBSTACLE_WIDTH;
        public float OBSTACLE_Z;
        public int PLAYER_HIT_SIZE;
        public int PLAYER_SIZE;
        public float PLAYER_Z;
        public float PLAYER_Z_BOOST;
        public float SCENERY_Z;
        public int STAR_SIZE_MAX;
        public int STAR_SIZE_MIN;
        public float TRANSLATION_PER_SEC;

        public Params(Resources res) {
            this.TRANSLATION_PER_SEC = res.getDimension(R.dimen.translation_per_sec);
            this.OBSTACLE_SPACING = res.getDimensionPixelSize(R.dimen.obstacle_spacing);
            this.BOOST_DV = res.getDimensionPixelSize(R.dimen.boost_dv);
            this.PLAYER_HIT_SIZE = res.getDimensionPixelSize(R.dimen.player_hit_size);
            this.PLAYER_SIZE = res.getDimensionPixelSize(R.dimen.player_size);
            this.OBSTACLE_WIDTH = res.getDimensionPixelSize(R.dimen.obstacle_width);
            this.OBSTACLE_STEM_WIDTH = res.getDimensionPixelSize(R.dimen.obstacle_stem_width);
            this.OBSTACLE_GAP = res.getDimensionPixelSize(R.dimen.obstacle_gap);
            this.OBSTACLE_MIN = res.getDimensionPixelSize(R.dimen.obstacle_height_min);
            this.BUILDING_HEIGHT_MIN = res.getDimensionPixelSize(R.dimen.building_height_min);
            this.BUILDING_WIDTH_MIN = res.getDimensionPixelSize(R.dimen.building_width_min);
            this.BUILDING_WIDTH_MAX = res.getDimensionPixelSize(R.dimen.building_width_max);
            this.CLOUD_SIZE_MIN = res.getDimensionPixelSize(R.dimen.cloud_size_min);
            this.CLOUD_SIZE_MAX = res.getDimensionPixelSize(R.dimen.cloud_size_max);
            this.STAR_SIZE_MIN = res.getDimensionPixelSize(R.dimen.star_size_min);
            this.STAR_SIZE_MAX = res.getDimensionPixelSize(R.dimen.star_size_max);
            this.G = res.getDimensionPixelSize(R.dimen.G);
            this.MAX_V = res.getDimensionPixelSize(R.dimen.max_v);
            this.SCENERY_Z = (float) res.getDimensionPixelSize(R.dimen.scenery_z);
            this.OBSTACLE_Z = (float) res.getDimensionPixelSize(R.dimen.obstacle_z);
            this.PLAYER_Z = (float) res.getDimensionPixelSize(R.dimen.player_z);
            this.PLAYER_Z_BOOST = (float) res.getDimensionPixelSize(R.dimen.player_z_boost);
            this.HUD_Z = (float) res.getDimensionPixelSize(R.dimen.hud_z);
            if (this.OBSTACLE_MIN <= this.OBSTACLE_WIDTH / 2) {
                MLand.L("error: obstacles might be too short, adjusting", new Object[0]);
                this.OBSTACLE_MIN = (this.OBSTACLE_WIDTH / 2) + 1;
            }
        }
    }

    private static class Player extends ImageView implements GameView {
        static int sNextColor = 0;
        public int color;
        public final float[] corners = new float[this.sHull.length];
        public float dv;
        /* access modifiers changed from: private */
        public boolean mAlive;
        private boolean mBoosting;
        private MLand mLand;
        /* access modifiers changed from: private */
        public int mScore;
        /* access modifiers changed from: private */
        public TextView mScoreField;
        /* access modifiers changed from: private */
        public float mTouchX = -1.0f;
        /* access modifiers changed from: private */
        public float mTouchY = -1.0f;
        private final int[] sColors = {-2407369, -12879641, -740352, -15753896, -8710016, -6381922};
        private final float[] sHull = {0.3f, 0.0f, 0.7f, 0.0f, 0.92f, 0.33f, 0.92f, 0.75f, 0.6f, 1.0f, 0.4f, 1.0f, 0.08f, 0.75f, 0.08f, 0.33f};

        public static Player create(MLand land) {
            Player p = new Player(land.getContext());
            p.mLand = land;
            p.reset();
            p.setVisibility(4);
            land.addView(p, new FrameLayout.LayoutParams(MLand.PARAMS.PLAYER_SIZE, MLand.PARAMS.PLAYER_SIZE));
            return p;
        }

        private void setScore(int score) {
            this.mScore = score;
            if (this.mScoreField != null) {
                this.mScoreField.setText(MLand.DEBUG_IDDQD ? "??" : String.valueOf(score));
            }
        }

        public int getScore() {
            return this.mScore;
        }

        /* access modifiers changed from: private */
        public void addScore(int incr) {
            setScore(this.mScore + incr);
        }

        public void setScoreField(TextView tv) {
            this.mScoreField = tv;
            if (tv != null) {
                setScore(this.mScore);
                this.mScoreField.getBackground().setColorFilter(this.color, PorterDuff.Mode.SRC_ATOP);
                this.mScoreField.setTextColor(MLand.luma(this.color) > 0.7f ? -16777216 : -1);
            }
        }

        public void reset() {
            setY((float) (((this.mLand.mHeight / 2) + ((int) (Math.random() * ((double) MLand.PARAMS.PLAYER_SIZE)))) - (MLand.PARAMS.PLAYER_SIZE / 2)));
            setScore(0);
            setScoreField(this.mScoreField);
            this.mBoosting = false;
            this.dv = 0.0f;
        }

        public Player(Context context) {
            super(context);
            setBackgroundResource(R.drawable.f2android);
            getBackground().setTintMode(PorterDuff.Mode.SRC_ATOP);
            int[] iArr = this.sColors;
            int i = sNextColor;
            sNextColor = i + 1;
            this.color = iArr[i % this.sColors.length];
            getBackground().setTint(this.color);
            setOutlineProvider(new ViewOutlineProvider() {
                public void getOutline(View view, Outline outline) {
                    int w = view.getWidth();
                    int h = view.getHeight();
                    int ix = (int) (((float) w) * 0.3f);
                    int iy = (int) (((float) h) * 0.2f);
                    outline.setRect(ix, iy, w - ix, h - iy);
                }
            });
        }

        public void prepareCheckIntersections() {
            int inset = (MLand.PARAMS.PLAYER_SIZE - MLand.PARAMS.PLAYER_HIT_SIZE) / 2;
            int scale = MLand.PARAMS.PLAYER_HIT_SIZE;
            int N = this.sHull.length / 2;
            for (int i = 0; i < N; i++) {
                this.corners[i * 2] = (((float) scale) * this.sHull[i * 2]) + ((float) inset);
                this.corners[(i * 2) + 1] = (((float) scale) * this.sHull[(i * 2) + 1]) + ((float) inset);
            }
            getMatrix().mapPoints(this.corners);
        }

        public boolean below(int h) {
            int N = this.corners.length / 2;
            for (int i = 0; i < N; i++) {
                if (((int) this.corners[(i * 2) + 1]) >= h) {
                    return true;
                }
            }
            return false;
        }

        public void step(long t_ms, long dt_ms, float t, float dt) {
            if (!this.mAlive) {
                setTranslationX(getTranslationX() - (MLand.PARAMS.TRANSLATION_PER_SEC * dt));
                return;
            }
            if (this.mBoosting) {
                this.dv = (float) (-MLand.PARAMS.BOOST_DV);
            } else {
                this.dv += (float) MLand.PARAMS.G;
            }
            if (this.dv < ((float) (-MLand.PARAMS.MAX_V))) {
                this.dv = (float) (-MLand.PARAMS.MAX_V);
            } else if (this.dv > ((float) MLand.PARAMS.MAX_V)) {
                this.dv = (float) MLand.PARAMS.MAX_V;
            }
            float y = getTranslationY() + (this.dv * dt);
            float f = 0.0f;
            if (y >= 0.0f) {
                f = y;
            }
            setTranslationY(f);
            setRotation(90.0f + MLand.lerp(MLand.clamp(MLand.rlerp(this.dv, (float) MLand.PARAMS.MAX_V, (float) (-1 * MLand.PARAMS.MAX_V))), 90.0f, -90.0f));
            prepareCheckIntersections();
        }

        public void boost(float x, float y) {
            this.mTouchX = x;
            this.mTouchY = y;
            boost();
        }

        public void boost() {
            this.mBoosting = true;
            this.dv = (float) (-MLand.PARAMS.BOOST_DV);
            animate().cancel();
            animate().scaleX(1.25f).scaleY(1.25f).translationZ(MLand.PARAMS.PLAYER_Z_BOOST).setDuration(100);
            setScaleX(1.25f);
            setScaleY(1.25f);
        }

        public void unboost() {
            this.mBoosting = false;
            this.mTouchY = -1.0f;
            this.mTouchX = -1.0f;
            animate().cancel();
            animate().scaleX(1.0f).scaleY(1.0f).translationZ(MLand.PARAMS.PLAYER_Z).setDuration(200);
        }

        public void die() {
            this.mAlive = false;
            TextView textView = this.mScoreField;
        }

        public void start() {
            this.mAlive = true;
        }
    }

    private class Pop extends Obstacle {
        Drawable antenna;
        int cx;
        int cy;
        Drawable eyes;
        int mRotate;
        Drawable mouth;
        int r;

        public Pop(Context context, float h) {
            super(context, h);
            setBackgroundResource(R.drawable.mm_head);
            this.antenna = context.getDrawable(MLand.pick(MLand.ANTENNAE));
            if (MLand.frand() > 0.5f) {
                this.eyes = context.getDrawable(MLand.pick(MLand.EYES));
                if (MLand.frand() > 0.8f) {
                    this.mouth = context.getDrawable(MLand.pick(MLand.MOUTHS));
                }
            }
            setOutlineProvider(new ViewOutlineProvider(MLand.this) {
                public void getOutline(View view, Outline outline) {
                    int pad = (int) ((((float) Pop.this.getWidth()) * 1.0f) / 6.0f);
                    outline.setOval(pad, pad, Pop.this.getWidth() - pad, Pop.this.getHeight() - pad);
                }
            });
        }

        public boolean intersects(Player p) {
            int N = p.corners.length / 2;
            for (int i = 0; i < N; i++) {
                if (Math.hypot((double) (((int) p.corners[i * 2]) - this.cx), (double) (((int) p.corners[(i * 2) + 1]) - this.cy)) <= ((double) this.r)) {
                    return true;
                }
            }
            return false;
        }

        public void step(long t_ms, long dt_ms, float t, float dt) {
            super.step(t_ms, dt_ms, t, dt);
            if (this.mRotate != 0) {
                setRotation(getRotation() + (45.0f * dt * ((float) this.mRotate)));
            }
            this.cx = (this.hitRect.left + this.hitRect.right) / 2;
            this.cy = (this.hitRect.top + this.hitRect.bottom) / 2;
            this.r = getWidth() / 3;
        }

        public void onDraw(Canvas c) {
            super.onDraw(c);
            if (this.antenna != null) {
                this.antenna.setBounds(0, 0, c.getWidth(), c.getHeight());
                this.antenna.draw(c);
            }
            if (this.eyes != null) {
                this.eyes.setBounds(0, 0, c.getWidth(), c.getHeight());
                this.eyes.draw(c);
            }
            if (this.mouth != null) {
                this.mouth.setBounds(0, 0, c.getWidth(), c.getHeight());
                this.mouth.draw(c);
            }
        }
    }

    private class Scenery extends FrameLayout implements GameView {
        public int h;
        public float v;
        public int w;
        public float z;

        public Scenery(Context context) {
            super(context);
        }

        public void step(long t_ms, long dt_ms, float t, float dt) {
            setTranslationX(getTranslationX() - ((MLand.PARAMS.TRANSLATION_PER_SEC * dt) * this.v));
        }
    }

    private class Star extends Scenery {
        public Star(Context context) {
            super(context);
            setBackgroundResource(R.drawable.star);
            int irand = MLand.irand(MLand.PARAMS.STAR_SIZE_MIN, MLand.PARAMS.STAR_SIZE_MAX);
            this.h = irand;
            this.w = irand;
            this.z = 0.0f;
            this.v = 0.0f;
        }
    }

    private class Stem extends Obstacle {
        int id;
        boolean mDrawShadow;
        GradientDrawable mGradient = new GradientDrawable();
        Path mJandystripe;
        Paint mPaint = new Paint();
        Paint mPaint2;
        Path mShadow = new Path();

        public Stem(Context context, float h, boolean drawShadow) {
            super(context, h);
            this.id = MLand.this.mCurrentPipeId;
            this.mDrawShadow = drawShadow;
            setBackground(null);
            this.mGradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
            this.mPaint.setColor(-16777216);
            this.mPaint.setColorFilter(new PorterDuffColorFilter(570425344, PorterDuff.Mode.MULTIPLY));
            if (MLand.frand() < 0.01f) {
                this.mGradient.setColors(new int[]{-1, -2236963});
                this.mJandystripe = new Path();
                this.mPaint2 = new Paint();
                this.mPaint2.setColor(-65536);
                this.mPaint2.setColorFilter(new PorterDuffColorFilter(-65536, PorterDuff.Mode.MULTIPLY));
                return;
            }
            this.mGradient.setColors(new int[]{-4412764, -6190977});
        }

        public void onAttachedToWindow() {
            super.onAttachedToWindow();
            setWillNotDraw(false);
            setOutlineProvider(new ViewOutlineProvider() {
                public void getOutline(View view, Outline outline) {
                    outline.setRect(0, 0, Stem.this.getWidth(), Stem.this.getHeight());
                }
            });
        }

        public void onDraw(Canvas c) {
            int w = c.getWidth();
            int h = c.getHeight();
            this.mGradient.setGradientCenter(((float) w) * 0.75f, 0.0f);
            int y = 0;
            this.mGradient.setBounds(0, 0, w, h);
            this.mGradient.draw(c);
            if (this.mJandystripe != null) {
                this.mJandystripe.reset();
                this.mJandystripe.moveTo(0.0f, (float) w);
                this.mJandystripe.lineTo((float) w, 0.0f);
                this.mJandystripe.lineTo((float) w, (float) (2 * w));
                this.mJandystripe.lineTo(0.0f, (float) (3 * w));
                this.mJandystripe.close();
                while (true) {
                    int y2 = y;
                    if (y2 >= h) {
                        break;
                    }
                    c.drawPath(this.mJandystripe, this.mPaint2);
                    this.mJandystripe.offset(0.0f, (float) (4 * w));
                    y = y2 + (4 * w);
                }
            }
            if (this.mDrawShadow) {
                this.mShadow.reset();
                this.mShadow.moveTo(0.0f, 0.0f);
                this.mShadow.lineTo((float) w, 0.0f);
                this.mShadow.lineTo((float) w, (((float) MLand.PARAMS.OBSTACLE_WIDTH) * 0.4f) + (((float) w) * 1.5f));
                this.mShadow.lineTo(0.0f, ((float) MLand.PARAMS.OBSTACLE_WIDTH) * 0.4f);
                this.mShadow.close();
                c.drawPath(this.mShadow, this.mPaint);
            }
        }
    }

    static /* synthetic */ int access$210(MLand x0) {
        int i = x0.mCountdown;
        x0.mCountdown = i - 1;
        return i;
    }

    public static void L(String s, Object... objects) {
        if (DEBUG) {
            Log.d("MLand", objects.length == 0 ? s : String.format(s, objects));
        }
    }

    public MLand(Context context) {
        this(context, null);
    }

    public MLand(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MLand(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAudioAttrs = new AudioAttributes.Builder().setUsage(14).build();
        this.mPlayers = new ArrayList<>();
        this.mObstaclesInPlay = new ArrayList<>();
        this.mCountdown = 0;
        this.mGameControllers = new ArrayList<>();
        this.mVibrator = (Vibrator) context.getSystemService("vibrator");
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        setFocusable(true);
        PARAMS = new Params(getResources());
        this.mTimeOfDay = irand(0, SKIES.length - 1);
        this.mScene = irand(0, 3);
        this.mTouchPaint = new Paint(1);
        this.mTouchPaint.setColor(-2130706433);
        this.mTouchPaint.setStyle(Paint.Style.FILL);
        this.mPlayerTracePaint = new Paint(1);
        this.mPlayerTracePaint.setColor(-2130706433);
        this.mPlayerTracePaint.setStyle(Paint.Style.STROKE);
        this.mPlayerTracePaint.setStrokeWidth(2.0f * dp);
        setLayoutDirection(0);
        setupPlayers(1);
        MetricsLogger.count(getContext(), "egg_mland_create", 1);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        dp = getResources().getDisplayMetrics().density;
        reset();
        start(false);
    }

    public boolean willNotDraw() {
        return !DEBUG;
    }

    public float getGameTime() {
        return this.t;
    }

    public void setScoreFieldHolder(ViewGroup vg) {
        this.mScoreFields = vg;
        if (vg != null) {
            LayoutTransition lt = new LayoutTransition();
            lt.setDuration(250);
            this.mScoreFields.setLayoutTransition(lt);
        }
        Iterator<Player> it = this.mPlayers.iterator();
        while (it.hasNext()) {
            this.mScoreFields.addView(it.next().mScoreField, new ViewGroup.MarginLayoutParams(-2, -1));
        }
    }

    public void setSplash(View v) {
        this.mSplash = v;
    }

    public static boolean isGamePad(InputDevice dev) {
        int sources = dev.getSources();
        return (sources & 1025) == 1025 || (sources & 16777232) == 16777232;
    }

    public ArrayList getGameControllers() {
        this.mGameControllers.clear();
        for (int deviceId : InputDevice.getDeviceIds()) {
            if (isGamePad(InputDevice.getDevice(deviceId)) && !this.mGameControllers.contains(Integer.valueOf(deviceId))) {
                this.mGameControllers.add(Integer.valueOf(deviceId));
            }
        }
        return this.mGameControllers;
    }

    public int getControllerPlayer(int id) {
        int player = this.mGameControllers.indexOf(Integer.valueOf(id));
        if (player < 0 || player >= this.mPlayers.size()) {
            return 0;
        }
        return player;
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        dp = getResources().getDisplayMetrics().density;
        stop();
        reset();
        start(false);
    }

    /* access modifiers changed from: private */
    public static float luma(int bgcolor) {
        return ((0.2126f * ((float) (16711680 & bgcolor))) / 1.671168E7f) + ((0.7152f * ((float) (65280 & bgcolor))) / 65280.0f) + ((0.0722f * ((float) (bgcolor & 255))) / 255.0f);
    }

    public Player getPlayer(int i) {
        if (i < this.mPlayers.size()) {
            return this.mPlayers.get(i);
        }
        return null;
    }

    private int addPlayerInternal(Player p) {
        this.mPlayers.add(p);
        realignPlayers();
        TextView scoreField = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.mland_scorefield, null);
        if (this.mScoreFields != null) {
            this.mScoreFields.addView(scoreField, new ViewGroup.MarginLayoutParams(-2, -1));
        }
        p.setScoreField(scoreField);
        return this.mPlayers.size() - 1;
    }

    private void removePlayerInternal(Player p) {
        if (this.mPlayers.remove(p)) {
            removeView(p);
            this.mScoreFields.removeView(p.mScoreField);
            realignPlayers();
        }
    }

    private void realignPlayers() {
        int N = this.mPlayers.size();
        float x = (float) ((this.mWidth - ((N - 1) * PARAMS.PLAYER_SIZE)) / 2);
        for (int i = 0; i < N; i++) {
            this.mPlayers.get(i).setX(x);
            x += (float) PARAMS.PLAYER_SIZE;
        }
    }

    private void clearPlayers() {
        while (this.mPlayers.size() > 0) {
            removePlayerInternal(this.mPlayers.get(0));
        }
    }

    public void setupPlayers(int num) {
        clearPlayers();
        for (int i = 0; i < num; i++) {
            addPlayerInternal(Player.create(this));
        }
    }

    public void addPlayer() {
        if (getNumPlayers() != 6) {
            addPlayerInternal(Player.create(this));
        }
    }

    public int getNumPlayers() {
        return this.mPlayers.size();
    }

    public void removePlayer() {
        if (getNumPlayers() != 1) {
            removePlayerInternal(this.mPlayers.get(this.mPlayers.size() - 1));
        }
    }

    private void thump(int playerIndex, long ms) {
        if (this.mAudioManager.getRingerMode() != 0) {
            if (playerIndex < this.mGameControllers.size()) {
                InputDevice dev = InputDevice.getDevice(this.mGameControllers.get(playerIndex).intValue());
                if (dev != null && dev.getVibrator().hasVibrator()) {
                    dev.getVibrator().vibrate((long) (((float) ms) * 2.0f), this.mAudioAttrs);
                    return;
                }
            }
            this.mVibrator.vibrate(ms, this.mAudioAttrs);
        }
    }

    public void reset() {
        Scenery s;
        L("reset", new Object[0]);
        Drawable sky = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, SKIES[this.mTimeOfDay]);
        boolean cloudless = true;
        sky.setDither(true);
        setBackground(sky);
        this.mFlipped = frand() > 0.5f;
        setScaleX(this.mFlipped ? -1.0f : 1.0f);
        int i = getChildCount();
        while (true) {
            int i2 = i - 1;
            if (i <= 0) {
                break;
            }
            if (getChildAt(i2) instanceof GameView) {
                removeViewAt(i2);
            }
            i = i2;
        }
        this.mObstaclesInPlay.clear();
        this.mCurrentPipeId = 0;
        this.mWidth = getWidth();
        this.mHeight = getHeight();
        boolean showingSun = (this.mTimeOfDay == 0 || this.mTimeOfDay == 3) && ((double) frand()) > 0.25d;
        if (showingSun) {
            Star sun = new Star(getContext());
            sun.setBackgroundResource(R.drawable.sun);
            int w = getResources().getDimensionPixelSize(R.dimen.sun_size);
            sun.setTranslationX(frand((float) w, (float) (this.mWidth - w)));
            if (this.mTimeOfDay == 0) {
                sun.setTranslationY(frand((float) w, ((float) this.mHeight) * 0.66f));
                sun.getBackground().setTint(0);
            } else {
                sun.setTranslationY(frand(((float) this.mHeight) * 0.66f, (float) (this.mHeight - w)));
                sun.getBackground().setTintMode(PorterDuff.Mode.SRC_ATOP);
                sun.getBackground().setTint(-1056997376);
            }
            addView(sun, new FrameLayout.LayoutParams(w, w));
        }
        if (!showingSun) {
            boolean dark = this.mTimeOfDay == 1 || this.mTimeOfDay == 2;
            float ff = frand();
            if ((dark && ff < 0.75f) || ff < 0.5f) {
                Star moon = new Star(getContext());
                moon.setBackgroundResource(R.drawable.moon);
                moon.getBackground().setAlpha(dark ? 255 : 128);
                moon.setScaleX(((double) frand()) > 0.5d ? -1.0f : 1.0f);
                moon.setRotation(moon.getScaleX() * frand(5.0f, 30.0f));
                int w2 = getResources().getDimensionPixelSize(R.dimen.sun_size);
                moon.setTranslationX(frand((float) w2, (float) (this.mWidth - w2)));
                moon.setTranslationY(frand((float) w2, (float) (this.mHeight - w2)));
                addView(moon, new FrameLayout.LayoutParams(w2, w2));
            }
        }
        int mh = this.mHeight / 6;
        if (((double) frand()) >= 0.25d) {
            cloudless = false;
        }
        for (int i3 = 0; i3 < 20; i3++) {
            float r1 = frand();
            if (((double) r1) < 0.3d && this.mTimeOfDay != 0) {
                s = new Star(getContext());
            } else if (((double) r1) >= 0.6d || cloudless) {
                switch (this.mScene) {
                    case 1:
                        s = new Cactus(getContext());
                        break;
                    case 2:
                        s = new Mountain(getContext());
                        break;
                    default:
                        s = new Building(getContext());
                        break;
                }
                s.z = ((float) i3) / 20.0f;
                s.v = 0.85f * s.z;
                if (this.mScene == 0) {
                    s.setBackgroundColor(-7829368);
                    s.h = irand(PARAMS.BUILDING_HEIGHT_MIN, mh);
                }
                int c = (int) (255.0f * s.z);
                Drawable bg = s.getBackground();
                if (bg != null) {
                    bg.setColorFilter(Color.rgb(c, c, c), PorterDuff.Mode.MULTIPLY);
                }
            } else {
                s = new Cloud(getContext());
            }
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(s.w, s.h);
            if (s instanceof Building) {
                lp.gravity = 80;
            } else {
                lp.gravity = 48;
                float r = frand();
                if (s instanceof Star) {
                    lp.topMargin = (int) (r * r * ((float) this.mHeight));
                } else {
                    lp.topMargin = ((int) (1.0f - (((r * r) * ((float) this.mHeight)) / 2.0f))) + (this.mHeight / 2);
                }
            }
            addView(s, lp);
            s.setTranslationX(frand((float) (-lp.width), (float) (this.mWidth + lp.width)));
        }
        Iterator<Player> it = this.mPlayers.iterator();
        while (it.hasNext()) {
            Player p = it.next();
            addView(p);
            p.reset();
        }
        realignPlayers();
        if (this.mAnim != null) {
            this.mAnim.cancel();
        }
        this.mAnim = new TimeAnimator();
        this.mAnim.setTimeListener(new TimeAnimator.TimeListener() {
            public void onTimeUpdate(TimeAnimator timeAnimator, long t, long dt) {
                MLand.this.step(t, dt);
            }
        });
    }

    public void start(boolean startPlaying) {
        Object[] objArr = new Object[1];
        objArr[0] = startPlaying ? "true" : "false";
        L("start(startPlaying=%s)", objArr);
        if (startPlaying && this.mCountdown <= 0) {
            showSplash();
            this.mSplash.findViewById(R.id.play_button).setEnabled(false);
            View playImage = this.mSplash.findViewById(R.id.play_button_image);
            final TextView playText = (TextView) this.mSplash.findViewById(R.id.play_button_text);
            playImage.animate().alpha(0.0f);
            playText.animate().alpha(1.0f);
            this.mCountdown = 3;
            post(new Runnable() {
                public void run() {
                    if (MLand.this.mCountdown == 0) {
                        MLand.this.startPlaying();
                    } else {
                        MLand.this.postDelayed(this, 500);
                    }
                    playText.setText(String.valueOf(MLand.this.mCountdown));
                    MLand.access$210(MLand.this);
                }
            });
        }
        Iterator<Player> it = this.mPlayers.iterator();
        while (it.hasNext()) {
            it.next().setVisibility(4);
        }
        if (!this.mAnimating) {
            this.mAnim.start();
            this.mAnimating = true;
        }
    }

    public void hideSplash() {
        if (this.mSplash != null && this.mSplash.getVisibility() == 0) {
            this.mSplash.setClickable(false);
            this.mSplash.animate().alpha(0.0f).translationZ(0.0f).setDuration(300).withEndAction(new Runnable() {
                public void run() {
                    MLand.this.mSplash.setVisibility(8);
                }
            });
        }
    }

    public void showSplash() {
        if (this.mSplash != null && this.mSplash.getVisibility() != 0) {
            this.mSplash.setClickable(true);
            this.mSplash.setAlpha(0.0f);
            this.mSplash.setVisibility(0);
            this.mSplash.animate().alpha(1.0f).setDuration(1000);
            this.mSplash.findViewById(R.id.play_button_image).setAlpha(1.0f);
            this.mSplash.findViewById(R.id.play_button_text).setAlpha(0.0f);
            this.mSplash.findViewById(R.id.play_button).setEnabled(true);
            this.mSplash.findViewById(R.id.play_button).requestFocus();
        }
    }

    public void startPlaying() {
        this.mPlaying = true;
        this.t = 0.0f;
        this.mLastPipeTime = getGameTime() - ((float) PARAMS.OBSTACLE_PERIOD);
        hideSplash();
        realignPlayers();
        this.mTaps = 0;
        int N = this.mPlayers.size();
        MetricsLogger.histogram(getContext(), "egg_mland_players", N);
        for (int i = 0; i < N; i++) {
            Player p = this.mPlayers.get(i);
            p.setVisibility(0);
            p.reset();
            p.start();
            p.boost(-1.0f, -1.0f);
            p.unboost();
        }
    }

    public void stop() {
        if (this.mAnimating) {
            this.mAnim.cancel();
            this.mAnim = null;
            this.mAnimating = false;
            this.mPlaying = false;
            this.mTimeOfDay = irand(0, SKIES.length - 1);
            this.mScene = irand(0, 3);
            this.mFrozen = true;
            Iterator<Player> it = this.mPlayers.iterator();
            while (it.hasNext()) {
                it.next().die();
            }
            postDelayed(new Runnable() {
                public void run() {
                    boolean unused = MLand.this.mFrozen = false;
                }
            }, 250);
        }
    }

    public static final float lerp(float x, float a, float b) {
        return ((b - a) * x) + a;
    }

    public static final float rlerp(float v, float a, float b) {
        return (v - a) / (b - a);
    }

    public static final float clamp(float f) {
        if (f < 0.0f) {
            return 0.0f;
        }
        if (f > 1.0f) {
            return 1.0f;
        }
        return f;
    }

    public static final float frand() {
        return (float) Math.random();
    }

    public static final float frand(float a, float b) {
        return lerp(frand(), a, b);
    }

    public static final int irand(int a, int b) {
        return Math.round(frand((float) a, (float) b));
    }

    public static int pick(int[] l) {
        return l[irand(0, l.length - 1)];
    }

    /* access modifiers changed from: private */
    public void step(long t_ms, long dt_ms) {
        int i;
        int livingPlayers;
        long j = t_ms;
        this.t = ((float) j) / 1000.0f;
        long j2 = dt_ms;
        this.dt = ((float) j2) / 1000.0f;
        if (DEBUG) {
            this.t *= 0.5f;
            this.dt *= 0.5f;
        }
        int N = getChildCount();
        int i2 = 0;
        while (true) {
            i = i2;
            if (i >= N) {
                break;
            }
            View v = getChildAt(i);
            if (v instanceof GameView) {
                ((GameView) v).step(j, j2, this.t, this.dt);
            }
            i2 = i + 1;
        }
        int i3 = 1;
        if (this.mPlaying != 0) {
            int livingPlayers2 = 0;
            i = 0;
            while (i < this.mPlayers.size()) {
                Player p = getPlayer(i);
                if (p.mAlive) {
                    if (p.below(this.mHeight)) {
                        if (DEBUG_IDDQD) {
                            poke(i);
                            unpoke(i);
                        } else {
                            Object[] objArr = new Object[i3];
                            objArr[0] = Integer.valueOf(i);
                            L("player %d hit the floor", objArr);
                            thump(i, 80);
                            p.die();
                        }
                    }
                    int maxPassedStem = 0;
                    int j3 = this.mObstaclesInPlay.size();
                    while (true) {
                        int j4 = j3 - 1;
                        if (j3 <= 0) {
                            break;
                        }
                        Obstacle ob = this.mObstaclesInPlay.get(j4);
                        if (ob.intersects(p) && !DEBUG_IDDQD) {
                            L("player hit an obstacle", new Object[0]);
                            thump(i, 80);
                            p.die();
                        } else if (ob.cleared(p) && (ob instanceof Stem)) {
                            maxPassedStem = Math.max(maxPassedStem, ((Stem) ob).id);
                        }
                        j3 = j4;
                    }
                    if (maxPassedStem > p.mScore) {
                        p.addScore(1);
                    }
                }
                if (p.mAlive) {
                    livingPlayers2++;
                }
                i++;
                i3 = 1;
            }
            if (livingPlayers2 == 0) {
                stop();
                MetricsLogger.count(getContext(), "egg_mland_taps", this.mTaps);
                this.mTaps = 0;
                int playerCount = this.mPlayers.size();
                for (int pi = 0; pi < playerCount; pi++) {
                    MetricsLogger.histogram(getContext(), "egg_mland_score", this.mPlayers.get(pi).getScore());
                }
            }
        }
        while (true) {
            livingPlayers = i - 1;
            if (i <= 0) {
                break;
            }
            View v2 = getChildAt(livingPlayers);
            if (v2 instanceof Obstacle) {
                if (v2.getTranslationX() + ((float) v2.getWidth()) < 0.0f) {
                    removeViewAt(livingPlayers);
                    this.mObstaclesInPlay.remove(v2);
                }
            } else if ((v2 instanceof Scenery) && v2.getTranslationX() + ((float) ((Scenery) v2).w) < 0.0f) {
                v2.setTranslationX((float) getWidth());
            }
            i = livingPlayers;
        }
        if (!this.mPlaying || this.t - this.mLastPipeTime <= ((float) PARAMS.OBSTACLE_PERIOD)) {
            int i4 = livingPlayers;
        } else {
            this.mLastPipeTime = this.t;
            this.mCurrentPipeId++;
            int obstacley = ((int) (frand() * ((float) ((this.mHeight - (PARAMS.OBSTACLE_MIN * 2)) - PARAMS.OBSTACLE_GAP)))) + PARAMS.OBSTACLE_MIN;
            int inset = (PARAMS.OBSTACLE_WIDTH - PARAMS.OBSTACLE_STEM_WIDTH) / 2;
            int yinset = PARAMS.OBSTACLE_WIDTH / 2;
            int d1 = irand(0, 250);
            Stem stem = new Stem(getContext(), (float) (obstacley - yinset), false);
            addView(stem, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_STEM_WIDTH, (int) stem.h, 51));
            stem.setTranslationX((float) (this.mWidth + inset));
            stem.setTranslationY((-stem.h) - ((float) yinset));
            stem.setTranslationZ(PARAMS.OBSTACLE_Z * 0.75f);
            stem.animate().translationY(0.0f).setStartDelay((long) d1).setDuration(250);
            this.mObstaclesInPlay.add(stem);
            Obstacle p1 = new Pop(getContext(), (float) PARAMS.OBSTACLE_WIDTH);
            int i5 = livingPlayers;
            addView(p1, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_WIDTH, PARAMS.OBSTACLE_WIDTH, 51));
            p1.setTranslationX((float) this.mWidth);
            p1.setTranslationY((float) (-PARAMS.OBSTACLE_WIDTH));
            p1.setTranslationZ(PARAMS.OBSTACLE_Z);
            p1.setScaleX(0.25f);
            p1.setScaleY(-0.25f);
            p1.animate().translationY(stem.h - ((float) inset)).scaleX(1.0f).scaleY(-1.0f).setStartDelay((long) d1).setDuration(250);
            this.mObstaclesInPlay.add(p1);
            int d2 = irand(0, 250);
            int i6 = obstacley;
            Obstacle s2 = new Stem(getContext(), (float) (((this.mHeight - obstacley) - PARAMS.OBSTACLE_GAP) - yinset), true);
            Stem stem2 = stem;
            addView(s2, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_STEM_WIDTH, (int) s2.h, 51));
            s2.setTranslationX((float) (this.mWidth + inset));
            s2.setTranslationY((float) (this.mHeight + yinset));
            s2.setTranslationZ(PARAMS.OBSTACLE_Z * 0.75f);
            s2.animate().translationY(((float) this.mHeight) - s2.h).setStartDelay((long) d2).setDuration(400);
            this.mObstaclesInPlay.add(s2);
            Obstacle p2 = new Pop(getContext(), (float) PARAMS.OBSTACLE_WIDTH);
            int i7 = inset;
            addView(p2, new FrameLayout.LayoutParams(PARAMS.OBSTACLE_WIDTH, PARAMS.OBSTACLE_WIDTH, 51));
            p2.setTranslationX((float) this.mWidth);
            p2.setTranslationY((float) this.mHeight);
            p2.setTranslationZ(PARAMS.OBSTACLE_Z);
            p2.setScaleX(0.25f);
            p2.setScaleY(0.25f);
            p2.animate().translationY((((float) this.mHeight) - s2.h) - ((float) yinset)).scaleX(1.0f).scaleY(1.0f).setStartDelay((long) d2).setDuration(400);
            this.mObstaclesInPlay.add(p2);
        }
        invalidate();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        L("touch: %s", ev);
        int actionIndex = ev.getActionIndex();
        float x = ev.getX(actionIndex);
        float y = ev.getY(actionIndex);
        int playerIndex = (int) (((float) getNumPlayers()) * (x / ((float) getWidth())));
        if (this.mFlipped) {
            playerIndex = (getNumPlayers() - 1) - playerIndex;
        }
        switch (ev.getActionMasked()) {
            case 0:
            case 5:
                poke(playerIndex, x, y);
                return true;
            case 1:
            case 6:
                unpoke(playerIndex);
                return true;
            default:
                return false;
        }
    }

    public boolean onTrackballEvent(MotionEvent ev) {
        L("trackball: %s", ev);
        switch (ev.getAction()) {
            case 0:
                poke(0);
                return true;
            case 1:
                unpoke(0);
                return true;
            default:
                return false;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent ev) {
        L("keyDown: %d", Integer.valueOf(keyCode));
        if (keyCode != 19 && keyCode != 23 && keyCode != 62 && keyCode != 66 && keyCode != 96) {
            return false;
        }
        poke(getControllerPlayer(ev.getDeviceId()));
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent ev) {
        L("keyDown: %d", Integer.valueOf(keyCode));
        if (keyCode != 19 && keyCode != 23 && keyCode != 62 && keyCode != 66 && keyCode != 96) {
            return false;
        }
        unpoke(getControllerPlayer(ev.getDeviceId()));
        return true;
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        L("generic: %s", ev);
        return false;
    }

    private void poke(int playerIndex) {
        poke(playerIndex, -1.0f, -1.0f);
    }

    private void poke(int playerIndex, float x, float y) {
        L("poke(%d)", Integer.valueOf(playerIndex));
        if (!this.mFrozen) {
            if (!this.mAnimating) {
                reset();
            }
            if (!this.mPlaying) {
                start(true);
            } else {
                Player p = getPlayer(playerIndex);
                if (p != null) {
                    p.boost(x, y);
                    this.mTaps++;
                    if (DEBUG) {
                        p.dv *= 0.5f;
                        p.animate().setDuration(400);
                    }
                }
            }
        }
    }

    private void unpoke(int playerIndex) {
        L("unboost(%d)", Integer.valueOf(playerIndex));
        if (!this.mFrozen && this.mAnimating && this.mPlaying) {
            Player p = getPlayer(playerIndex);
            if (p != null) {
                p.unboost();
            }
        }
    }

    public void onDraw(Canvas c) {
        super.onDraw(c);
        Iterator<Player> it = this.mPlayers.iterator();
        while (it.hasNext()) {
            Player p = it.next();
            if (p.mTouchX > 0.0f) {
                this.mTouchPaint.setColor(p.color & -2130706433);
                this.mPlayerTracePaint.setColor(p.color & -2130706433);
                float x1 = p.mTouchX;
                float y1 = p.mTouchY;
                c.drawCircle(x1, y1, 100.0f, this.mTouchPaint);
                float x2 = p.getX() + p.getPivotX();
                float y2 = p.getY() + p.getPivotY();
                float angle = 1.5707964f - ((float) Math.atan2((double) (x2 - x1), (double) (y2 - y1)));
                float x12 = (float) (((double) x1) + (Math.cos((double) angle) * 100.0d));
                c.drawLine(x12, (float) (((double) y1) + (100.0d * Math.sin((double) angle))), x2, y2, this.mPlayerTracePaint);
            }
        }
    }
}
