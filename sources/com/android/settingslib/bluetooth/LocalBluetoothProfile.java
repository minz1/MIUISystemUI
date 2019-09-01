package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface LocalBluetoothProfile {
    boolean connect(BluetoothDevice bluetoothDevice);

    boolean disconnect(BluetoothDevice bluetoothDevice);

    int getConnectionStatus(BluetoothDevice bluetoothDevice);

    int getProfileId();

    boolean isAutoConnectable();

    boolean isConnectable();

    boolean isPreferred(BluetoothDevice bluetoothDevice);

    boolean isProfileReady();

    void setPreferred(BluetoothDevice bluetoothDevice, boolean z);
}
