package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LocalBluetoothProfileManager {
    private A2dpProfile mA2dpProfile;
    private A2dpSinkProfile mA2dpSinkProfile;
    private final Context mContext;
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public final BluetoothEventManager mEventManager;
    private HeadsetProfile mHeadsetProfile;
    private HearingAidProfile mHearingAidProfile;
    private HfpClientProfile mHfpClientProfile;
    private HidDeviceProfile mHidDeviceProfile;
    private final HidProfile mHidProfile;
    /* access modifiers changed from: private */
    public final LocalBluetoothAdapter mLocalAdapter;
    private MapClientProfile mMapClientProfile;
    private MapProfile mMapProfile;
    private OppProfile mOppProfile;
    private final PanProfile mPanProfile;
    private PbapClientProfile mPbapClientProfile;
    private final PbapServerProfile mPbapProfile;
    private final Map<String, LocalBluetoothProfile> mProfileNameMap = new HashMap();
    private final Collection<ServiceListener> mServiceListeners = new ArrayList();
    private final boolean mUseMapClient;
    private final boolean mUsePbapPce;

    private class HeadsetStateChangeHandler extends StateChangedHandler {
        private final String mAudioChangeAction;
        private final int mAudioDisconnectedState;

        HeadsetStateChangeHandler(LocalBluetoothProfile profile, String audioChangeAction, int audioDisconnectedState) {
            super(profile);
            this.mAudioChangeAction = audioChangeAction;
            this.mAudioDisconnectedState = audioDisconnectedState;
        }

        public void onReceiveInternal(Intent intent, CachedBluetoothDevice cachedDevice) {
            if (this.mAudioChangeAction.equals(intent.getAction())) {
                if (intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0) != this.mAudioDisconnectedState) {
                    cachedDevice.onProfileStateChanged(this.mProfile, 2);
                }
                cachedDevice.refresh();
                return;
            }
            super.onReceiveInternal(intent, cachedDevice);
        }
    }

    private class PanStateChangedHandler extends StateChangedHandler {
        PanStateChangedHandler(LocalBluetoothProfile profile) {
            super(profile);
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            ((PanProfile) this.mProfile).setLocalRole(device, intent.getIntExtra("android.bluetooth.pan.extra.LOCAL_ROLE", 0));
            super.onReceive(context, intent, device);
        }
    }

    public interface ServiceListener {
        void onServiceConnected();

        void onServiceDisconnected();
    }

    private class StateChangedHandler implements BluetoothEventManager.Handler {
        final LocalBluetoothProfile mProfile;

        StateChangedHandler(LocalBluetoothProfile profile) {
            this.mProfile = profile;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (device == null) {
                Log.w("LocalBluetoothProfileManager", "StateChangedHandler receives state-change for invalid device");
                return;
            }
            CachedBluetoothDevice cachedDevice = LocalBluetoothProfileManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w("LocalBluetoothProfileManager", "StateChangedHandler found new device: " + device);
                cachedDevice = LocalBluetoothProfileManager.this.mDeviceManager.addDevice(LocalBluetoothProfileManager.this.mLocalAdapter, LocalBluetoothProfileManager.this, device);
            }
            onReceiveInternal(intent, cachedDevice);
        }

        /* access modifiers changed from: protected */
        public void onReceiveInternal(Intent intent, CachedBluetoothDevice cachedDevice) {
            int newState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
            int oldState = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", 0);
            if (newState == 0 && oldState == 1) {
                Log.i("LocalBluetoothProfileManager", "Failed to connect " + this.mProfile + " device");
            }
            if (LocalBluetoothProfileManager.this.getHearingAidProfile() != null && (this.mProfile instanceof HearingAidProfile) && newState == 2 && cachedDevice.getHiSyncId() == 0) {
                long newHiSyncId = LocalBluetoothProfileManager.this.getHearingAidProfile().getHiSyncId(cachedDevice.getDevice());
                if (newHiSyncId != 0) {
                    cachedDevice.setHiSyncId(newHiSyncId);
                    LocalBluetoothProfileManager.this.mDeviceManager.onHiSyncIdChanged(newHiSyncId);
                }
            }
            LocalBluetoothProfileManager.this.mEventManager.dispatchProfileConnectionStateChanged(cachedDevice, newState, this.mProfile.getProfileId());
            cachedDevice.onProfileStateChanged(this.mProfile, newState);
            cachedDevice.refresh();
        }
    }

    LocalBluetoothProfileManager(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, BluetoothEventManager eventManager) {
        this.mContext = context;
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mEventManager = eventManager;
        this.mUsePbapPce = this.mContext.getResources().getBoolean(17957110);
        this.mUseMapClient = this.mContext.getResources().getBoolean(17957110);
        this.mLocalAdapter.setProfileManager(this);
        this.mEventManager.setProfileManager(this);
        ParcelUuid[] uuids = adapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }
        this.mHidProfile = new HidProfile(context, this.mLocalAdapter, this.mDeviceManager, this);
        addProfile(this.mHidProfile, "HID", "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        this.mPanProfile = new PanProfile(context, this.mLocalAdapter);
        addPanProfile(this.mPanProfile, "PAN", "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        this.mHidDeviceProfile = new HidDeviceProfile(context, this.mLocalAdapter, this.mDeviceManager, this);
        addProfile(this.mHidDeviceProfile, "HID DEVICE", "android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED");
        Log.d("LocalBluetoothProfileManager", "Adding local MAP profile");
        if (this.mUseMapClient) {
            this.mMapClientProfile = new MapClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mMapClientProfile, "MAP Client", "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
        } else {
            this.mMapProfile = new MapProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mMapProfile, "MAP", "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED");
        }
        Log.d("LocalBluetoothProfileManager", "Adding local PBAP profile");
        this.mPbapProfile = new PbapServerProfile(context);
        addProfile(this.mPbapProfile, PbapServerProfile.NAME, "android.bluetooth.pbap.profile.action.CONNECTION_STATE_CHANGED");
        if (this.mLocalAdapter.getSupportedProfiles().contains(21)) {
            this.mHearingAidProfile = new HearingAidProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
            addProfile(this.mHearingAidProfile, "HearingAid", "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
        }
        this.mEventManager.registerProfileIntentReceiver();
        Log.d("LocalBluetoothProfileManager", "LocalBluetoothProfileManager construction complete");
    }

    /* access modifiers changed from: package-private */
    public void updateLocalProfiles(ParcelUuid[] uuids) {
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSource)) {
            if (this.mA2dpProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local A2DP SRC profile");
                this.mA2dpProfile = new A2dpProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mA2dpProfile, "A2DP", "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mA2dpProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: A2DP profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSink)) {
            if (this.mA2dpSinkProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local A2DP Sink profile");
                this.mA2dpSinkProfile = new A2dpSinkProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mA2dpSinkProfile, "A2DPSink", "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mA2dpSinkProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: A2DP Sink profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree_AG) || BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP_AG)) {
            if (this.mHeadsetProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local HEADSET profile");
                this.mHeadsetProfile = new HeadsetProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addHeadsetProfile(this.mHeadsetProfile, "HEADSET", "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED", "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED", 10);
            }
        } else if (this.mHeadsetProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: HEADSET profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree)) {
            if (this.mHfpClientProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local HfpClient profile");
                this.mHfpClientProfile = new HfpClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addHeadsetProfile(this.mHfpClientProfile, "HEADSET_CLIENT", "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED", "android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED", 0);
            }
        } else if (this.mHfpClientProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: Hfp Client profile was previously added but the UUID is now missing.");
        } else {
            Log.d("LocalBluetoothProfileManager", "Handsfree Uuid not found.");
        }
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.MNS)) {
            if (this.mMapClientProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local Map Client profile");
                this.mMapClientProfile = new MapClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mMapClientProfile, "MAP Client", "android.bluetooth.mapmce.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mMapClientProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: MAP Client profile was previously added but the UUID is now missing.");
        } else {
            Log.d("LocalBluetoothProfileManager", "MAP Client Uuid not found.");
        }
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush)) {
            if (this.mOppProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local OPP profile");
                this.mOppProfile = new OppProfile();
                this.mProfileNameMap.put("OPP", this.mOppProfile);
            }
        } else if (this.mOppProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: OPP profile was previously added but the UUID is now missing.");
        }
        if (this.mUsePbapPce) {
            if (this.mPbapClientProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local PBAP Client profile");
                this.mPbapClientProfile = new PbapClientProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mPbapClientProfile, "PbapClient", "android.bluetooth.pbapclient.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mPbapClientProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: PBAP Client profile was previously added but the UUID is now missing.");
        }
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HearingAid)) {
            if (this.mHearingAidProfile == null) {
                Log.d("LocalBluetoothProfileManager", "Adding local Hearing Aid profile");
                this.mHearingAidProfile = new HearingAidProfile(this.mContext, this.mLocalAdapter, this.mDeviceManager, this);
                addProfile(this.mHearingAidProfile, "HearingAid", "android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
            }
        } else if (this.mHearingAidProfile != null) {
            Log.w("LocalBluetoothProfileManager", "Warning: Hearing Aid profile was previously added but the UUID is now missing.");
        }
        this.mEventManager.registerProfileIntentReceiver();
    }

    private void addHeadsetProfile(LocalBluetoothProfile profile, String profileName, String stateChangedAction, String audioStateChangedAction, int audioDisconnectedState) {
        BluetoothEventManager.Handler handler = new HeadsetStateChangeHandler(profile, audioStateChangedAction, audioDisconnectedState);
        this.mEventManager.addProfileHandler(stateChangedAction, handler);
        this.mEventManager.addProfileHandler(audioStateChangedAction, handler);
        this.mProfileNameMap.put(profileName, profile);
    }

    private void addProfile(LocalBluetoothProfile profile, String profileName, String stateChangedAction) {
        this.mEventManager.addProfileHandler(stateChangedAction, new StateChangedHandler(profile));
        this.mProfileNameMap.put(profileName, profile);
    }

    private void addPanProfile(LocalBluetoothProfile profile, String profileName, String stateChangedAction) {
        this.mEventManager.addProfileHandler(stateChangedAction, new PanStateChangedHandler(profile));
        this.mProfileNameMap.put(profileName, profile);
    }

    /* access modifiers changed from: package-private */
    public void setBluetoothStateOn() {
        ParcelUuid[] uuids = this.mLocalAdapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }
        this.mEventManager.readPairedDevices();
    }

    /* access modifiers changed from: package-private */
    public void callServiceConnectedListeners() {
        for (ServiceListener l : this.mServiceListeners) {
            l.onServiceConnected();
        }
    }

    /* access modifiers changed from: package-private */
    public void callServiceDisconnectedListeners() {
        for (ServiceListener listener : this.mServiceListeners) {
            listener.onServiceDisconnected();
        }
    }

    public A2dpProfile getA2dpProfile() {
        return this.mA2dpProfile;
    }

    public HeadsetProfile getHeadsetProfile() {
        return this.mHeadsetProfile;
    }

    public PbapServerProfile getPbapProfile() {
        return this.mPbapProfile;
    }

    public HearingAidProfile getHearingAidProfile() {
        return this.mHearingAidProfile;
    }

    /* access modifiers changed from: package-private */
    public HidProfile getHidProfile() {
        return this.mHidProfile;
    }

    /* access modifiers changed from: package-private */
    public HidDeviceProfile getHidDeviceProfile() {
        return this.mHidDeviceProfile;
    }

    /* access modifiers changed from: package-private */
    public synchronized void updateProfiles(ParcelUuid[] uuids, ParcelUuid[] localUuids, Collection<LocalBluetoothProfile> profiles, Collection<LocalBluetoothProfile> removedProfiles, boolean isPanNapConnected, BluetoothDevice device) {
        removedProfiles.clear();
        removedProfiles.addAll(profiles);
        Log.d("LocalBluetoothProfileManager", "Current Profiles" + profiles.toString());
        profiles.clear();
        if (uuids != null) {
            if (this.mHeadsetProfile != null && ((BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.HSP_AG) && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP)) || ((BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.Handsfree_AG) && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree)) || this.mHeadsetProfile.getConnectionStatus(device) == 2))) {
                profiles.add(this.mHeadsetProfile);
                removedProfiles.remove(this.mHeadsetProfile);
            }
            if (this.mHfpClientProfile != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree_AG) && BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.Handsfree)) {
                profiles.add(this.mHfpClientProfile);
                removedProfiles.remove(this.mHfpClientProfile);
            }
            if (this.mA2dpProfile != null && (BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.SINK_UUIDS) || this.mA2dpProfile.getConnectionStatus(device) == 2)) {
                profiles.add(this.mA2dpProfile);
                removedProfiles.remove(this.mA2dpProfile);
            }
            if (BluetoothUuid.containsAnyUuid(uuids, A2dpSinkProfile.SRC_UUIDS) && this.mA2dpSinkProfile != null) {
                profiles.add(this.mA2dpSinkProfile);
                removedProfiles.remove(this.mA2dpSinkProfile);
            }
            if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush) && this.mOppProfile != null) {
                profiles.add(this.mOppProfile);
                removedProfiles.remove(this.mOppProfile);
            }
            if ((BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hid) || BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hogp)) && this.mHidProfile != null) {
                profiles.add(this.mHidProfile);
                removedProfiles.remove(this.mHidProfile);
            }
            if (!(this.mHidDeviceProfile == null || this.mHidDeviceProfile.getConnectionStatus(device) == 0)) {
                profiles.add(this.mHidDeviceProfile);
                removedProfiles.remove(this.mHidDeviceProfile);
            }
            if (isPanNapConnected) {
                Log.d("LocalBluetoothProfileManager", "Valid PAN-NAP connection exists.");
            }
            if ((BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.NAP) && this.mPanProfile != null) || isPanNapConnected) {
                profiles.add(this.mPanProfile);
                removedProfiles.remove(this.mPanProfile);
            }
            if (this.mMapProfile != null && this.mMapProfile.getConnectionStatus(device) == 2) {
                profiles.add(this.mMapProfile);
                removedProfiles.remove(this.mMapProfile);
                this.mMapProfile.setPreferred(device, true);
            }
            if (this.mPbapProfile != null && this.mPbapProfile.getConnectionStatus(device) == 2) {
                profiles.add(this.mPbapProfile);
                removedProfiles.remove(this.mPbapProfile);
                this.mPbapProfile.setPreferred(device, true);
            }
            if (this.mMapClientProfile != null) {
                profiles.add(this.mMapClientProfile);
                removedProfiles.remove(this.mMapClientProfile);
            }
            if (this.mUsePbapPce) {
                profiles.add(this.mPbapClientProfile);
                removedProfiles.remove(this.mPbapClientProfile);
            }
            if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HearingAid) && this.mHearingAidProfile != null) {
                profiles.add(this.mHearingAidProfile);
                removedProfiles.remove(this.mHearingAidProfile);
            }
            Log.d("LocalBluetoothProfileManager", "New Profiles" + profiles.toString());
        }
    }
}
