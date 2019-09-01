package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.R;
import com.android.settingslib.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {
    private final AudioManager mAudioManager;
    private BluetoothClass mBtClass;
    private final Collection<Callback> mCallbacks = new ArrayList();
    private long mConnectAttempted;
    private final Context mContext;
    private final BluetoothDevice mDevice;
    private long mHiSyncId;
    private boolean mIsActiveDeviceA2dp = false;
    private boolean mIsActiveDeviceHeadset = false;
    private boolean mIsActiveDeviceHearingAid = false;
    private boolean mIsConnectingErrorPossible;
    private boolean mJustDiscovered;
    private final LocalBluetoothAdapter mLocalAdapter;
    private boolean mLocalNapRoleConnected;
    private int mMessageRejectionCount;
    private String mName;
    private HashMap<LocalBluetoothProfile, Integer> mProfileConnectionState;
    private final LocalBluetoothProfileManager mProfileManager;
    private final List<LocalBluetoothProfile> mProfiles = new CopyOnWriteArrayList();
    private final List<LocalBluetoothProfile> mRemovedProfiles = new ArrayList();
    private short mRssi;

    public interface Callback {
        void onDeviceAttributesChanged();
    }

    public long getHiSyncId() {
        return this.mHiSyncId;
    }

    public void setHiSyncId(long id) {
        Log.d("CachedBluetoothDevice", "setHiSyncId: mDevice " + this.mDevice + ", id " + id);
        this.mHiSyncId = id;
    }

    private BluetoothDevice getTwsPeerDevice() {
        if (this.mDevice.isTwsPlusDevice()) {
            return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(this.mDevice.getTwsPlusPeerAddress());
        }
        return null;
    }

    private String describe(LocalBluetoothProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Address:");
        sb.append(this.mDevice);
        if (profile != null) {
            sb.append(" Profile:");
            sb.append(profile);
        }
        return sb.toString();
    }

    public void onProfileStateChanged(LocalBluetoothProfile profile, int newProfileState) {
        Log.d("CachedBluetoothDevice", "onProfileStateChanged: profile " + profile + " newProfileState " + newProfileState);
        if (this.mLocalAdapter.getBluetoothState() == 13) {
            Log.d("CachedBluetoothDevice", " BT Turninig Off...Profile conn state change ignored...");
            return;
        }
        this.mProfileConnectionState.put(profile, Integer.valueOf(newProfileState));
        if (newProfileState == 2) {
            if (profile instanceof MapProfile) {
                profile.setPreferred(this.mDevice, true);
            }
            if (!this.mProfiles.contains(profile)) {
                this.mRemovedProfiles.remove(profile);
                this.mProfiles.add(profile);
                if ((profile instanceof PanProfile) && ((PanProfile) profile).isLocalRoleNap(this.mDevice)) {
                    this.mLocalNapRoleConnected = true;
                }
            }
        } else if ((profile instanceof MapProfile) && newProfileState == 0) {
            profile.setPreferred(this.mDevice, false);
        } else if (this.mLocalNapRoleConnected && (profile instanceof PanProfile) && ((PanProfile) profile).isLocalRoleNap(this.mDevice) && newProfileState == 0) {
            Log.d("CachedBluetoothDevice", "Removing PanProfile from device after NAP disconnect");
            this.mProfiles.remove(profile);
            this.mRemovedProfiles.add(profile);
            this.mLocalNapRoleConnected = false;
        }
        fetchActiveDevices();
    }

    CachedBluetoothDevice(Context context, LocalBluetoothAdapter adapter, LocalBluetoothProfileManager profileManager, BluetoothDevice device) {
        this.mContext = context;
        this.mLocalAdapter = adapter;
        this.mProfileManager = profileManager;
        this.mAudioManager = (AudioManager) context.getSystemService(AudioManager.class);
        this.mDevice = device;
        this.mProfileConnectionState = new HashMap<>();
        fillData();
        this.mHiSyncId = 0;
    }

    public void disconnect() {
        for (LocalBluetoothProfile profile : this.mProfiles) {
            disconnect(profile);
        }
        PbapServerProfile PbapProfile = this.mProfileManager.getPbapProfile();
        if (PbapProfile.getConnectionStatus(this.mDevice) == 2) {
            PbapProfile.disconnect(this.mDevice);
        }
    }

    public void disconnect(LocalBluetoothProfile profile) {
        if (profile.disconnect(this.mDevice)) {
            Log.d("CachedBluetoothDevice", "Command sent successfully:DISCONNECT " + describe(profile));
        }
    }

    public void connect(boolean connectAllProfiles) {
        if (ensurePaired()) {
            this.mConnectAttempted = SystemClock.elapsedRealtime();
            connectWithoutResettingTimer(connectAllProfiles);
        }
    }

    /* access modifiers changed from: package-private */
    public void onBondingDockConnect() {
        connect(false);
    }

    private void connectWithoutResettingTimer(boolean connectAllProfiles) {
        if (this.mProfiles.isEmpty()) {
            Log.d("CachedBluetoothDevice", "No profiles. Maybe we will connect later");
            return;
        }
        this.mIsConnectingErrorPossible = true;
        int preferredProfiles = 0;
        for (LocalBluetoothProfile profile : this.mProfiles) {
            if (connectAllProfiles) {
                if (!profile.isConnectable()) {
                }
            } else if (!profile.isAutoConnectable()) {
            }
            if (profile.isPreferred(this.mDevice)) {
                preferredProfiles++;
                connectInt(profile);
            }
        }
        if (preferredProfiles == 0) {
            connectAutoConnectableProfiles();
        }
    }

    private void connectAutoConnectableProfiles() {
        if (ensurePaired()) {
            this.mIsConnectingErrorPossible = true;
            for (LocalBluetoothProfile profile : this.mProfiles) {
                if (profile.isAutoConnectable()) {
                    profile.setPreferred(this.mDevice, true);
                    connectInt(profile);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public synchronized void connectInt(LocalBluetoothProfile profile) {
        if (ensurePaired()) {
            if (profile.connect(this.mDevice)) {
                Log.d("CachedBluetoothDevice", "Command sent successfully:CONNECT " + describe(profile));
                return;
            }
            Log.i("CachedBluetoothDevice", "Failed to connect " + profile.toString() + " to " + this.mName);
        }
    }

    private boolean ensurePaired() {
        if (getBondState() != 10) {
            return true;
        }
        startPairing();
        return false;
    }

    public boolean startPairing() {
        if (this.mLocalAdapter.isDiscovering()) {
            this.mLocalAdapter.cancelDiscovery();
        }
        if (!this.mDevice.createBond()) {
            return false;
        }
        return true;
    }

    public void unpair() {
        int state = getBondState();
        if (state == 11) {
            this.mDevice.cancelBondProcess();
        }
        if (state != 10) {
            BluetoothDevice dev = this.mDevice;
            if (this.mDevice.isTwsPlusDevice()) {
                BluetoothDevice peerDevice = getTwsPeerDevice();
                if (peerDevice != null && peerDevice.removeBond()) {
                    Log.d("CachedBluetoothDevice", "Command sent successfully:REMOVE_BOND " + peerDevice.getName());
                }
            }
            if (dev != null && dev.removeBond()) {
                Log.d("CachedBluetoothDevice", "Command sent successfully:REMOVE_BOND " + describe(null));
            }
        }
    }

    public int getProfileConnectionState(LocalBluetoothProfile profile) {
        if (this.mProfileConnectionState == null || this.mProfileConnectionState.get(profile) == null) {
            this.mProfileConnectionState.put(profile, Integer.valueOf(profile.getConnectionStatus(this.mDevice)));
        }
        return this.mProfileConnectionState.get(profile).intValue();
    }

    public void clearProfileConnectionState() {
        Log.d("CachedBluetoothDevice", " Clearing all connection state for dev:" + this.mDevice.getName());
        for (LocalBluetoothProfile profile : getProfiles()) {
            this.mProfileConnectionState.put(profile, 0);
        }
    }

    private void fillData() {
        fetchName();
        fetchBtClass();
        updateProfiles();
        fetchActiveDevices();
        migratePhonebookPermissionChoice();
        migrateMessagePermissionChoice();
        fetchMessageRejectionCount();
        dispatchAttributesChanged();
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public String getName() {
        return this.mName;
    }

    /* access modifiers changed from: package-private */
    public void setNewName(String name) {
        if (this.mName == null) {
            this.mName = name;
            if (this.mName == null || TextUtils.isEmpty(this.mName)) {
                this.mName = this.mDevice.getAddress();
            }
            dispatchAttributesChanged();
        }
    }

    public boolean setActive() {
        boolean result = false;
        A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
        if (a2dpProfile != null && isConnectedProfile(a2dpProfile) && a2dpProfile.setActiveDevice(getDevice())) {
            Log.i("CachedBluetoothDevice", "OnPreferenceClickListener: A2DP active device=" + this);
            result = true;
        }
        HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
        if (headsetProfile != null && isConnectedProfile(headsetProfile) && headsetProfile.setActiveDevice(getDevice())) {
            Log.i("CachedBluetoothDevice", "OnPreferenceClickListener: Headset active device=" + this);
            result = true;
        }
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        if (hearingAidProfile == null || !isConnectedProfile(hearingAidProfile) || !hearingAidProfile.setActiveDevice(getDevice())) {
            return result;
        }
        Log.i("CachedBluetoothDevice", "OnPreferenceClickListener: Hearing Aid active device=" + this);
        return true;
    }

    /* access modifiers changed from: package-private */
    public void refreshName() {
        fetchName();
        dispatchAttributesChanged();
    }

    private void fetchName() {
        this.mName = this.mDevice.getAliasName();
        if (TextUtils.isEmpty(this.mName)) {
            this.mName = this.mDevice.getAddress();
        }
    }

    public int getBatteryLevel() {
        return this.mDevice.getBatteryLevel();
    }

    public void refresh() {
        dispatchAttributesChanged();
    }

    public void setJustDiscovered(boolean justDiscovered) {
        if (this.mJustDiscovered != justDiscovered) {
            this.mJustDiscovered = justDiscovered;
            dispatchAttributesChanged();
        }
    }

    public int getBondState() {
        return this.mDevice.getBondState();
    }

    public void onActiveDeviceChanged(boolean isActive, int bluetoothProfile) {
        boolean changed = false;
        boolean z = false;
        if (bluetoothProfile != 21) {
            switch (bluetoothProfile) {
                case 1:
                    if (this.mIsActiveDeviceHeadset != isActive) {
                        z = true;
                    }
                    changed = z;
                    this.mIsActiveDeviceHeadset = isActive;
                    break;
                case 2:
                    if (this.mIsActiveDeviceA2dp != isActive) {
                        z = true;
                    }
                    changed = z;
                    this.mIsActiveDeviceA2dp = isActive;
                    break;
                default:
                    Log.w("CachedBluetoothDevice", "onActiveDeviceChanged: unknown profile " + bluetoothProfile + " isActive " + isActive);
                    break;
            }
        } else {
            if (this.mIsActiveDeviceHearingAid != isActive) {
                z = true;
            }
            changed = z;
            this.mIsActiveDeviceHearingAid = isActive;
        }
        if (changed) {
            dispatchAttributesChanged();
        }
    }

    /* access modifiers changed from: package-private */
    public void onAudioModeChanged() {
        dispatchAttributesChanged();
    }

    public boolean isActiveDevice(int bluetoothProfile) {
        if (bluetoothProfile == 21) {
            return this.mIsActiveDeviceHearingAid;
        }
        switch (bluetoothProfile) {
            case 1:
                return this.mIsActiveDeviceHeadset;
            case 2:
                return this.mIsActiveDeviceA2dp;
            default:
                Log.w("CachedBluetoothDevice", "getActiveDevice: unknown profile " + bluetoothProfile);
                return false;
        }
    }

    /* access modifiers changed from: package-private */
    public void setRssi(short rssi) {
        if (this.mRssi != rssi) {
            this.mRssi = rssi;
            dispatchAttributesChanged();
        }
    }

    public boolean isConnected() {
        for (LocalBluetoothProfile profile : this.mProfiles) {
            if (getProfileConnectionState(profile) == 2) {
                return true;
            }
        }
        return false;
    }

    public boolean isConnectedProfile(LocalBluetoothProfile profile) {
        return getProfileConnectionState(profile) == 2;
    }

    private void fetchBtClass() {
        this.mBtClass = this.mDevice.getBluetoothClass();
    }

    private boolean updateProfiles() {
        ParcelUuid[] uuids = this.mDevice.getUuids();
        if (uuids == null) {
            return false;
        }
        ParcelUuid[] localUuids = this.mLocalAdapter.getUuids();
        if (localUuids == null) {
            return false;
        }
        processPhonebookAccess();
        this.mProfileManager.updateProfiles(uuids, localUuids, this.mProfiles, this.mRemovedProfiles, this.mLocalNapRoleConnected, this.mDevice);
        return true;
    }

    private void fetchActiveDevices() {
        A2dpProfile a2dpProfile = this.mProfileManager.getA2dpProfile();
        if (a2dpProfile != null) {
            this.mIsActiveDeviceA2dp = this.mDevice.equals(a2dpProfile.getActiveDevice());
        }
        HeadsetProfile headsetProfile = this.mProfileManager.getHeadsetProfile();
        if (headsetProfile != null) {
            this.mIsActiveDeviceHeadset = this.mDevice.equals(headsetProfile.getActiveDevice());
        }
        HearingAidProfile hearingAidProfile = this.mProfileManager.getHearingAidProfile();
        if (hearingAidProfile != null) {
            this.mIsActiveDeviceHearingAid = hearingAidProfile.getActiveDevices().contains(this.mDevice);
        }
    }

    /* access modifiers changed from: package-private */
    public void refreshBtClass() {
        fetchBtClass();
        dispatchAttributesChanged();
    }

    /* access modifiers changed from: package-private */
    public void onUuidChanged() {
        updateProfiles();
        long timeout = 5000;
        if (BluetoothUuid.isUuidPresent(this.mDevice.getUuids(), BluetoothUuid.Hogp)) {
            timeout = 30000;
        }
        if (!this.mProfiles.isEmpty() && this.mConnectAttempted + timeout > SystemClock.elapsedRealtime()) {
            connectWithoutResettingTimer(false);
        }
        dispatchAttributesChanged();
    }

    /* access modifiers changed from: package-private */
    public void onBondingStateChanged(int bondState) {
        if (bondState == 10) {
            this.mProfiles.clear();
            setPhonebookPermissionChoice(0);
            setMessagePermissionChoice(0);
            setSimPermissionChoice(0);
            this.mMessageRejectionCount = 0;
            saveMessageRejectionCount();
        }
        refresh();
        if (bondState != 12) {
            return;
        }
        if (this.mDevice.isBluetoothDock()) {
            onBondingDockConnect();
        } else if (SystemProperties.getBoolean("persist.vendor.btstack.connect.peer_earbud", true)) {
            Log.d("CachedBluetoothDevice", "Initiating connection to" + this.mDevice);
            if (this.mDevice.isBondingInitiatedLocally() || this.mDevice.isTwsPlusDevice()) {
                connect(false);
            }
        } else if (this.mDevice.isBondingInitiatedLocally()) {
            connect(false);
        }
    }

    /* access modifiers changed from: package-private */
    public void setBtClass(BluetoothClass btClass) {
        if (btClass != null && this.mBtClass != btClass) {
            this.mBtClass = btClass;
            dispatchAttributesChanged();
        }
    }

    public BluetoothClass getBtClass() {
        return this.mBtClass;
    }

    public List<LocalBluetoothProfile> getProfiles() {
        return Collections.unmodifiableList(this.mProfiles);
    }

    public void registerCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(callback);
        }
    }

    private void dispatchAttributesChanged() {
        synchronized (this.mCallbacks) {
            for (Callback callback : this.mCallbacks) {
                callback.onDeviceAttributesChanged();
            }
        }
    }

    public String toString() {
        return this.mDevice.toString();
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CachedBluetoothDevice)) {
            return false;
        }
        return this.mDevice.equals(((CachedBluetoothDevice) o).mDevice);
    }

    public int hashCode() {
        return this.mDevice.getAddress().hashCode();
    }

    public int compareTo(CachedBluetoothDevice another) {
        int comparison = (another.isConnected() ? 1 : 0) - (isConnected() ? 1 : 0);
        if (comparison != 0) {
            return comparison;
        }
        int i = 0;
        int i2 = another.getBondState() == 12 ? 1 : 0;
        if (getBondState() == 12) {
            i = 1;
        }
        int comparison2 = i2 - i;
        if (comparison2 != 0) {
            return comparison2;
        }
        int comparison3 = (another.mJustDiscovered ? 1 : 0) - (this.mJustDiscovered ? 1 : 0);
        if (comparison3 != 0) {
            return comparison3;
        }
        int comparison4 = another.mRssi - this.mRssi;
        if (comparison4 != 0) {
            return comparison4;
        }
        return this.mName.compareTo(another.mName);
    }

    public int getPhonebookPermissionChoice() {
        int permission = this.mDevice.getPhonebookAccessPermission();
        if (permission == 1) {
            return 1;
        }
        if (permission == 2) {
            return 2;
        }
        return 0;
    }

    public void setPhonebookPermissionChoice(int permissionChoice) {
        int permission = 0;
        if (permissionChoice == 1) {
            permission = 1;
        } else if (permissionChoice == 2) {
            permission = 2;
        }
        this.mDevice.setPhonebookAccessPermission(permission);
    }

    private void migratePhonebookPermissionChoice() {
        SharedPreferences preferences = this.mContext.getSharedPreferences("bluetooth_phonebook_permission", 0);
        if (preferences.contains(this.mDevice.getAddress())) {
            if (this.mDevice.getPhonebookAccessPermission() == 0) {
                int oldPermission = preferences.getInt(this.mDevice.getAddress(), 0);
                if (oldPermission == 1) {
                    this.mDevice.setPhonebookAccessPermission(1);
                } else if (oldPermission == 2) {
                    this.mDevice.setPhonebookAccessPermission(2);
                }
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(this.mDevice.getAddress());
            editor.commit();
        }
    }

    public void setMessagePermissionChoice(int permissionChoice) {
        int permission = 0;
        if (permissionChoice == 1) {
            permission = 1;
        } else if (permissionChoice == 2) {
            permission = 2;
        }
        this.mDevice.setMessageAccessPermission(permission);
    }

    /* access modifiers changed from: package-private */
    public void setSimPermissionChoice(int permissionChoice) {
        int permission = 0;
        if (permissionChoice == 1) {
            permission = 1;
        } else if (permissionChoice == 2) {
            permission = 2;
        }
        this.mDevice.setSimAccessPermission(permission);
    }

    private void migrateMessagePermissionChoice() {
        SharedPreferences preferences = this.mContext.getSharedPreferences("bluetooth_message_permission", 0);
        if (preferences.contains(this.mDevice.getAddress())) {
            if (this.mDevice.getMessageAccessPermission() == 0) {
                int oldPermission = preferences.getInt(this.mDevice.getAddress(), 0);
                if (oldPermission == 1) {
                    this.mDevice.setMessageAccessPermission(1);
                } else if (oldPermission == 2) {
                    this.mDevice.setMessageAccessPermission(2);
                }
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(this.mDevice.getAddress());
            editor.commit();
        }
    }

    private void fetchMessageRejectionCount() {
        this.mMessageRejectionCount = this.mContext.getSharedPreferences("bluetooth_message_reject", 0).getInt(this.mDevice.getAddress(), 0);
    }

    private void saveMessageRejectionCount() {
        SharedPreferences.Editor editor = this.mContext.getSharedPreferences("bluetooth_message_reject", 0).edit();
        if (this.mMessageRejectionCount == 0) {
            editor.remove(this.mDevice.getAddress());
        } else {
            editor.putInt(this.mDevice.getAddress(), this.mMessageRejectionCount);
        }
        editor.commit();
    }

    private void processPhonebookAccess() {
        if (this.mDevice.getBondState() == 12 && BluetoothUuid.containsAnyUuid(this.mDevice.getUuids(), PbapServerProfile.PBAB_CLIENT_UUIDS) && getPhonebookPermissionChoice() == 0) {
            if (this.mDevice.getBluetoothClass() == null || !(this.mDevice.getBluetoothClass().getDeviceClass() == 1032 || this.mDevice.getBluetoothClass().getDeviceClass() == 1028)) {
                setPhonebookPermissionChoice(2);
            } else {
                setPhonebookPermissionChoice(1);
            }
        }
    }

    public int getMaxConnectionState() {
        int maxState = 0;
        for (LocalBluetoothProfile profile : getProfiles()) {
            int connectionStatus = getProfileConnectionState(profile);
            if (connectionStatus > maxState) {
                maxState = connectionStatus;
            }
        }
        return maxState;
    }

    public String getConnectionSummary() {
        boolean profileConnected = false;
        boolean a2dpNotConnected = false;
        boolean hfpNotConnected = false;
        boolean hearingAidNotConnected = false;
        for (LocalBluetoothProfile profile : getProfiles()) {
            int connectionStatus = getProfileConnectionState(profile);
            switch (connectionStatus) {
                case 0:
                    if (profile.isProfileReady()) {
                        if (!(profile instanceof A2dpProfile) && !(profile instanceof A2dpSinkProfile)) {
                            if (!(profile instanceof HeadsetProfile) && !(profile instanceof HfpClientProfile)) {
                                if (!(profile instanceof HearingAidProfile)) {
                                    break;
                                } else {
                                    hearingAidNotConnected = true;
                                    break;
                                }
                            } else {
                                hfpNotConnected = true;
                                break;
                            }
                        } else {
                            a2dpNotConnected = true;
                            break;
                        }
                    } else {
                        break;
                    }
                case 1:
                case 3:
                    return this.mContext.getString(Utils.getConnectionStateSummary(connectionStatus));
                case 2:
                    profileConnected = true;
                    break;
            }
        }
        String batteryLevelPercentageString = null;
        int batteryLevel = getBatteryLevel();
        if (batteryLevel != -1) {
            batteryLevelPercentageString = Utils.formatPercentage(batteryLevel);
        }
        String[] activeDeviceStringsArray = this.mContext.getResources().getStringArray(R.array.bluetooth_audio_active_device_summaries);
        String activeDeviceString = activeDeviceStringsArray[0];
        if (!this.mIsActiveDeviceA2dp || !this.mIsActiveDeviceHeadset) {
            if (this.mIsActiveDeviceA2dp) {
                activeDeviceString = activeDeviceStringsArray[2];
                if (this.mDevice.isTwsPlusDevice() && !hfpNotConnected) {
                    BluetoothDevice peerDevice = getTwsPeerDevice();
                    if (peerDevice != null && peerDevice.isConnected()) {
                        activeDeviceString = activeDeviceStringsArray[0];
                    }
                }
            }
            if (this.mIsActiveDeviceHeadset) {
                activeDeviceString = activeDeviceStringsArray[3];
                if (this.mDevice.isTwsPlusDevice() && !a2dpNotConnected) {
                    BluetoothDevice peerDevice2 = getTwsPeerDevice();
                    if (peerDevice2 != null && peerDevice2.isConnected()) {
                        activeDeviceString = activeDeviceStringsArray[1];
                    }
                }
            }
        } else {
            activeDeviceString = activeDeviceStringsArray[1];
        }
        if (!hearingAidNotConnected && this.mIsActiveDeviceHearingAid) {
            return this.mContext.getString(R.string.bluetooth_connected, new Object[]{activeDeviceStringsArray[1]});
        } else if (!profileConnected) {
            return getBondState() == 11 ? this.mContext.getString(R.string.bluetooth_pairing) : null;
        } else if (!a2dpNotConnected || !hfpNotConnected) {
            if (a2dpNotConnected) {
                if (batteryLevelPercentageString != null) {
                    return this.mContext.getString(R.string.bluetooth_connected_no_a2dp_battery_level, new Object[]{batteryLevelPercentageString, activeDeviceString});
                }
                return this.mContext.getString(R.string.bluetooth_connected_no_a2dp, new Object[]{activeDeviceString});
            } else if (hfpNotConnected) {
                if (batteryLevelPercentageString != null) {
                    return this.mContext.getString(R.string.bluetooth_connected_no_headset_battery_level, new Object[]{batteryLevelPercentageString, activeDeviceString});
                }
                return this.mContext.getString(R.string.bluetooth_connected_no_headset, new Object[]{activeDeviceString});
            } else if (batteryLevelPercentageString != null) {
                return this.mContext.getString(R.string.bluetooth_connected_battery_level, new Object[]{batteryLevelPercentageString, activeDeviceString});
            } else {
                return this.mContext.getString(R.string.bluetooth_connected, new Object[]{activeDeviceString});
            }
        } else if (batteryLevelPercentageString != null) {
            return this.mContext.getString(R.string.bluetooth_connected_no_headset_no_a2dp_battery_level, new Object[]{batteryLevelPercentageString, activeDeviceString});
        } else {
            return this.mContext.getString(R.string.bluetooth_connected_no_headset_no_a2dp, new Object[]{activeDeviceString});
        }
    }
}
