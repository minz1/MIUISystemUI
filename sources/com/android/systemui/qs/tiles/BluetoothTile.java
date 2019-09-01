package com.android.systemui.qs.tiles;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.SystemUICompat;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.BluetoothController;
import java.util.ArrayList;
import java.util.Collection;

public class BluetoothTile extends QSTileImpl<QSTile.BooleanState> {
    /* access modifiers changed from: private */
    public static final Intent BLUETOOTH_SETTINGS = new Intent("android.settings.BLUETOOTH_SETTINGS");
    private final ActivityStarter mActivityStarter = ((ActivityStarter) Dependency.get(ActivityStarter.class));
    private final BluetoothController.Callback mCallback = new BluetoothController.Callback() {
        public void onBluetoothStateChange(boolean enabled) {
            BluetoothTile.this.refreshState();
            if (BluetoothTile.this.isShowingDetail()) {
                BluetoothTile.this.mDetailAdapter.updateItems();
                BluetoothTile.this.fireToggleStateChanged(enabled);
            }
        }

        public void onBluetoothDevicesChanged() {
            if (BluetoothTile.this.isShowingDetail()) {
                BluetoothTile.this.mDetailAdapter.updateItems();
            }
        }

        public void onBluetoothInoutStateChange(String action) {
        }
    };
    /* access modifiers changed from: private */
    public final BluetoothController mController = ((BluetoothController) Dependency.get(BluetoothController.class));
    /* access modifiers changed from: private */
    public final BluetoothDetailAdapter mDetailAdapter = ((BluetoothDetailAdapter) createDetailAdapter());

    protected class BluetoothDetailAdapter implements DetailAdapter, QSDetailItems.Callback {
        private QSDetailItems mItems;

        protected BluetoothDetailAdapter() {
        }

        public CharSequence getTitle() {
            return BluetoothTile.this.mContext.getString(R.string.quick_settings_bluetooth_label);
        }

        public Boolean getToggleState() {
            return Boolean.valueOf(((QSTile.BooleanState) BluetoothTile.this.mState).value);
        }

        public boolean getToggleEnabled() {
            return BluetoothTile.this.mController.getBluetoothState() == 10 || BluetoothTile.this.mController.getBluetoothState() == 12;
        }

        public Intent getSettingsIntent() {
            return BluetoothTile.BLUETOOTH_SETTINGS;
        }

        public void setToggleState(boolean state) {
            MetricsLogger.action(BluetoothTile.this.mContext, 154, state);
            BluetoothTile.this.mController.setBluetoothEnabled(state);
        }

        public int getMetricsCategory() {
            return 150;
        }

        public boolean hasHeader() {
            return true;
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            this.mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            this.mItems.setTagSuffix("Bluetooth");
            this.mItems.setCallback(this);
            if (BluetoothTile.this.isShowingDetail()) {
                updateItems();
            }
            return this.mItems;
        }

