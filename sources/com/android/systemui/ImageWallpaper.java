package com.android.systemui;

import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.AsyncTask;
import android.renderscript.Matrix4f;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class ImageWallpaper extends WallpaperService {
    DrawableEngine mEngine;
    boolean mIsHwAccelerated;
    WallpaperManager mWallpaperManager;

    class DrawableEngine extends WallpaperService.Engine {
        Bitmap mBackground;
        int mBackgroundHeight = -1;
        int mBackgroundWidth = -1;
        private Display mDefaultDisplay;
        private int mDisplayHeightAtLastSurfaceSizeUpdate = -1;
        private int mDisplayWidthAtLastSurfaceSizeUpdate = -1;
        private EGL10 mEgl;
        private EGLConfig mEglConfig;
        private EGLContext mEglContext;
        private EGLDisplay mEglDisplay;
        private EGLSurface mEglSurface;
        private int mLastRequestedHeight = -1;
        private int mLastRequestedWidth = -1;
        int mLastRotation = -1;
        int mLastSurfaceHeight = -1;
        int mLastSurfaceWidth = -1;
        int mLastXTranslation;
        int mLastYTranslation;
        /* access modifiers changed from: private */
        public AsyncTask<Void, Void, Bitmap> mLoader;
        /* access modifiers changed from: private */
        public boolean mNeedsDrawAfterLoadingWallpaper;
        boolean mOffsetsChanged;
        private int mRotationAtLastSurfaceSizeUpdate = -1;
        float mScale = 1.0f;
        private boolean mSurfaceValid;
        private final DisplayInfo mTmpDisplayInfo = new DisplayInfo();
        boolean mVisible = true;
        float mXOffset = 0.5f;
        float mYOffset = 0.5f;

        public DrawableEngine() {
            super(ImageWallpaper.this);
            setFixedSizeAllowed(true);
        }

        public void trimMemory(int level) {
            if (level >= 10 && level <= 15 && this.mBackground != null) {
                unloadWallpaper(true);
            }
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.mDefaultDisplay = ((WindowManager) ImageWallpaper.this.getSystemService(WindowManager.class)).getDefaultDisplay();
            setOffsetNotificationsEnabled(false);
            updateSurfaceSize(surfaceHolder, getDefaultDisplayInfo(), false);
        }

        public void onDestroy() {
            super.onDestroy();
            this.mBackground = null;
            unloadWallpaper(true);
        }

        /* access modifiers changed from: package-private */
        public boolean updateSurfaceSize(SurfaceHolder surfaceHolder, DisplayInfo displayInfo, boolean forDraw) {
            boolean hasWallpaper = true;
            if (this.mBackgroundWidth <= 0 || this.mBackgroundHeight <= 0) {
                loadWallpaper(forDraw, true);
                hasWallpaper = false;
            }
            int surfaceWidth = Math.max(displayInfo.logicalWidth, this.mBackgroundWidth);
            int surfaceHeight = Math.max(displayInfo.logicalHeight, this.mBackgroundHeight);
            surfaceHolder.setFixedSize(surfaceWidth, surfaceHeight);
            this.mLastRequestedWidth = surfaceWidth;
            this.mLastRequestedHeight = surfaceHeight;
            return hasWallpaper;
        }

        public void onVisibilityChanged(boolean visible) {
            if (this.mVisible != visible) {
                this.mVisible = visible;
                if (visible) {
                    drawFrame();
                }
            }
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixels, int yPixels) {
            if (!(this.mXOffset == xOffset && this.mYOffset == yOffset)) {
                this.mXOffset = xOffset;
                this.mYOffset = yOffset;
                this.mOffsetsChanged = true;
            }
            drawFrame();
        }

        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mSurfaceValid = false;
        }

        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            this.mLastSurfaceHeight = -1;
            this.mLastSurfaceWidth = -1;
            this.mSurfaceValid = true;
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            super.onSurfaceRedrawNeeded(holder);
            drawFrame();
        }

        /* access modifiers changed from: private */
        public DisplayInfo getDefaultDisplayInfo() {
            this.mDefaultDisplay.getDisplayInfo(this.mTmpDisplayInfo);
            return this.mTmpDisplayInfo;
        }

        /* access modifiers changed from: package-private */
        /* JADX WARNING: Removed duplicated region for block: B:28:0x006c A[Catch:{ all -> 0x016a }] */
        /* JADX WARNING: Removed duplicated region for block: B:35:0x0078 A[Catch:{ all -> 0x016a }] */
        /* JADX WARNING: Removed duplicated region for block: B:44:0x008f A[Catch:{ all -> 0x016a }] */
        /* JADX WARNING: Removed duplicated region for block: B:49:0x009f  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void drawFrame() {
            /*
                r22 = this;
                r7 = r22
                boolean r0 = r7.mSurfaceValid
                if (r0 != 0) goto L_0x0007
                return
            L_0x0007:
                r8 = 8
                r10 = 0
                java.lang.String r0 = "drawWallpaper"
                android.os.Trace.traceBegin(r8, r0)     // Catch:{ all -> 0x016a }
                android.view.DisplayInfo r0 = r22.getDefaultDisplayInfo()     // Catch:{ all -> 0x016a }
                int r1 = r0.rotation     // Catch:{ all -> 0x016a }
                r11 = r1
                int r1 = r7.mLastRotation     // Catch:{ all -> 0x016a }
                r2 = 1
                if (r11 != r1) goto L_0x0027
                int r1 = r7.mDisplayWidthAtLastSurfaceSizeUpdate     // Catch:{ all -> 0x016a }
                int r3 = r0.logicalWidth     // Catch:{ all -> 0x016a }
                if (r1 != r3) goto L_0x0027
                int r1 = r7.mDisplayHeightAtLastSurfaceSizeUpdate     // Catch:{ all -> 0x016a }
                int r3 = r0.logicalHeight     // Catch:{ all -> 0x016a }
                if (r1 == r3) goto L_0x0048
            L_0x0027:
                android.view.SurfaceHolder r1 = r22.getSurfaceHolder()     // Catch:{ all -> 0x016a }
                boolean r1 = r7.updateSurfaceSize(r1, r0, r2)     // Catch:{ all -> 0x016a }
                if (r1 != 0) goto L_0x003e
                android.os.Trace.traceEnd(r8)
                com.android.systemui.ImageWallpaper r1 = com.android.systemui.ImageWallpaper.this
                boolean r1 = r1.mIsHwAccelerated
                if (r1 != 0) goto L_0x003d
                r7.unloadWallpaper(r10)
            L_0x003d:
                return
            L_0x003e:
                r7.mRotationAtLastSurfaceSizeUpdate = r11     // Catch:{ all -> 0x016a }
                int r1 = r0.logicalWidth     // Catch:{ all -> 0x016a }
                r7.mDisplayWidthAtLastSurfaceSizeUpdate = r1     // Catch:{ all -> 0x016a }
                int r1 = r0.logicalHeight     // Catch:{ all -> 0x016a }
                r7.mDisplayHeightAtLastSurfaceSizeUpdate = r1     // Catch:{ all -> 0x016a }
            L_0x0048:
                android.view.SurfaceHolder r1 = r22.getSurfaceHolder()     // Catch:{ all -> 0x016a }
                r12 = r1
                android.graphics.Rect r1 = r12.getSurfaceFrame()     // Catch:{ all -> 0x016a }
                r13 = r1
                int r1 = r13.width()     // Catch:{ all -> 0x016a }
                r14 = r1
                int r1 = r13.height()     // Catch:{ all -> 0x016a }
                r15 = r1
                int r1 = r7.mLastSurfaceWidth     // Catch:{ all -> 0x016a }
                if (r14 != r1) goto L_0x0067
                int r1 = r7.mLastSurfaceHeight     // Catch:{ all -> 0x016a }
                if (r15 == r1) goto L_0x0065
                goto L_0x0067
            L_0x0065:
                r1 = r10
                goto L_0x0068
            L_0x0067:
                r1 = r2
            L_0x0068:
                r16 = r1
                if (r16 != 0) goto L_0x0073
                int r1 = r7.mLastRotation     // Catch:{ all -> 0x016a }
                if (r11 == r1) goto L_0x0071
                goto L_0x0073
            L_0x0071:
                r1 = r10
                goto L_0x0074
            L_0x0073:
                r1 = r2
            L_0x0074:
                r17 = r1
                if (r17 != 0) goto L_0x0089
                boolean r1 = r7.mOffsetsChanged     // Catch:{ all -> 0x016a }
                if (r1 != 0) goto L_0x0089
                android.os.Trace.traceEnd(r8)
                com.android.systemui.ImageWallpaper r1 = com.android.systemui.ImageWallpaper.this
                boolean r1 = r1.mIsHwAccelerated
                if (r1 != 0) goto L_0x0088
                r7.unloadWallpaper(r10)
            L_0x0088:
                return
            L_0x0089:
                r7.mLastRotation = r11     // Catch:{ all -> 0x016a }
                android.graphics.Bitmap r1 = r7.mBackground     // Catch:{ all -> 0x016a }
                if (r1 != 0) goto L_0x009f
                r7.loadWallpaper(r2, r2)     // Catch:{ all -> 0x016a }
                android.os.Trace.traceEnd(r8)
                com.android.systemui.ImageWallpaper r1 = com.android.systemui.ImageWallpaper.this
                boolean r1 = r1.mIsHwAccelerated
                if (r1 != 0) goto L_0x009e
                r7.unloadWallpaper(r10)
            L_0x009e:
                return
            L_0x009f:
                r1 = 1065353216(0x3f800000, float:1.0)
                float r2 = (float) r14
                android.graphics.Bitmap r3 = r7.mBackground     // Catch:{ all -> 0x016a }
                int r3 = r3.getWidth()     // Catch:{ all -> 0x016a }
                float r3 = (float) r3     // Catch:{ all -> 0x016a }
                float r2 = r2 / r3
                float r3 = (float) r15     // Catch:{ all -> 0x016a }
                android.graphics.Bitmap r4 = r7.mBackground     // Catch:{ all -> 0x016a }
                int r4 = r4.getHeight()     // Catch:{ all -> 0x016a }
                float r4 = (float) r4     // Catch:{ all -> 0x016a }
                float r3 = r3 / r4
                float r2 = java.lang.Math.max(r2, r3)     // Catch:{ all -> 0x016a }
                float r1 = java.lang.Math.max(r1, r2)     // Catch:{ all -> 0x016a }
                r7.mScale = r1     // Catch:{ all -> 0x016a }
                android.graphics.Bitmap r1 = r7.mBackground     // Catch:{ all -> 0x016a }
                int r1 = r1.getWidth()     // Catch:{ all -> 0x016a }
                float r1 = (float) r1     // Catch:{ all -> 0x016a }
                float r2 = r7.mScale     // Catch:{ all -> 0x016a }
                float r1 = r1 * r2
                int r1 = (int) r1     // Catch:{ all -> 0x016a }
                int r18 = r14 - r1
                android.graphics.Bitmap r1 = r7.mBackground     // Catch:{ all -> 0x016a }
                int r1 = r1.getHeight()     // Catch:{ all -> 0x016a }
                float r1 = (float) r1     // Catch:{ all -> 0x016a }
                float r2 = r7.mScale     // Catch:{ all -> 0x016a }
                float r1 = r1 * r2
                int r1 = (int) r1     // Catch:{ all -> 0x016a }
                int r19 = r15 - r1
                int r1 = r18 / 2
                int r2 = r19 / 2
                android.graphics.Bitmap r3 = r7.mBackground     // Catch:{ all -> 0x016a }
                int r3 = r3.getWidth()     // Catch:{ all -> 0x016a }
                int r6 = r14 - r3
                android.graphics.Bitmap r3 = r7.mBackground     // Catch:{ all -> 0x016a }
                int r3 = r3.getHeight()     // Catch:{ all -> 0x016a }
                int r5 = r15 - r3
                r3 = 1056964608(0x3f000000, float:0.5)
                if (r6 >= 0) goto L_0x00f7
                float r4 = (float) r6     // Catch:{ all -> 0x016a }
                float r8 = r7.mXOffset     // Catch:{ all -> 0x016a }
                float r8 = r8 - r3
                float r4 = r4 * r8
                float r4 = r4 + r3
                int r4 = (int) r4     // Catch:{ all -> 0x016a }
                int r1 = r1 + r4
            L_0x00f7:
                r8 = r1
                if (r5 >= 0) goto L_0x0102
                float r1 = (float) r5     // Catch:{ all -> 0x016a }
                float r4 = r7.mYOffset     // Catch:{ all -> 0x016a }
                float r4 = r4 - r3
                float r1 = r1 * r4
                float r1 = r1 + r3
                int r1 = (int) r1     // Catch:{ all -> 0x016a }
                int r2 = r2 + r1
            L_0x0102:
                r9 = r2
                r7.mOffsetsChanged = r10     // Catch:{ all -> 0x016a }
                if (r16 == 0) goto L_0x010b
                r7.mLastSurfaceWidth = r14     // Catch:{ all -> 0x016a }
                r7.mLastSurfaceHeight = r15     // Catch:{ all -> 0x016a }
            L_0x010b:
                if (r17 != 0) goto L_0x0124
                int r1 = r7.mLastXTranslation     // Catch:{ all -> 0x016a }
                if (r8 != r1) goto L_0x0124
                int r1 = r7.mLastYTranslation     // Catch:{ all -> 0x016a }
                if (r9 != r1) goto L_0x0124
                r1 = 8
                android.os.Trace.traceEnd(r1)
                com.android.systemui.ImageWallpaper r1 = com.android.systemui.ImageWallpaper.this
                boolean r1 = r1.mIsHwAccelerated
                if (r1 != 0) goto L_0x0123
                r7.unloadWallpaper(r10)
            L_0x0123:
                return
            L_0x0124:
                r7.mLastXTranslation = r8     // Catch:{ all -> 0x016a }
                r7.mLastYTranslation = r9     // Catch:{ all -> 0x016a }
                com.android.systemui.ImageWallpaper r1 = com.android.systemui.ImageWallpaper.this     // Catch:{ all -> 0x016a }
                boolean r1 = r1.mIsHwAccelerated     // Catch:{ all -> 0x016a }
                if (r1 == 0) goto L_0x014c
                r1 = r7
                r2 = r12
                r3 = r18
                r4 = r19
                r20 = r5
                r5 = r8
                r21 = r6
                r6 = r9
                boolean r1 = r1.drawWallpaperWithOpenGL(r2, r3, r4, r5, r6)     // Catch:{ all -> 0x016a }
                if (r1 != 0) goto L_0x015b
                r1 = r7
                r2 = r12
                r3 = r18
                r4 = r19
                r5 = r8
                r6 = r9
                r1.drawWallpaperWithCanvas(r2, r3, r4, r5, r6)     // Catch:{ all -> 0x016a }
                goto L_0x015b
            L_0x014c:
                r20 = r5
                r21 = r6
                r1 = r7
                r2 = r12
                r3 = r18
                r4 = r19
                r5 = r8
                r6 = r9
                r1.drawWallpaperWithCanvas(r2, r3, r4, r5, r6)     // Catch:{ all -> 0x016a }
            L_0x015b:
                r1 = 8
                android.os.Trace.traceEnd(r1)
                com.android.systemui.ImageWallpaper r0 = com.android.systemui.ImageWallpaper.this
                boolean r0 = r0.mIsHwAccelerated
                if (r0 != 0) goto L_0x0169
                r7.unloadWallpaper(r10)
            L_0x0169:
                return
            L_0x016a:
                r0 = move-exception
                r1 = 8
                android.os.Trace.traceEnd(r1)
                com.android.systemui.ImageWallpaper r1 = com.android.systemui.ImageWallpaper.this
                boolean r1 = r1.mIsHwAccelerated
                if (r1 != 0) goto L_0x0179
                r7.unloadWallpaper(r10)
            L_0x0179:
                throw r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.ImageWallpaper.DrawableEngine.drawFrame():void");
        }

        private void loadWallpaper(boolean needsDraw, final boolean needsReset) {
            this.mNeedsDrawAfterLoadingWallpaper |= needsDraw;
            if (this.mLoader != null) {
                if (needsReset) {
                    this.mLoader.cancel(false);
                    this.mLoader = null;
                } else {
                    return;
                }
            }
            this.mLoader = new AsyncTask<Void, Void, Bitmap>() {
                /* access modifiers changed from: protected */
                public Bitmap doInBackground(Void... params) {
                    try {
                        if (needsReset) {
                            ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                        }
                        return ImageWallpaper.this.mWallpaperManager.getBitmap();
                    } catch (OutOfMemoryError | RuntimeException exception) {
                        if (!isCancelled() && exception != null) {
                            Log.w("ImageWallpaper", "Unable to load wallpaper!", exception);
                            try {
                                ImageWallpaper.this.mWallpaperManager.clear();
                            } catch (IOException ex) {
                                Log.w("ImageWallpaper", "Unable reset to default wallpaper!", ex);
                            }
                            if (isCancelled()) {
                                return null;
                            }
                            try {
                                return ImageWallpaper.this.mWallpaperManager.getBitmap();
                            } catch (OutOfMemoryError | RuntimeException e) {
                                Log.w("ImageWallpaper", "Unable to load default wallpaper!", e);
                                return null;
                            }
                        }
                        return null;
                    }
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(Bitmap b) {
                    DrawableEngine.this.mBackground = null;
                    DrawableEngine.this.mBackgroundWidth = -1;
                    DrawableEngine.this.mBackgroundHeight = -1;
                    if (b != null) {
                        DrawableEngine.this.mBackground = b;
                        DrawableEngine.this.mBackgroundWidth = DrawableEngine.this.mBackground.getWidth();
                        DrawableEngine.this.mBackgroundHeight = DrawableEngine.this.mBackground.getHeight();
                    }
                    DrawableEngine.this.updateSurfaceSize(DrawableEngine.this.getSurfaceHolder(), DrawableEngine.this.getDefaultDisplayInfo(), false);
                    if (DrawableEngine.this.mNeedsDrawAfterLoadingWallpaper) {
                        DrawableEngine.this.drawFrame();
                    }
                    AsyncTask unused = DrawableEngine.this.mLoader = null;
                    boolean unused2 = DrawableEngine.this.mNeedsDrawAfterLoadingWallpaper = false;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }

        private void unloadWallpaper(boolean forgetSize) {
            if (this.mLoader != null) {
                this.mLoader.cancel(false);
                this.mLoader = null;
            }
            this.mBackground = null;
            if (forgetSize) {
                this.mBackgroundWidth = -1;
                this.mBackgroundHeight = -1;
            }
            this.mLoader = new AsyncTask<Void, Void, Bitmap>() {
                /* access modifiers changed from: protected */
                public Bitmap doInBackground(Void... params) {
                    ImageWallpaper.this.mWallpaperManager.forgetLoadedWallpaper();
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }

        /* access modifiers changed from: protected */
        public void dump(String prefix, FileDescriptor fd, PrintWriter out, String[] args) {
            super.dump(prefix, fd, out, args);
            out.print(prefix);
            out.println("ImageWallpaper.DrawableEngine:");
            out.print(prefix);
            out.print(" mBackground=");
            out.print(this.mBackground);
            out.print(" mBackgroundWidth=");
            out.print(this.mBackgroundWidth);
            out.print(" mBackgroundHeight=");
            out.println(this.mBackgroundHeight);
            out.print(prefix);
            out.print(" mLastRotation=");
            out.print(this.mLastRotation);
            out.print(" mLastSurfaceWidth=");
            out.print(this.mLastSurfaceWidth);
            out.print(" mLastSurfaceHeight=");
            out.println(this.mLastSurfaceHeight);
            out.print(prefix);
            out.print(" mXOffset=");
            out.print(this.mXOffset);
            out.print(" mYOffset=");
            out.println(this.mYOffset);
            out.print(prefix);
            out.print(" mVisible=");
            out.print(this.mVisible);
            out.print(" mOffsetsChanged=");
            out.println(this.mOffsetsChanged);
            out.print(prefix);
            out.print(" mLastXTranslation=");
            out.print(this.mLastXTranslation);
            out.print(" mLastYTranslation=");
            out.print(this.mLastYTranslation);
            out.print(" mScale=");
            out.println(this.mScale);
            out.print(prefix);
            out.print(" mLastRequestedWidth=");
            out.print(this.mLastRequestedWidth);
            out.print(" mLastRequestedHeight=");
            out.println(this.mLastRequestedHeight);
            out.print(prefix);
            out.println(" DisplayInfo at last updateSurfaceSize:");
            out.print(prefix);
            out.print("  rotation=");
            out.print(this.mRotationAtLastSurfaceSizeUpdate);
            out.print("  width=");
            out.print(this.mDisplayWidthAtLastSurfaceSizeUpdate);
            out.print("  height=");
            out.println(this.mDisplayHeightAtLastSurfaceSizeUpdate);
        }

        private void drawWallpaperWithCanvas(SurfaceHolder sh, int w, int h, int left, int top) {
            Canvas c = sh.lockCanvas();
            if (c != null) {
                try {
                    float right = ((float) left) + (((float) this.mBackground.getWidth()) * this.mScale);
                    float bottom = ((float) top) + (((float) this.mBackground.getHeight()) * this.mScale);
                    if (w < 0 || h < 0) {
                        c.save(2);
                        c.clipRect((float) left, (float) top, right, bottom, Region.Op.DIFFERENCE);
                        c.drawColor(-16777216);
                        c.restore();
                    }
                    if (this.mBackground != null) {
                        c.drawBitmap(this.mBackground, null, new RectF((float) left, (float) top, right, bottom), null);
                    }
                } finally {
                    sh.unlockCanvasAndPost(c);
                }
            }
        }

        private boolean drawWallpaperWithOpenGL(SurfaceHolder sh, int w, int h, int left, int top) {
            int i = left;
            int i2 = top;
            if (!initGL(sh)) {
                return false;
            }
            float right = ((float) i) + (((float) this.mBackground.getWidth()) * this.mScale);
            float bottom = ((float) i2) + (((float) this.mBackground.getHeight()) * this.mScale);
            Rect frame = sh.getSurfaceFrame();
            Matrix4f ortho = new Matrix4f();
            ortho.loadOrtho(0.0f, (float) frame.width(), (float) frame.height(), 0.0f, -1.0f, 1.0f);
            FloatBuffer triangleVertices = createMesh(i, i2, right, bottom);
            int texture = loadTexture(this.mBackground);
            int program = buildProgram("attribute vec4 position;\nattribute vec2 texCoords;\nvarying vec2 outTexCoords;\nuniform mat4 projection;\n\nvoid main(void) {\n    outTexCoords = texCoords;\n    gl_Position = projection * position;\n}\n\n", "precision mediump float;\n\nvarying vec2 outTexCoords;\nuniform sampler2D texture;\n\nvoid main(void) {\n    gl_FragColor = texture2D(texture, outTexCoords);\n}\n\n");
            int attribPosition = GLES20.glGetAttribLocation(program, "position");
            int attribTexCoords = GLES20.glGetAttribLocation(program, "texCoords");
            int uniformTexture = GLES20.glGetUniformLocation(program, "texture");
            int uniformProjection = GLES20.glGetUniformLocation(program, "projection");
            checkGlError();
            GLES20.glViewport(0, 0, frame.width(), frame.height());
            GLES20.glBindTexture(3553, texture);
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(attribPosition);
            GLES20.glEnableVertexAttribArray(attribTexCoords);
            GLES20.glUniform1i(uniformTexture, 0);
            GLES20.glUniformMatrix4fv(uniformProjection, 1, false, ortho.getArray(), 0);
            checkGlError();
            if (w > 0 || h > 0) {
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GLES20.glClear(16384);
            }
            triangleVertices.position(0);
            FloatBuffer floatBuffer = triangleVertices;
            GLES20.glVertexAttribPointer(attribPosition, 3, 5126, false, 20, floatBuffer);
            triangleVertices.position(3);
            GLES20.glVertexAttribPointer(attribTexCoords, 3, 5126, false, 20, floatBuffer);
            GLES20.glDrawArrays(5, 0, 4);
            boolean status = this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface);
            checkEglError();
            finishGL(texture, program);
            return status;
        }

        private FloatBuffer createMesh(int left, int top, float right, float bottom) {
            float[] verticesData = {(float) left, bottom, 0.0f, 0.0f, 1.0f, right, bottom, 0.0f, 1.0f, 1.0f, (float) left, (float) top, 0.0f, 0.0f, 0.0f, right, (float) top, 0.0f, 1.0f, 0.0f};
            FloatBuffer triangleVertices = ByteBuffer.allocateDirect(verticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            triangleVertices.put(verticesData).position(0);
            return triangleVertices;
        }

        private int loadTexture(Bitmap bitmap) {
            int[] textures = new int[1];
            GLES20.glActiveTexture(33984);
            GLES20.glGenTextures(1, textures, 0);
            checkGlError();
            int texture = textures[0];
            GLES20.glBindTexture(3553, texture);
            checkGlError();
            GLES20.glTexParameteri(3553, 10241, 9729);
            GLES20.glTexParameteri(3553, 10240, 9729);
            GLES20.glTexParameteri(3553, 10242, 33071);
            GLES20.glTexParameteri(3553, 10243, 33071);
            GLUtils.texImage2D(3553, 0, 6408, bitmap, 5121, 0);
            checkGlError();
            return texture;
        }

        private int buildProgram(String vertex, String fragment) {
            int vertexShader = buildShader(vertex, 35633);
            if (vertexShader == 0) {
                return 0;
            }
            int fragmentShader = buildShader(fragment, 35632);
            if (fragmentShader == 0) {
                return 0;
            }
            int program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);
            checkGlError();
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            int[] status = new int[1];
            GLES20.glGetProgramiv(program, 35714, status, 0);
            if (status[0] == 1) {
                return program;
            }
            String error = GLES20.glGetProgramInfoLog(program);
            Log.d("ImageWallpaperGL", "Error while linking program:\n" + error);
            GLES20.glDeleteProgram(program);
            return 0;
        }

        private int buildShader(String source, int type) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, source);
            checkGlError();
            GLES20.glCompileShader(shader);
            checkGlError();
            int[] status = new int[1];
            GLES20.glGetShaderiv(shader, 35713, status, 0);
            if (status[0] == 1) {
                return shader;
            }
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.d("ImageWallpaperGL", "Error while compiling shader:\n" + error);
            GLES20.glDeleteShader(shader);
            return 0;
        }

        private void checkEglError() {
            int error = this.mEgl.eglGetError();
            if (error != 12288) {
                Log.w("ImageWallpaperGL", "EGL error = " + GLUtils.getEGLErrorString(error));
            }
        }

        private void checkGlError() {
            int error = GLES20.glGetError();
            if (error != 0) {
                Log.w("ImageWallpaperGL", "GL error = 0x" + Integer.toHexString(error), new Throwable());
            }
        }

        private void finishGL(int texture, int program) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
            GLES20.glDeleteProgram(program);
            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            this.mEgl.eglDestroySurface(this.mEglDisplay, this.mEglSurface);
            this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
            this.mEgl.eglTerminate(this.mEglDisplay);
        }

        private boolean initGL(SurfaceHolder surfaceHolder) {
            this.mEgl = (EGL10) EGLContext.getEGL();
            this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (this.mEglDisplay != EGL10.EGL_NO_DISPLAY) {
                if (this.mEgl.eglInitialize(this.mEglDisplay, new int[2])) {
                    this.mEglConfig = chooseEglConfig();
                    if (this.mEglConfig != null) {
                        this.mEglContext = createContext(this.mEgl, this.mEglDisplay, this.mEglConfig);
                        if (this.mEglContext != EGL10.EGL_NO_CONTEXT) {
                            EGLSurface tmpSurface = this.mEgl.eglCreatePbufferSurface(this.mEglDisplay, this.mEglConfig, new int[]{12375, 1, 12374, 1, 12344});
                            this.mEgl.eglMakeCurrent(this.mEglDisplay, tmpSurface, tmpSurface, this.mEglContext);
                            int[] maxSize = new int[1];
                            Rect frame = surfaceHolder.getSurfaceFrame();
                            GLES20.glGetIntegerv(3379, maxSize, 0);
                            this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                            this.mEgl.eglDestroySurface(this.mEglDisplay, tmpSurface);
                            if (frame.width() > maxSize[0] || frame.height() > maxSize[0]) {
                                this.mEgl.eglDestroyContext(this.mEglDisplay, this.mEglContext);
                                this.mEgl.eglTerminate(this.mEglDisplay);
                                Log.e("ImageWallpaperGL", "requested  texture size " + frame.width() + "x" + frame.height() + " exceeds the support maximum of " + maxSize[0] + "x" + maxSize[0]);
                                return false;
                            }
                            this.mEglSurface = this.mEgl.eglCreateWindowSurface(this.mEglDisplay, this.mEglConfig, surfaceHolder, null);
                            if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                                int error = this.mEgl.eglGetError();
                                if (error == 12299 || error == 12291) {
                                    Log.e("ImageWallpaperGL", "createWindowSurface returned " + GLUtils.getEGLErrorString(error) + ".");
                                    return false;
                                }
                                throw new RuntimeException("createWindowSurface failed " + GLUtils.getEGLErrorString(error));
                            } else if (this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                                return true;
                            } else {
                                throw new RuntimeException("eglMakeCurrent failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
                            }
                        } else {
                            throw new RuntimeException("createContext failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
                        }
                    } else {
                        throw new RuntimeException("eglConfig not initialized");
                    }
                } else {
                    throw new RuntimeException("eglInitialize failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
                }
            } else {
                throw new RuntimeException("eglGetDisplay failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            }
        }

        /* access modifiers changed from: package-private */
        public EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, new int[]{12440, 2, 12344});
        }

        private EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            if (!this.mEgl.eglChooseConfig(this.mEglDisplay, getConfig(), configs, 1, configsCount)) {
                throw new IllegalArgumentException("eglChooseConfig failed " + GLUtils.getEGLErrorString(this.mEgl.eglGetError()));
            } else if (configsCount[0] > 0) {
                return configs[0];
            } else {
                return null;
            }
        }

        private int[] getConfig() {
            return new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12327, 12344, 12344};
        }
    }

    public void onCreate() {
        super.onCreate();
        this.mWallpaperManager = (WallpaperManager) getSystemService("wallpaper");
        this.mIsHwAccelerated = ActivityManager.isHighEndGfx();
    }

    public void onTrimMemory(int level) {
        if (this.mEngine != null) {
            this.mEngine.trimMemory(level);
        }
    }

    public WallpaperService.Engine onCreateEngine() {
        this.mEngine = new DrawableEngine();
        return this.mEngine;
    }
}
