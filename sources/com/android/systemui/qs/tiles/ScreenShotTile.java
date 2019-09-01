package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.text.TextUtils;
import android.widget.Switch;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.io.File;
import java.io.FilenameFilter;
import miui.os.Environment;

public class ScreenShotTile extends QSTileImpl<QSTile.BooleanState> {
    public ScreenShotTile(QSHost host) {
        super(host);
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int newUserId) {
    }

    public Intent getLongClickIntent() {
        return null;
    }

    public boolean isAvailable() {
        return true;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        this.mHost.collapsePanels();
        this.mHandler.post(new Runnable() {
            public void run() {
                if (ScreenShotTile.this.mHost.isQSFullyCollapsed()) {
                    ScreenShotTile.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            ScreenShotTile.this.mContext.sendBroadcastAsUser(new Intent("android.intent.action.CAPTURE_SCREENSHOT"), UserHandle.CURRENT);
                        }
                    }, 300);
                } else {
                    ScreenShotTile.this.mHandler.postDelayed(this, 50);
                }
            }
        });
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_screenshot_label);
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        longClickScreenshot();
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        state.value = false;
        state.state = 1;
        state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_screenshot);
        state.label = this.mHost.getContext().getString(R.string.quick_settings_screenshot_label);
        state.contentDescription = this.mContext.getString(R.string.quick_settings_screenshot_label);
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return -1;
    }

    private boolean longClickScreenshot() {
        String path = null;
        File screenShotFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Screenshots");
        if (screenShotFolder.exists() && screenShotFolder.isDirectory()) {
            File[] files = screenShotFolder.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    String filename2 = filename.toLowerCase();
                    if (filename2.endsWith("png") || filename2.endsWith("jpg") || filename2.endsWith("jpeg")) {
                        return true;
                    }
                    return false;
                }
            });
            if (files == null) {
                return false;
            }
            long maxLastModifed = 0;
            String path2 = null;
            for (File file : files) {
                if (file.lastModified() > maxLastModifed) {
                    maxLastModifed = file.lastModified();
                    path2 = file.getAbsolutePath();
                }
            }
            path = path2;
        }
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        this.mHost.collapsePanels();
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(Uri.fromFile(new File(path)), "image/*");
        intent.setFlags(268435456);
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
        return true;
    }
}
