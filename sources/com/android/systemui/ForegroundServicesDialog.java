package com.android.systemui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.logging.MetricsLogger;
import java.util.ArrayList;

public final class ForegroundServicesDialog extends AlertActivity implements DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, AlertController.AlertParams.OnPrepareListViewListener {
    /* access modifiers changed from: private */
    public PackageItemAdapter mAdapter;
    private DialogInterface.OnClickListener mAppClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            String pkg = ((ApplicationInfo) ForegroundServicesDialog.this.mAdapter.getItem(which)).packageName;
            Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", pkg, null));
            ForegroundServicesDialog.this.startActivity(intent);
            ForegroundServicesDialog.this.finish();
        }
    };
    LayoutInflater mInflater;
    private MetricsLogger mMetricsLogger;
    private String[] mPackages;

    private static class PackageItemAdapter extends ArrayAdapter<ApplicationInfo> {
        final IconDrawableFactory mIconDrawableFactory;
        final LayoutInflater mInflater;
        final PackageManager mPm;

        public PackageItemAdapter(Context context) {
            super(context, R.layout.foreground_service_item);
            this.mPm = context.getPackageManager();
            this.mInflater = LayoutInflater.from(context);
            this.mIconDrawableFactory = IconDrawableFactory.newInstance(context, true);
        }

        public void setPackages(String[] packages) {
            clear();
            ArrayList<ApplicationInfo> apps = new ArrayList<>();
            for (int i = 0; i < packages.length; i++) {
                try {
                    apps.add(this.mPm.getApplicationInfo(packages[i], 4202496));
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            apps.sort(new ApplicationInfo.DisplayNameComparator(this.mPm));
            addAll(apps);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = this.mInflater.inflate(R.layout.foreground_service_item, parent, false);
            } else {
                view = convertView;
            }
            ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(this.mIconDrawableFactory.getBadgedIcon((ApplicationInfo) getItem(position)));
            ((TextView) view.findViewById(R.id.app_name)).setText(((ApplicationInfo) getItem(position)).loadLabel(this.mPm));
            return view;
        }
    }

    /* JADX WARNING: type inference failed for: r4v0, types: [android.content.Context, android.content.DialogInterface$OnClickListener, com.android.internal.app.AlertActivity, com.android.systemui.ForegroundServicesDialog, com.android.internal.app.AlertController$AlertParams$OnPrepareListViewListener, android.widget.AdapterView$OnItemSelectedListener] */
    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        ForegroundServicesDialog.super.onCreate(savedInstanceState);
        DependencyUI.initDependencies(getApplicationContext());
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mInflater = LayoutInflater.from(this);
        this.mAdapter = new PackageItemAdapter(this);
        AlertController.AlertParams p = this.mAlertParams;
        p.mAdapter = this.mAdapter;
        p.mOnClickListener = this.mAppClickListener;
        p.mCustomTitleView = this.mInflater.inflate(R.layout.foreground_service_title, null);
        p.mIsSingleChoice = true;
        p.mOnItemSelectedListener = this;
        p.mPositiveButtonText = getString(17039847);
        p.mPositiveButtonListener = this;
        p.mOnPrepareListViewListener = this;
        updateApps(getIntent());
        if (this.mPackages == null) {
            Log.w("ForegroundServicesDialog", "No packages supplied");
            finish();
            return;
        }
        setupAlert();
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        ForegroundServicesDialog.super.onResume();
        this.mMetricsLogger.visible(944);
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        ForegroundServicesDialog.super.onPause();
        this.mMetricsLogger.hidden(944);
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        ForegroundServicesDialog.super.onNewIntent(intent);
        updateApps(intent);
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        ForegroundServicesDialog.super.onStop();
        if (!isChangingConfigurations()) {
            finish();
        }
    }

    /* access modifiers changed from: package-private */
    public void updateApps(Intent intent) {
        this.mPackages = intent.getStringArrayExtra("packages");
        if (this.mPackages != null) {
            this.mAdapter.setPackages(this.mPackages);
        }
    }

    public void onPrepareListView(ListView listView) {
    }

    public void onClick(DialogInterface dialog, int which) {
        finish();
    }

    public void onItemSelected(AdapterView parent, View view, int position, long id) {
    }

    public void onNothingSelected(AdapterView parent) {
    }
}
