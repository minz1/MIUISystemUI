package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;

public final class BluetoothDeviceFilter {
    public static final Filter ALL_FILTER = new AllFilter();
    public static final Filter BONDED_DEVICE_FILTER = new BondedDeviceFilter();
    private static final Filter[] FILTERS = {ALL_FILTER, new AudioFilter(), new TransferFilter(), new PanuFilter(), new NapFilter()};
    public static final Filter UNBONDED_DEVICE_FILTER = new UnbondedDeviceFilter();

    private static final class AllFilter implements Filter {
        private AllFilter() {
        }

        public boolean matches(BluetoothDevice device) {
            return true;
        }
    }

    private static final class AudioFilter extends ClassUuidFilter {
        private AudioFilter() {
            super();
        }

        /* access modifiers changed from: package-private */
        /* JADX WARNING: Removed duplicated region for block: B:16:0x002c A[RETURN] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean matches(android.os.ParcelUuid[] r4, android.bluetooth.BluetoothClass r5) {
            /*
                r3 = this;
                r0 = 0
                r1 = 1
                if (r4 == 0) goto L_0x0016
                android.os.ParcelUuid[] r2 = com.android.settingslib.bluetooth.A2dpProfile.SINK_UUIDS
                boolean r2 = android.bluetooth.BluetoothUuid.containsAnyUuid(r4, r2)
                if (r2 == 0) goto L_0x000d
                return r1
            L_0x000d:
                android.os.ParcelUuid[] r2 = com.android.settingslib.bluetooth.HeadsetProfile.UUIDS
                boolean r2 = android.bluetooth.BluetoothUuid.containsAnyUuid(r4, r2)
                if (r2 == 0) goto L_0x002c
                return r1
            L_0x0016:
                if (r5 == 0) goto L_0x002c
                boolean r2 = r5.doesClassMatch(r1)
                if (r2 != 0) goto L_0x002b
                r2 = 6
                boolean r2 = r5.doesClassMatch(r2)
                if (r2 != 0) goto L_0x002b
                boolean r2 = r5.doesClassMatch(r0)
                if (r2 == 0) goto L_0x002c
            L_0x002b:
                return r1
            L_0x002c:
                return r0
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.bluetooth.BluetoothDeviceFilter.AudioFilter.matches(android.os.ParcelUuid[], android.bluetooth.BluetoothClass):boolean");
        }
    }

    private static final class BondedDeviceFilter implements Filter {
        private BondedDeviceFilter() {
        }

        public boolean matches(BluetoothDevice device) {
            return device.getBondState() == 12;
        }
    }

    private static abstract class ClassUuidFilter implements Filter {
        /* access modifiers changed from: package-private */
        public abstract boolean matches(ParcelUuid[] parcelUuidArr, BluetoothClass bluetoothClass);

        private ClassUuidFilter() {
        }

        public boolean matches(BluetoothDevice device) {
            return matches(device.getUuids(), device.getBluetoothClass());
        }
    }

    public interface Filter {
        boolean matches(BluetoothDevice bluetoothDevice);
    }

    private static final class NapFilter extends ClassUuidFilter {
        private NapFilter() {
            super();
        }

        /* access modifiers changed from: package-private */
        public boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            boolean z = true;
            if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.NAP)) {
                return true;
            }
            if (btClass == null || !btClass.doesClassMatch(5)) {
                z = false;
            }
            return z;
        }
    }

    private static final class PanuFilter extends ClassUuidFilter {
        private PanuFilter() {
            super();
        }

        /* access modifiers changed from: package-private */
        public boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            boolean z = true;
            if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.PANU)) {
                return true;
            }
            if (btClass == null || !btClass.doesClassMatch(4)) {
                z = false;
            }
            return z;
        }
    }

    private static final class TransferFilter extends ClassUuidFilter {
        private TransferFilter() {
            super();
        }

        /* access modifiers changed from: package-private */
        public boolean matches(ParcelUuid[] uuids, BluetoothClass btClass) {
            boolean z = true;
            if (uuids != null && BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.ObexObjectPush)) {
                return true;
            }
            if (btClass == null || !btClass.doesClassMatch(2)) {
                z = false;
            }
            return z;
        }
    }

    private static final class UnbondedDeviceFilter implements Filter {
        private UnbondedDeviceFilter() {
        }

        public boolean matches(BluetoothDevice device) {
            return device.getBondState() != 12;
        }
    }
}