        /* access modifiers changed from: private */
        public void updateItems() {
            if (this.mItems != null) {
                if (BluetoothTile.this.mController.isBluetoothEnabled()) {
                    ArrayList<QSDetailItems.Item> items = new ArrayList<>();
                    Collection<CachedBluetoothDevice> devices = BluetoothTile.this.mController.getCachedDevicesCopy();
                    if (devices != null) {
                        int connectedDevices = 0;
                        int count = 0;
                        for (CachedBluetoothDevice device : devices) {
                            QSDetailItems.Item item = new QSDetailItems.Item();
                            item.icon = R.drawable.ic_qs_bluetooth_on;
                            item.line1 = device.getName();
                            item.line2 = BluetoothTile.this.mController.getSummary(device);
                            item.tag = device;
                            BluetoothClass bluetoothClass = device.getBtClass();
                            if (bluetoothClass != null) {
                                if (bluetoothClass.doesClassMatch(0) || bluetoothClass.doesClassMatch(1)) {
                                    item.icon = R.drawable.ic_qs_bluetooth_device_headset;
                                } else {
                                    int majorDeviceClass = bluetoothClass.getMajorDeviceClass();
                                    if (majorDeviceClass == 0) {
                                        item.icon = R.drawable.ic_qs_bluetooth_device_misc;
                                    } else if (majorDeviceClass == 256) {
                                        item.icon = R.drawable.ic_qs_bluetooth_device_laptop;
                                    } else if (majorDeviceClass == 512) {
                                        item.icon = R.drawable.ic_qs_bluetooth_device_cellphone;
                                    } else if (majorDeviceClass == 768) {
                                        item.icon = R.drawable.ic_qs_bluetooth_device_network;
                                    } else if (majorDeviceClass != 1536) {
                                        item.icon = R.drawable.ic_qs_bluetooth_device_common;
                                    } else {
                                        item.icon = R.drawable.ic_qs_bluetooth_device_imaging;
                                    }
                                }
                            }
                            int state = BluetoothTile.this.mController.getMaxConnectionState(device);
                            if (state == 2) {
                                item.icon2 = R.drawable.ic_qs_bluetooth_connected;
                                item.canDisconnect = true;
                                items.add(connectedDevices, item);
                                connectedDevices++;
                            } else if (state == 1) {
                                item.icon2 = R.drawable.ic_qs_bluetooth_connecting;
                                items.add(connectedDevices, item);
                            } else {
                                items.add(item);
                            }
                            count++;
                            if (count == 20) {
                                break;
                            }
                        }
                    }
                    if (items.size() == 0) {
                        this.mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty, R.string.quick_settings_bluetooth_detail_empty_text);
                    }
                    this.mItems.setItems((QSDetailItems.Item[]) items.toArray(new QSDetailItems.Item[items.size()]));
                } else {
                    this.mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty, R.string.bt_is_off);
                    this.mItems.setItems(null);
                }
            }
        }

        public void onDetailItemClick(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                CachedBluetoothDevice device = (CachedBluetoothDevice) item.tag;
                if (device != null) {
                    if (device.isConnected()) {
                        SystemUICompat.setDeviceActive(device);
                    } else {
                        BluetoothTile.this.mController.connect(device);
                    }
                }
            }
        }

        public void onDetailItemDisconnect(QSDetailItems.Item item) {
            if (item != null && item.tag != null) {
                CachedBluetoothDevice device = (CachedBluetoothDevice) item.tag;
                if (device != null) {
                    BluetoothTile.this.mController.disconnect(device);
                }
            }
        }
    }

    public BluetoothTile(QSHost host) {
        super(host);
    }

    public DetailAdapter getDetailAdapter() {
        return this.mDetailAdapter;
    }

    public QSTile.BooleanState newTileState() {
        return new QSTile.BooleanState();
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mController.addCallback(this.mCallback);
        } else {
            this.mController.removeCallback(this.mCallback);
        }
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        String str = this.TAG;
        Log.d(str, "handleClick: from: " + ((QSTile.BooleanState) this.mState).value + ", to: " + (!((QSTile.BooleanState) this.mState).value));
        if (!this.mController.isBluetoothReady() || ((QSTile.BooleanState) this.mState).isTransient) {
            Log.d(this.TAG, "handleClick: bluetooth not ready");
            return;
        }
        boolean isEnabled = ((QSTile.BooleanState) this.mState).value;
        refreshState(isEnabled ? null : ARG_SHOW_TRANSIENT_ENABLING);
        this.mController.setBluetoothEnabled(!isEnabled);
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.BLUETOOTH_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        if (!this.mController.canConfigBluetooth()) {
            this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.settings.BLUETOOTH_SETTINGS"), 0);
            return;
        }
        showDetail(true);
        if (!((QSTile.BooleanState) this.mState).value) {
            this.mController.setBluetoothEnabled(true);
        }
    }

    public CharSequence getTileLabel() {
        return this.mContext.getString(R.string.quick_settings_bluetooth_label);
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        int i;
        boolean transientEnabling = arg == ARG_SHOW_TRANSIENT_ENABLING;
        boolean enabled = transientEnabling || this.mController.isBluetoothEnabled();
        boolean connected = this.mController.isBluetoothConnected();
        state.isTransient = transientEnabling || this.mController.isBluetoothConnecting() || this.mController.getBluetoothState() == 11;
        state.dualTarget = true;
        if (state.value != enabled) {
            state.value = enabled;
        }
        state.label = this.mContext.getString(R.string.quick_settings_bluetooth_label);
        if (!enabled) {
            state.contentDescription = state.label + "," + this.mContext.getString(R.string.switch_bar_off);
        } else if (connected) {
            state.contentDescription = this.mContext.getString(R.string.accessibility_bluetooth_name, new Object[]{state.label});
            state.label = this.mController.getLastDeviceName();
        } else if (state.isTransient) {
            state.contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_connecting);
        } else {
            state.contentDescription = state.label + "," + this.mContext.getString(R.string.switch_bar_on) + "," + this.mContext.getString(R.string.accessibility_not_connected);
        }
        state.state = state.value ? 2 : 1;
        if (state.value) {
            i = R.drawable.ic_qs_bluetooth_on;
        } else {
            i = R.drawable.ic_qs_bluetooth_off;
        }
        state.icon = QSTileImpl.ResourceIcon.get(i);
        state.dualLabelContentDescription = this.mContext.getResources().getString(R.string.accessibility_quick_settings_open_settings, new Object[]{getTileLabel()});
        state.expandedAccessibilityClassName = Switch.class.getName();
    }

    public int getMetricsCategory() {
        return 113;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        if (((QSTile.BooleanState) this.mState).value) {
            return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_on);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_off);
    }

    public boolean isAvailable() {
        return this.mController.isBluetoothSupported();
    }

    /* access modifiers changed from: protected */
    public DetailAdapter createDetailAdapter() {
        return new BluetoothDetailAdapter();
    }
}
