package com.android.systemui.qs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.SettingsCompat;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.FontUtils;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.SecurityController;
import miui.app.AlertDialog;

public class QSSecurityFooter implements DialogInterface.OnClickListener, View.OnClickListener {
    protected static final boolean DEBUG = Log.isLoggable("QSSecurityFooter", 3);
    /* access modifiers changed from: private */
    public final ActivityStarter mActivityStarter;
    private final Callback mCallback = new Callback();
    private final Context mContext;
    /* access modifiers changed from: private */
    public AlertDialog mDialog;
    /* access modifiers changed from: private */
    public final ImageView mFooterIcon;
    /* access modifiers changed from: private */
    public int mFooterIconId;
    /* access modifiers changed from: private */
    public final TextView mFooterText;
    /* access modifiers changed from: private */
    public CharSequence mFooterTextContent = null;
    protected H mHandler;
    /* access modifiers changed from: private */
    public QSTileHost mHost;
    /* access modifiers changed from: private */
    public boolean mIsVisible;
    private final Handler mMainHandler;
    /* access modifiers changed from: private */
    public final View mRootView;
    private final SecurityController mSecurityController;
    private final Runnable mUpdateDisplayState = new Runnable() {
        public void run() {
            if (QSSecurityFooter.this.mFooterTextContent != null) {
                QSSecurityFooter.this.mFooterText.setText(QSSecurityFooter.this.mFooterTextContent);
            }
            QSSecurityFooter.this.mRootView.setVisibility(QSSecurityFooter.this.mIsVisible ? 0 : 8);
        }
    };
    private final Runnable mUpdateIcon = new Runnable() {
        public void run() {
            QSSecurityFooter.this.mFooterIcon.setImageResource(QSSecurityFooter.this.mFooterIconId);
        }
    };

    private class Callback implements SecurityController.SecurityControllerCallback {
        private Callback() {
        }

        public void onStateChanged() {
            QSSecurityFooter.this.refreshState();
        }
    }

