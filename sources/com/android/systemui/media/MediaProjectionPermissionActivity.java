package com.android.systemui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.projection.IMediaProjection;
import android.media.projection.IMediaProjectionManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.android.systemui.R;

public class MediaProjectionPermissionActivity extends Activity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private AlertDialog mDialog;
    private String mPackageName;
    private boolean mPermanentGrant;
    private IMediaProjectionManager mService;
    private int mUid;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPackageName = getCallingPackage();
        this.mService = IMediaProjectionManager.Stub.asInterface(ServiceManager.getService("media_projection"));
        if (this.mPackageName == null) {
            finish();
            return;
        }
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo aInfo = packageManager.getApplicationInfo(this.mPackageName, 0);
            this.mUid = aInfo.uid;
            try {
                if (this.mService.hasProjectionPermission(this.mUid, this.mPackageName)) {
                    setResult(-1, getMediaProjectionIntent(this.mUid, this.mPackageName, false));
                    finish();
                    return;
                }
                TextPaint paint = new TextPaint();
                paint.setTextSize(42.0f);
                String label = aInfo.loadLabel(packageManager).toString();
                int labelLength = label.length();
                int offset = 0;
                while (true) {
                    if (offset >= labelLength) {
                        break;
                    }
                    int codePoint = label.codePointAt(offset);
                    int type = Character.getType(codePoint);
                    if (type == 13 || type == 15 || type == 14) {
                        label = label.substring(0, offset) + "…";
                    } else {
                        offset += Character.charCount(codePoint);
                    }
                }
                label = label.substring(0, offset) + "…";
                if (label.isEmpty() != 0) {
                    label = this.mPackageName;
                }
                String appName = BidiFormatter.getInstance().unicodeWrap(TextUtils.ellipsize(label, paint, 500.0f, TextUtils.TruncateAt.END).toString());
                String actionText = getString(R.string.media_projection_dialog_text, new Object[]{appName});
                SpannableString message = new SpannableString(actionText);
                int appNameIndex = actionText.indexOf(appName);
                if (appNameIndex >= 0) {
                    message.setSpan(new StyleSpan(1), appNameIndex, appNameIndex + appName.length(), 0);
                }
                View view = LayoutInflater.from(this).inflate(R.layout.remember_permission_checkbox, null);
                this.mDialog = new AlertDialog.Builder(this, com.android.systemui.plugins.R.style.Theme_Dialog_Alert).setIcon(aInfo.loadIcon(packageManager)).setMessage(message).setPositiveButton(R.string.media_projection_action_text, this).setNegativeButton(17039360, this).setView(view).setOnCancelListener(this).create();
                this.mDialog.create();
                this.mDialog.getButton(-1).setFilterTouchesWhenObscured(true);
                ((CheckBox) view.findViewById(R.id.remember)).setOnCheckedChangeListener(this);
                Window w = this.mDialog.getWindow();
                w.setType(2003);
                w.addPrivateFlags(524288);
                this.mDialog.show();
            } catch (RemoteException e) {
                Log.e("MediaProjectionPermissionActivity", "Error checking projection permissions", e);
                finish();
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e("MediaProjectionPermissionActivity", "unable to look up package name", e2);
            finish();
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0021, code lost:
        if (r4.mDialog == null) goto L_0x003a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0033, code lost:
        if (r4.mDialog != null) goto L_0x0035;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0035, code lost:
        r4.mDialog.dismiss();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x003a, code lost:
        finish();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x003e, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onClick(android.content.DialogInterface r5, int r6) {
        /*
            r4 = this;
            r0 = -1
            if (r6 != r0) goto L_0x0031
            int r1 = r4.mUid     // Catch:{ RemoteException -> 0x0013 }
            java.lang.String r2 = r4.mPackageName     // Catch:{ RemoteException -> 0x0013 }
            boolean r3 = r4.mPermanentGrant     // Catch:{ RemoteException -> 0x0013 }
            android.content.Intent r1 = r4.getMediaProjectionIntent(r1, r2, r3)     // Catch:{ RemoteException -> 0x0013 }
            r4.setResult(r0, r1)     // Catch:{ RemoteException -> 0x0013 }
            goto L_0x0031
        L_0x0011:
            r0 = move-exception
            goto L_0x0024
        L_0x0013:
            r0 = move-exception
            java.lang.String r1 = "MediaProjectionPermissionActivity"
            java.lang.String r2 = "Error granting projection permission"
            android.util.Log.e(r1, r2, r0)     // Catch:{ all -> 0x0011 }
            r1 = 0
            r4.setResult(r1)     // Catch:{ all -> 0x0011 }
            android.app.AlertDialog r0 = r4.mDialog
            if (r0 == 0) goto L_0x003a
            goto L_0x0035
        L_0x0024:
            android.app.AlertDialog r1 = r4.mDialog
            if (r1 == 0) goto L_0x002d
            android.app.AlertDialog r1 = r4.mDialog
            r1.dismiss()
        L_0x002d:
            r4.finish()
            throw r0
        L_0x0031:
            android.app.AlertDialog r0 = r4.mDialog
            if (r0 == 0) goto L_0x003a
        L_0x0035:
            android.app.AlertDialog r0 = r4.mDialog
            r0.dismiss()
        L_0x003a:
            r4.finish()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.media.MediaProjectionPermissionActivity.onClick(android.content.DialogInterface, int):void");
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        this.mPermanentGrant = isChecked;
    }

    private Intent getMediaProjectionIntent(int uid, String packageName, boolean permanentGrant) throws RemoteException {
        IMediaProjection projection = this.mService.createProjection(uid, packageName, 0, permanentGrant);
        Intent intent = new Intent();
        intent.putExtra("android.media.projection.extra.EXTRA_MEDIA_PROJECTION", projection.asBinder());
        return intent;
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
