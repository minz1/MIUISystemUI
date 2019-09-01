package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class NfcTile extends QSTileImpl<QSTile.BooleanState> {
    private NfcAdapter mAdapter;
    private boolean mListening;
    private BroadcastReceiver mNfcReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            NfcTile.this.refreshState();
        }
    };
    private boolean mTransientEnabling;

    public NfcTile(QSHost host) {
        super(host);
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
        this.mListening = listening;
        if (this.mListening) {
            this.mContext.registerReceiverAsUser(this.mNfcReceiver, UserHandle.ALL, new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED"), null, null);
        } else {
            this.mContext.unregisterReceiver(this.mNfcReceiver);
        }
    }

    public boolean isAvailable() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.nfc");
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int newUserId) {
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.NFC_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        NfcAdapter nfcAdapter = getAdapter();
        if (nfcAdapter == null || !isNfcReady(nfcAdapter)) {
            Log.d(this.TAG, "handleClick: not ready, ignore");
            return;
        }
        String str = this.TAG;
        Log.d(str, "handleClick: from: " + ((QSTile.BooleanState) this.mState).value + ", to: " + (!((QSTile.BooleanState) this.mState).value));
        if (((QSTile.BooleanState) this.mState).value) {
            nfcAdapter.disable();
        } else {
            refreshState(ARG_SHOW_TRANSIENT_ENABLING);
            nfcAdapter.enable();
        }
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        handleClick();
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_nfc_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int i;
        boolean z = false;
        int i2 = 1;
        this.mTransientEnabling = arg == ARG_SHOW_TRANSIENT_ENABLING;
        NfcAdapter nfcAdapter = getAdapter();
        if (nfcAdapter != null) {
            boolean isTurningOn = nfcAdapter.getAdapterState() == 2;
            if (this.mTransientEnabling || isTurningOn || nfcAdapter.isEnabled()) {
                z = true;
            }
            state.value = z;
        } else {
            state.value = false;
        }
        if (state.value) {
            i2 = 2;
        }
        state.state = i2;
        state.label = this.mContext.getString(R.string.quick_settings_nfc_label);
        if (state.value) {
            i = R.drawable.ic_qs_nfc_enabled;
        } else {
            i = R.drawable.ic_qs_nfc_disabled;
        }
        state.icon = QSTileImpl.ResourceIcon.get(i);
        state.expandedAccessibilityClassName = Switch.class.getName();
        state.contentDescription = state.label;
    }

    public int getMetricsCategory() {
        return 800;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.quick_settings_nfc_on);
        }
        return this.mContext.getString(R.string.quick_settings_nfc_off);
    }

    private boolean isNfcReady(NfcAdapter nfcAdapter) {
        int state = nfcAdapter.getAdapterState();
        if (state == 2 || state == 4) {
            return false;
        }
        return true;
    }

    private NfcAdapter getAdapter() {
        if (this.mAdapter == null) {
            try {
                this.mAdapter = NfcAdapter.getNfcAdapter(this.mContext);
            } catch (UnsupportedOperationException e) {
                this.mAdapter = null;
            }
        }
        return this.mAdapter;
    }
}