    private class H extends Handler {
        private H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            try {
                if (msg.what == 1) {
                    String name = "handleRefreshState";
                    QSSecurityFooter.this.handleRefreshState();
                } else if (msg.what == 0) {
                    String name2 = "handleClick";
                    QSSecurityFooter.this.handleClick();
                }
            } catch (Throwable t) {
                String error = "Error in " + null;
                Log.w("QSSecurityFooter", error, t);
                QSSecurityFooter.this.mHost.warn(error, t);
            }
        }
    }

    protected class VpnSpan extends ClickableSpan {
        protected VpnSpan() {
        }

        public void onClick(View widget) {
            Intent intent = new Intent(SettingsCompat.ACTION_VPN_SETTINGS);
            QSSecurityFooter.this.mDialog.dismiss();
            QSSecurityFooter.this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        }

        public boolean equals(Object object) {
            return object instanceof VpnSpan;
        }

        public int hashCode() {
            return 314159257;
        }
    }

    public QSSecurityFooter(QSPanel qsPanel, Context context) {
        this.mRootView = LayoutInflater.from(context).inflate(R.layout.quick_settings_footer, qsPanel, false);
        this.mRootView.setOnClickListener(this);
        this.mFooterText = (TextView) this.mRootView.findViewById(R.id.footer_text);
        this.mFooterIcon = (ImageView) this.mRootView.findViewById(R.id.footer_icon);
        this.mFooterIconId = R.drawable.ic_info_outline;
        this.mContext = context;
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mSecurityController = (SecurityController) Dependency.get(SecurityController.class);
        this.mHandler = new H((Looper) Dependency.get(Dependency.BG_LOOPER));
    }

    public void setHostEnvironment(QSTileHost host) {
        this.mHost = host;
    }

    public void setListening(boolean listening) {
        if (listening) {
            this.mSecurityController.addCallback(this.mCallback);
        } else {
            this.mSecurityController.removeCallback(this.mCallback);
        }
    }

    public void onConfigurationChanged() {
        FontUtils.updateFontSize(this.mFooterText, R.dimen.qs_tile_label_text_size);
    }

    public View getView() {
        return this.mRootView;
    }

    public void onClick(View v) {
        this.mHandler.sendEmptyMessage(0);
    }

    /* access modifiers changed from: private */
    public void handleClick() {
        showDeviceMonitoringDialog();
    }

    public void showDeviceMonitoringDialog() {
        this.mHost.collapsePanels();
        createDialog();
    }

    public void refreshState() {
        this.mHandler.sendEmptyMessage(1);
    }

    /* access modifiers changed from: private */
    public void handleRefreshState() {
        int footerIconId;
        boolean isDeviceManaged = this.mSecurityController.isDeviceManaged();
        boolean hasWorkProfile = this.mSecurityController.hasWorkProfile();
        boolean hasCACerts = this.mSecurityController.hasCACertInCurrentUser();
        boolean hasCACertsInWorkProfile = this.mSecurityController.hasCACertInWorkProfile();
        boolean isNetworkLoggingEnabled = this.mSecurityController.isNetworkLoggingEnabled();
        String vpnName = this.mSecurityController.getPrimaryVpnName();
        String vpnNameWorkProfile = this.mSecurityController.getWorkProfileVpnName();
        CharSequence organizationName = this.mSecurityController.getDeviceOwnerOrganizationName();
        CharSequence workProfileName = this.mSecurityController.getWorkProfileOrganizationName();
        this.mIsVisible = (isDeviceManaged || hasCACerts || hasCACertsInWorkProfile || vpnName != null || vpnNameWorkProfile != null) && !this.mSecurityController.isSilentVpnPackage();
        this.mFooterTextContent = getFooterText(isDeviceManaged, hasWorkProfile, hasCACerts, hasCACertsInWorkProfile, isNetworkLoggingEnabled, vpnName, vpnNameWorkProfile, organizationName, workProfileName);
        if (vpnName == null && vpnNameWorkProfile == null) {
            footerIconId = R.drawable.ic_info_outline;
        } else {
            footerIconId = R.drawable.ic_qs_vpn;
        }
        if (this.mFooterIconId != footerIconId) {
            this.mFooterIconId = footerIconId;
            this.mMainHandler.post(this.mUpdateIcon);
        }
        this.mMainHandler.post(this.mUpdateDisplayState);
    }

    /* access modifiers changed from: protected */
    public CharSequence getFooterText(boolean isDeviceManaged, boolean hasWorkProfile, boolean hasCACerts, boolean hasCACertsInWorkProfile, boolean isNetworkLoggingEnabled, String vpnName, String vpnNameWorkProfile, CharSequence organizationName, CharSequence workProfileName) {
        if (isDeviceManaged) {
            if (hasCACerts || hasCACertsInWorkProfile || isNetworkLoggingEnabled) {
                if (organizationName == null) {
                    return this.mContext.getString(R.string.quick_settings_disclosure_management_monitoring);
                }
                return this.mContext.getString(R.string.quick_settings_disclosure_named_management_monitoring, new Object[]{organizationName});
            } else if (vpnName == null || vpnNameWorkProfile == null) {
                if (vpnName == null && vpnNameWorkProfile == null) {
                    if (organizationName == null) {
                        return this.mContext.getString(R.string.quick_settings_disclosure_management);
                    }
                    return this.mContext.getString(R.string.quick_settings_disclosure_named_management, new Object[]{organizationName});
                } else if (organizationName == null) {
                    Context context = this.mContext;
                    Object[] objArr = new Object[1];
                    objArr[0] = vpnName != null ? vpnName : vpnNameWorkProfile;
                    return context.getString(R.string.quick_settings_disclosure_management_named_vpn, objArr);
                } else {
                    Context context2 = this.mContext;
                    Object[] objArr2 = new Object[2];
                    objArr2[0] = organizationName;
                    objArr2[1] = vpnName != null ? vpnName : vpnNameWorkProfile;
                    return context2.getString(R.string.quick_settings_disclosure_named_management_named_vpn, objArr2);
                }
            } else if (organizationName == null) {
                return this.mContext.getString(R.string.quick_settings_disclosure_management_vpns);
            } else {
                return this.mContext.getString(R.string.quick_settings_disclosure_named_management_vpns, new Object[]{organizationName});
            }
        } else if (hasCACertsInWorkProfile) {
            if (workProfileName == null) {
                return this.mContext.getString(R.string.quick_settings_disclosure_managed_profile_monitoring);
            }
            return this.mContext.getString(R.string.quick_settings_disclosure_named_managed_profile_monitoring, new Object[]{workProfileName});
        } else if (hasCACerts) {
            return this.mContext.getString(R.string.quick_settings_disclosure_monitoring);
        } else {
            if (vpnName != null && vpnNameWorkProfile != null) {
                return this.mContext.getString(R.string.quick_settings_disclosure_vpns);
            }
            if (vpnNameWorkProfile != null) {
                return this.mContext.getString(R.string.quick_settings_disclosure_managed_profile_named_vpn, new Object[]{vpnNameWorkProfile});
            } else if (vpnName == null) {
                return null;
            } else {
                if (hasWorkProfile) {
                    return this.mContext.getString(R.string.quick_settings_disclosure_personal_profile_named_vpn, new Object[]{vpnName});
                }
                return this.mContext.getString(R.string.quick_settings_disclosure_named_vpn, new Object[]{vpnName});
            }
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -2) {
            Intent intent = new Intent("android.settings.ENTERPRISE_PRIVACY_SETTINGS");
            this.mDialog.dismiss();
            this.mActivityStarter.postStartActivityDismissingKeyguard(intent, 0);
        }
    }

    private void createDialog() {
        boolean isDeviceManaged = this.mSecurityController.isDeviceManaged();
        boolean hasWorkProfile = this.mSecurityController.hasWorkProfile();
        CharSequence deviceOwnerOrganization = this.mSecurityController.getDeviceOwnerOrganizationName();
        boolean hasCACerts = this.mSecurityController.hasCACertInCurrentUser();
        boolean hasCACertsInWorkProfile = this.mSecurityController.hasCACertInWorkProfile();
        boolean isNetworkLoggingEnabled = this.mSecurityController.isNetworkLoggingEnabled();
        String vpnName = this.mSecurityController.getPrimaryVpnName();
        String vpnNameWorkProfile = this.mSecurityController.getWorkProfileVpnName();
        this.mDialog = new SystemUIDialog(this.mContext);
        this.mDialog.requestWindowFeature(1);
        View dialogView = LayoutInflater.from(this.mContext).inflate(R.layout.quick_settings_footer_dialog, null, false);
        this.mDialog.setView(dialogView);
        this.mDialog.setButton(-1, getPositiveButton(), this);
        CharSequence managementMessage = getManagementMessage(isDeviceManaged, deviceOwnerOrganization);
        if (managementMessage == null) {
            dialogView.findViewById(R.id.device_management_disclosures).setVisibility(8);
        } else {
            dialogView.findViewById(R.id.device_management_disclosures).setVisibility(0);
            ((TextView) dialogView.findViewById(R.id.device_management_warning)).setText(managementMessage);
            this.mDialog.setButton(-2, getSettingsButton(), this);
        }
        CharSequence caCertsMessage = getCaCertsMessage(isDeviceManaged, hasCACerts, hasCACertsInWorkProfile);
        if (caCertsMessage == null) {
            dialogView.findViewById(R.id.ca_certs_disclosures).setVisibility(8);
        } else {
            dialogView.findViewById(R.id.ca_certs_disclosures).setVisibility(0);
            TextView caCertsWarning = (TextView) dialogView.findViewById(R.id.ca_certs_warning);
            caCertsWarning.setText(caCertsMessage);
            caCertsWarning.setMovementMethod(new LinkMovementMethod());
        }
        CharSequence networkLoggingMessage = getNetworkLoggingMessage(isNetworkLoggingEnabled);
        if (networkLoggingMessage == null) {
            dialogView.findViewById(R.id.network_logging_disclosures).setVisibility(8);
        } else {
            dialogView.findViewById(R.id.network_logging_disclosures).setVisibility(0);
            ((TextView) dialogView.findViewById(R.id.network_logging_warning)).setText(networkLoggingMessage);
        }
        CharSequence vpnMessage = getVpnMessage(isDeviceManaged, hasWorkProfile, vpnName, vpnNameWorkProfile);
        if (vpnMessage == null) {
            dialogView.findViewById(R.id.vpn_disclosures).setVisibility(8);
        } else {
            dialogView.findViewById(R.id.vpn_disclosures).setVisibility(0);
            TextView vpnWarning = (TextView) dialogView.findViewById(R.id.vpn_warning);
            vpnWarning.setText(vpnMessage);
            vpnWarning.setMovementMethod(new LinkMovementMethod());
        }
        this.mDialog.show();
        this.mDialog.getWindow().setLayout(-1, -2);
    }

    private String getSettingsButton() {
        return this.mContext.getString(R.string.monitoring_button_view_policies);
    }

    private String getPositiveButton() {
        return this.mContext.getString(R.string.quick_settings_done);
    }

    /* access modifiers changed from: protected */
    public CharSequence getManagementMessage(boolean isDeviceManaged, CharSequence organizationName) {
        if (!isDeviceManaged) {
            return null;
        }
        if (organizationName == null) {
            return this.mContext.getString(R.string.monitoring_description_management);
        }
        return this.mContext.getString(R.string.monitoring_description_named_management, new Object[]{organizationName});
    }

    /* access modifiers changed from: protected */
    public CharSequence getCaCertsMessage(boolean isDeviceManaged, boolean hasCACerts, boolean hasCACertsInWorkProfile) {
        if (!hasCACerts && !hasCACertsInWorkProfile) {
            return null;
        }
        if (isDeviceManaged) {
            return this.mContext.getString(R.string.monitoring_description_management_ca_certificate);
        }
        if (hasCACertsInWorkProfile) {
            return this.mContext.getString(R.string.monitoring_description_managed_profile_ca_certificate);
        }
        return this.mContext.getString(R.string.monitoring_description_ca_certificate);
    }

    /* access modifiers changed from: protected */
    public CharSequence getNetworkLoggingMessage(boolean isNetworkLoggingEnabled) {
        if (!isNetworkLoggingEnabled) {
            return null;
        }
        return this.mContext.getString(R.string.monitoring_description_management_network_logging);
    }

    /* access modifiers changed from: protected */
    public CharSequence getVpnMessage(boolean isDeviceManaged, boolean hasWorkProfile, String vpnName, String vpnNameWorkProfile) {
        if (vpnName == null && vpnNameWorkProfile == null) {
            return null;
        }
        SpannableStringBuilder message = new SpannableStringBuilder();
        if (isDeviceManaged) {
            if (vpnName == null || vpnNameWorkProfile == null) {
                Context context = this.mContext;
                Object[] objArr = new Object[1];
                objArr[0] = vpnName != null ? vpnName : vpnNameWorkProfile;
                message.append(context.getString(R.string.monitoring_description_named_vpn, objArr));
            } else {
                message.append(this.mContext.getString(R.string.monitoring_description_two_named_vpns, new Object[]{vpnName, vpnNameWorkProfile}));
            }
        } else if (vpnName != null && vpnNameWorkProfile != null) {
            message.append(this.mContext.getString(R.string.monitoring_description_two_named_vpns, new Object[]{vpnName, vpnNameWorkProfile}));
        } else if (vpnNameWorkProfile != null) {
            message.append(this.mContext.getString(R.string.monitoring_description_managed_profile_named_vpn, new Object[]{vpnNameWorkProfile}));
        } else if (hasWorkProfile) {
            message.append(this.mContext.getString(R.string.monitoring_description_personal_profile_named_vpn, new Object[]{vpnName}));
        } else {
            message.append(this.mContext.getString(R.string.monitoring_description_named_vpn, new Object[]{vpnName}));
        }
        message.append(this.mContext.getString(R.string.monitoring_description_vpn_settings_separator));
        message.append(this.mContext.getString(R.string.monitoring_description_vpn_settings), new VpnSpan(), 0);
        return message;
    }
}
