package com.android.systemui.qs;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;

public class QSFooterDataUsage extends FrameLayout {
    private boolean mAvailable = false;
    private Handler mBgHandler;
    private TextView mDataUsage;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 100000) {
                QSFooterDataUsage.this.setDataUsage((DataUsageInfo) msg.obj);
            }
        }
    };
    /* access modifiers changed from: private */
    public Intent mIntent1;
    /* access modifiers changed from: private */
    public Intent mIntent2;
    private Uri mNetworkUri;
    private ImageView mPieImage;
    private TextView mPurchase;
    private QSContainerImpl mQSContainer;

    protected class DataUsageInfo {
        private boolean mDataUsageAvailable;
        private Bitmap mIconImage;
        private CharSequence mText1;
        private String mText2;

        protected DataUsageInfo() {
        }

        public String getText2() {
            return this.mText2;
        }

        public void setText2(String text2) {
            this.mText2 = text2;
        }

        public CharSequence getText1() {
            return this.mText1;
        }

        public void setText1(CharSequence text1) {
            this.mText1 = text1;
        }

        public Bitmap getIconImage() {
            return this.mIconImage;
        }

        public void setIconImage(Bitmap iconImage) {
            this.mIconImage = iconImage;
        }

        public boolean isDataUsageAvailable() {
            return this.mDataUsageAvailable;
        }

        public void setDataUsageAvailable(boolean dataUsageAvaliable) {
            this.mDataUsageAvailable = dataUsageAvaliable;
        }
    }

    private final class QueryDataUsageHandler extends Handler {
        QueryDataUsageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                QSFooterDataUsage.this.queryDataUsage();
            }
        }
    }

    public QSFooterDataUsage(Context context, AttributeSet attrs) {
        super(context, attrs);
        initNetworkAssistantProviderUri();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPieImage = (ImageView) findViewById(R.id.pie);
        this.mDataUsage = (TextView) findViewById(R.id.data_usage);
        this.mDataUsage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (QSFooterDataUsage.this.mIntent1 != null) {
                    ((ActivityStarter) Dependency.get(ActivityStarter.class)).startActivity(QSFooterDataUsage.this.mIntent1, true);
                }
            }
        });
        this.mPurchase = (TextView) findViewById(R.id.data_purchase);
        this.mPurchase.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (QSFooterDataUsage.this.mIntent2 != null) {
                    ((ActivityStarter) Dependency.get(ActivityStarter.class)).startActivity(QSFooterDataUsage.this.mIntent2, true);
                }
            }
        });
        this.mPurchase.setVisibility(0);
        this.mBgHandler = new QueryDataUsageHandler((Looper) Dependency.get(Dependency.BG_LOOPER));
        updateDataUsageInfo();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    public void updateDataUsageInfo() {
        if (this.mBgHandler != null) {
            this.mBgHandler.removeMessages(100);
            this.mBgHandler.sendEmptyMessage(100);
        }
    }

    public void setQSContainer(QSContainerImpl container) {
        this.mQSContainer = container;
    }

    /* access modifiers changed from: private */
    public void setDataUsage(DataUsageInfo usageInfo) {
        boolean avaliable = usageInfo.isDataUsageAvailable();
        if (avaliable) {
            this.mPieImage.setImageBitmap(usageInfo.getIconImage());
            this.mDataUsage.setText(usageInfo.getText1());
            this.mPurchase.setText(usageInfo.getText2());
        }
        if (avaliable != this.mAvailable) {
            this.mAvailable = avaliable;
            if (this.mQSContainer != null) {
                this.mQSContainer.updateFooter();
            }
        }
    }

    public boolean isAvailable() {
        return this.mAvailable;
    }

    private void initNetworkAssistantProviderUri() {
        this.mNetworkUri = Uri.parse("content://com.miui.networkassistant.provider/datausage_noti_status");
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00ef, code lost:
        if (r2 != null) goto L_0x00f1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00f1, code lost:
        r2.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x011c, code lost:
        if (r2 == null) goto L_0x011f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x011f, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void queryDataUsage() {
        /*
            r13 = this;
            boolean r0 = com.android.systemui.statusbar.phone.StatusBar.sBootCompleted
            if (r0 != 0) goto L_0x0005
            return
        L_0x0005:
            com.android.systemui.qs.QSFooterDataUsage$DataUsageInfo r0 = new com.android.systemui.qs.QSFooterDataUsage$DataUsageInfo
            r0.<init>()
            r1 = 0
            r2 = r1
            android.content.Context r3 = r13.mContext     // Catch:{ Exception -> 0x00f7 }
            android.content.ContentResolver r4 = r3.getContentResolver()     // Catch:{ Exception -> 0x00f7 }
            android.net.Uri r5 = r13.mNetworkUri     // Catch:{ Exception -> 0x00f7 }
            r6 = 0
            r7 = 0
            r8 = 0
            r9 = 0
            android.database.Cursor r3 = r4.query(r5, r6, r7, r8, r9)     // Catch:{ Exception -> 0x00f7 }
            r2 = r3
            if (r2 == 0) goto L_0x00d6
            boolean r3 = r2.moveToFirst()     // Catch:{ Exception -> 0x00f7 }
            if (r3 == 0) goto L_0x00d6
            java.lang.String r3 = "text1"
            int r3 = r2.getColumnIndex(r3)     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r3 = r2.getString(r3)     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r4 = "text2"
            int r4 = r2.getColumnIndex(r4)     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r4 = r2.getString(r4)     // Catch:{ Exception -> 0x00f7 }
            boolean r5 = android.text.TextUtils.isEmpty(r3)     // Catch:{ Exception -> 0x00f7 }
            if (r5 == 0) goto L_0x0052
            boolean r5 = android.text.TextUtils.isEmpty(r4)     // Catch:{ Exception -> 0x00f7 }
            if (r5 == 0) goto L_0x0052
            java.lang.String r1 = "QSFooterDataUsage"
            java.lang.String r5 = "queryDataUsage: cannot find text1, text2."
            android.util.Log.d(r1, r5)     // Catch:{ Exception -> 0x00f7 }
            if (r2 == 0) goto L_0x0051
            r2.close()
        L_0x0051:
            return
        L_0x0052:
            java.lang.String r5 = "icon"
            int r5 = r2.getColumnIndex(r5)     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r5 = r2.getString(r5)     // Catch:{ Exception -> 0x00f7 }
            android.net.Uri r6 = android.net.Uri.parse(r5)     // Catch:{ Exception -> 0x00f7 }
            android.content.Context r7 = r13.mContext     // Catch:{ Exception -> 0x00f7 }
            android.content.ContentResolver r7 = r7.getContentResolver()     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r8 = "r"
            android.os.ParcelFileDescriptor r7 = r7.openFileDescriptor(r6, r8)     // Catch:{ Exception -> 0x00f7 }
            android.os.ParcelFileDescriptor$AutoCloseInputStream r8 = new android.os.ParcelFileDescriptor$AutoCloseInputStream     // Catch:{ Exception -> 0x00f7 }
            r8.<init>(r7)     // Catch:{ Exception -> 0x00f7 }
            android.graphics.Bitmap r8 = android.graphics.BitmapFactory.decodeStream(r8)     // Catch:{ Exception -> 0x00f7 }
            if (r8 != 0) goto L_0x0084
            java.lang.String r1 = "QSFooterDataUsage"
            java.lang.String r9 = "queryDataUsage: cannot load icon."
            android.util.Log.d(r1, r9)     // Catch:{ Exception -> 0x00f7 }
            if (r2 == 0) goto L_0x0083
            r2.close()
        L_0x0083:
            return
        L_0x0084:
            java.lang.String r9 = "action1"
            int r9 = r2.getColumnIndex(r9)     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r9 = r2.getString(r9)     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r10 = "action2"
            int r10 = r2.getColumnIndex(r10)     // Catch:{ Exception -> 0x00f7 }
            java.lang.String r10 = r2.getString(r10)     // Catch:{ Exception -> 0x00f7 }
            r11 = 1
            android.content.Intent r12 = android.content.Intent.parseUri(r9, r11)     // Catch:{ Exception -> 0x00f7 }
            r13.mIntent1 = r12     // Catch:{ Exception -> 0x00f7 }
            android.content.Intent r12 = android.content.Intent.parseUri(r10, r11)     // Catch:{ Exception -> 0x00f7 }
            r13.mIntent2 = r12     // Catch:{ Exception -> 0x00f7 }
            android.content.Intent r12 = r13.mIntent1     // Catch:{ Exception -> 0x00f7 }
            if (r12 != 0) goto L_0x00ba
            android.content.Intent r12 = r13.mIntent2     // Catch:{ Exception -> 0x00f7 }
            if (r12 != 0) goto L_0x00ba
            java.lang.String r1 = "QSFooterDataUsage"
            java.lang.String r11 = "queryDataUsage: cannot find action1, action2."
            android.util.Log.d(r1, r11)     // Catch:{ Exception -> 0x00f7 }
            if (r2 == 0) goto L_0x00b9
            r2.close()
        L_0x00b9:
            return
        L_0x00ba:
            r0.setDataUsageAvailable(r11)     // Catch:{ Exception -> 0x00f7 }
            if (r3 == 0) goto L_0x00cc
            java.lang.String r1 = "&nbsp;"
            java.lang.String r11 = "&ensp;"
            java.lang.String r1 = r3.replaceAll(r1, r11)     // Catch:{ Exception -> 0x00f7 }
            android.text.Spanned r1 = android.text.Html.fromHtml(r1)     // Catch:{ Exception -> 0x00f7 }
        L_0x00cc:
            r0.setText1(r1)     // Catch:{ Exception -> 0x00f7 }
            r0.setText2(r4)     // Catch:{ Exception -> 0x00f7 }
            r0.setIconImage(r8)     // Catch:{ Exception -> 0x00f7 }
            goto L_0x00da
        L_0x00d6:
            r1 = 0
            r0.setDataUsageAvailable(r1)     // Catch:{ Exception -> 0x00f7 }
        L_0x00da:
            android.os.Handler r1 = r13.mHandler     // Catch:{ Exception -> 0x00f7 }
            r3 = 100000(0x186a0, float:1.4013E-40)
            r1.removeMessages(r3)     // Catch:{ Exception -> 0x00f7 }
            android.os.Message r1 = android.os.Message.obtain()     // Catch:{ Exception -> 0x00f7 }
            r1.what = r3     // Catch:{ Exception -> 0x00f7 }
            r1.obj = r0     // Catch:{ Exception -> 0x00f7 }
            android.os.Handler r3 = r13.mHandler     // Catch:{ Exception -> 0x00f7 }
            r3.sendMessage(r1)     // Catch:{ Exception -> 0x00f7 }
            if (r2 == 0) goto L_0x011f
        L_0x00f1:
            r2.close()
            goto L_0x011f
        L_0x00f5:
            r1 = move-exception
            goto L_0x0120
        L_0x00f7:
            r1 = move-exception
            java.lang.String r3 = "QSFooterDataUsage"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ all -> 0x00f5 }
            r4.<init>()     // Catch:{ all -> 0x00f5 }
            java.lang.Throwable r5 = r1.getCause()     // Catch:{ all -> 0x00f5 }
            r4.append(r5)     // Catch:{ all -> 0x00f5 }
            java.lang.String r5 = ", "
            r4.append(r5)     // Catch:{ all -> 0x00f5 }
            java.lang.String r5 = r1.getMessage()     // Catch:{ all -> 0x00f5 }
            r4.append(r5)     // Catch:{ all -> 0x00f5 }
            java.lang.String r4 = r4.toString()     // Catch:{ all -> 0x00f5 }
            android.util.Log.d(r3, r4)     // Catch:{ all -> 0x00f5 }
            r1.printStackTrace()     // Catch:{ all -> 0x00f5 }
            if (r2 == 0) goto L_0x011f
            goto L_0x00f1
        L_0x011f:
            return
        L_0x0120:
            if (r2 == 0) goto L_0x0125
            r2.close()
        L_0x0125:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.qs.QSFooterDataUsage.queryDataUsage():void");
    }
}
