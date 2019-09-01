package com.android.systemui.screenshot;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.android.systemui.Util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/* compiled from: GlobalScreenshot */
class SaveImageInBackgroundTask extends AsyncTask<SaveImageInBackgroundData, Void, SaveImageInBackgroundData> {
    private static boolean mTickerAddSpace;
    private String mImageFileName;
    private String mImageFilePath;
    private int mImageHeight;
    private long mImageTime = System.currentTimeMillis();
    private int mImageWidth;
    private NotificationManager mNotificationManager;
    public NotifyMediaStoreData mNotifyMediaStoreData;
    private File mScreenshotDir;
    private String mTempImageFilePath;

    SaveImageInBackgroundTask(Context context, SaveImageInBackgroundData data, NotificationManager nManager) {
        Resources resources = context.getResources();
        String imageDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS").format(new Date(this.mImageTime));
        this.mScreenshotDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Screenshots");
        if (!this.mScreenshotDir.exists()) {
            this.mScreenshotDir.mkdirs();
        }
        this.mImageFileName = String.format("Screenshot_%s_%s.png", new Object[]{imageDate, Util.getTopActivityPkg(context, true)});
        this.mImageFilePath = String.format("%s/%s", new Object[]{this.mScreenshotDir, this.mImageFileName});
        this.mTempImageFilePath = String.format("%s/%s", new Object[]{this.mScreenshotDir, "." + this.mImageFileName});
        this.mImageWidth = data.image.getWidth();
        this.mImageHeight = data.image.getHeight();
        mTickerAddSpace = mTickerAddSpace ^ true;
        this.mNotificationManager = nManager;
        this.mNotifyMediaStoreData = new NotifyMediaStoreData();
        this.mNotifyMediaStoreData.imageFilePath = this.mImageFilePath;
        this.mNotifyMediaStoreData.tempImageFilePath = this.mTempImageFilePath;
        this.mNotifyMediaStoreData.imageFileName = this.mImageFileName;
        this.mNotifyMediaStoreData.width = this.mImageWidth;
        this.mNotifyMediaStoreData.height = this.mImageHeight;
        this.mNotifyMediaStoreData.takenTime = this.mImageTime;
    }

    /* access modifiers changed from: protected */
    public SaveImageInBackgroundData doInBackground(SaveImageInBackgroundData... params) {
        if (params.length != 1) {
            return null;
        }
        Context context = params[0].context;
        Bitmap image = params[0].image;
        Resources resources = context.getResources();
        try {
            OutputStream out = new FileOutputStream(this.mTempImageFilePath);
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            params[0].result = 0;
            Log.e("GlobalScreenshot", "Save success, mScreenshotDir.getAbsolutePath() = " + this.mScreenshotDir.getAbsolutePath() + ", mTempImageFilePath = " + this.mTempImageFilePath);
        } catch (Exception e) {
            Log.e("GlobalScreenshot", "Save fail, mScreenshotDir.getAbsolutePath() = " + this.mScreenshotDir.getAbsolutePath() + ", mTempImageFilePath = " + this.mTempImageFilePath);
            for (File tempFile = this.mScreenshotDir; tempFile != null; tempFile = tempFile.getParentFile()) {
                Log.e("GlobalScreenshot", "Save fail, path = " + tempFile.getAbsolutePath() + ", exists = " + tempFile.exists() + "\n");
            }
            params[0].result = 1;
            e.printStackTrace();
        }
        return params[0];
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(SaveImageInBackgroundData params) {
        this.mNotifyMediaStoreData.saveFinished = true;
        if (this.mNotifyMediaStoreData.isPending) {
            GlobalScreenshot.notifyMediaAndFinish(params.context, this.mNotifyMediaStoreData);
        }
        if (params.result > 0) {
            GlobalScreenshot.notifyScreenshotError(params.context, this.mNotificationManager);
        }
        if (params.finisher != null) {
            params.finisher.run();
        }
    }
}
