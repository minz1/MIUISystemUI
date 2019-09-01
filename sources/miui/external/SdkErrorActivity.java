package miui.external;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SdkErrorActivity extends Activity implements SdkConstants {
    private DialogInterface.OnClickListener mDismissListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            SdkErrorActivity.this.finish();
            System.exit(0);
        }
    };
    private String mLanguage;
    private DialogInterface.OnClickListener mUpdateListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            final Dialog updateDialog = SdkErrorActivity.this.createUpdateDialog();
            new SdkDialogFragment(updateDialog).show(SdkErrorActivity.this.getFragmentManager(), "SdkUpdatePromptDialog");
            new AsyncTask<Void, Void, Boolean>() {
                /* access modifiers changed from: protected */
                public Boolean doInBackground(Void... params) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return Boolean.valueOf(SdkErrorActivity.this.updateSdk());
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(Boolean result) {
                    updateDialog.dismiss();
                    new SdkDialogFragment(result.booleanValue() ? SdkErrorActivity.this.createUpdateSuccessfulDialog() : SdkErrorActivity.this.createUpdateFailedDialog()).show(SdkErrorActivity.this.getFragmentManager(), "SdkUpdateFinishDialog");
                }
            }.execute(new Void[0]);
        }
    };

    class SdkDialogFragment extends DialogFragment {
        private Dialog mDialog;

        public SdkDialogFragment(Dialog dialog) {
            this.mDialog = dialog;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return this.mDialog;
        }
    }

    /* JADX WARNING: type inference failed for: r2v6, types: [java.io.Serializable] */
    /* access modifiers changed from: protected */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onCreate(android.os.Bundle r7) {
        /*
            r6 = this;
            r0 = 16973909(0x1030055, float:2.4061138E-38)
            r6.setTheme(r0)
            super.onCreate(r7)
            java.util.Locale r0 = java.util.Locale.getDefault()
            java.lang.String r0 = r0.getLanguage()
            r6.mLanguage = r0
            r0 = 0
            android.content.Intent r1 = r6.getIntent()
            if (r1 == 0) goto L_0x0023
            java.lang.String r2 = "com.miui.sdk.error"
            java.io.Serializable r2 = r1.getSerializableExtra(r2)
            r0 = r2
            miui.external.SdkConstants$SdkError r0 = (miui.external.SdkConstants.SdkError) r0
        L_0x0023:
            if (r0 != 0) goto L_0x0027
            miui.external.SdkConstants$SdkError r0 = miui.external.SdkConstants.SdkError.GENERIC
        L_0x0027:
            r2 = 0
            int[] r3 = miui.external.SdkErrorActivity.AnonymousClass3.$SwitchMap$miui$external$SdkConstants$SdkError
            int r4 = r0.ordinal()
            r3 = r3[r4]
            switch(r3) {
                case 1: goto L_0x003d;
                case 2: goto L_0x0038;
                default: goto L_0x0033;
            }
        L_0x0033:
            android.app.Dialog r2 = r6.createGenericErrorDialog()
            goto L_0x0042
        L_0x0038:
            android.app.Dialog r2 = r6.createLowSdkVersionDialog()
            goto L_0x0042
        L_0x003d:
            android.app.Dialog r2 = r6.createNoSdkDialog()
        L_0x0042:
            miui.external.SdkErrorActivity$SdkDialogFragment r3 = new miui.external.SdkErrorActivity$SdkDialogFragment
            r3.<init>(r2)
            android.app.FragmentManager r4 = r6.getFragmentManager()
            java.lang.String r5 = "SdkErrorPromptDialog"
            r3.show(r4, r5)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: miui.external.SdkErrorActivity.onCreate(android.os.Bundle):void");
    }

    private Dialog createSingleActionDialog(String title, String message, DialogInterface.OnClickListener listener) {
        return new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(17039370, listener).setIcon(17301543).setCancelable(false).create();
    }

    private Dialog createDoubleActionDialog(String title, String message, DialogInterface.OnClickListener posListener, DialogInterface.OnClickListener negListener) {
        return new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(17039370, posListener).setNegativeButton(17039360, negListener).setIcon(17301543).setCancelable(false).create();
    }

    private Dialog createGenericErrorDialog() {
        String message;
        String title;
        if (Locale.CHINESE.getLanguage().equals(this.mLanguage)) {
            title = "MIUI SDK发生错误";
            message = "请重新安装MIUI SDK再运行本程序。";
        } else {
            title = "MIUI SDK encounter errors";
            message = "Please re-install MIUI SDK and then re-run this application.";
        }
        return createSingleActionDialog(title, message, this.mDismissListener);
    }

    private Dialog createNoSdkDialog() {
        String message;
        String title;
        if (Locale.CHINESE.getLanguage().equals(this.mLanguage)) {
            title = "没有找到MIUI SDK";
            message = "请先安装MIUI SDK再运行本程序。";
        } else {
            title = "MIUI SDK not found";
            message = "Please install MIUI SDK and then re-run this application.";
        }
        return createSingleActionDialog(title, message, this.mDismissListener);
    }

    private Dialog createLowSdkVersionDialog() {
        String message;
        String title;
        String message2;
        String title2;
        if (!supportUpdateSdk()) {
            if (Locale.CHINESE.getLanguage().equals(this.mLanguage)) {
                title2 = "MIUI SDK版本过低";
                message2 = "请先升级MIUI SDK再运行本程序。";
            } else {
                title2 = "MIUI SDK too old";
                message2 = "Please upgrade MIUI SDK and then re-run this application.";
            }
            return createSingleActionDialog(title2, message2, this.mDismissListener);
        }
        if (Locale.CHINESE.getLanguage().equals(this.mLanguage)) {
            title = "MIUI SDK版本过低";
            message = "请先升级MIUI SDK再运行本程序。是否现在升级？";
        } else {
            title = "MIUI SDK too old";
            message = "Please upgrade MIUI SDK and then re-run this application. Upgrade now?";
        }
        return createDoubleActionDialog(title, message, this.mUpdateListener, this.mDismissListener);
    }

    /* access modifiers changed from: private */
    public Dialog createUpdateDialog() {
        String message;
        String title;
        if (Locale.CHINESE.getLanguage().equals(this.mLanguage)) {
            title = "MIUI SDK正在更新";
            message = "请稍候...";
        } else {
            title = "MIUI SDK updating";
            message = "Please wait...";
        }
        return ProgressDialog.show(this, title, message, true, false);
    }

    /* access modifiers changed from: private */
    public Dialog createUpdateSuccessfulDialog() {
        String message;
        String title;
        if (Locale.CHINESE.getLanguage().equals(this.mLanguage)) {
            title = "MIUI SDK更新完成";
            message = "请重新运行本程序。";
        } else {
            title = "MIUI SDK updated";
            message = "Please re-run this application.";
        }
        return createSingleActionDialog(title, message, this.mDismissListener);
    }

    /* access modifiers changed from: private */
    public Dialog createUpdateFailedDialog() {
        String message;
        String title;
        if (Locale.CHINESE.getLanguage().equals(this.mLanguage)) {
            title = "MIUI SDK更新失败";
            message = "请稍后重试。";
        } else {
            title = "MIUI SDK update failed";
            message = "Please try it later.";
        }
        return createSingleActionDialog(title, message, this.mDismissListener);
    }

    private boolean supportUpdateSdk() {
        try {
            return ((Boolean) SdkEntranceHelper.getSdkEntrance().getMethod("supportUpdate", new Class[]{Map.class}).invoke(null, new Object[]{null})).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* access modifiers changed from: private */
    public boolean updateSdk() {
        try {
            Map<String, Object> metaData = new HashMap<>();
            return ((Boolean) SdkEntranceHelper.getSdkEntrance().getMethod("update", new Class[]{Map.class}).invoke(null, new Object[]{metaData})).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
