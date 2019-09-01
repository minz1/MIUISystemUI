package android.hardware.usb;

public class UsbManagerCompat {
    public static void setCurrentFunction(UsbManager usbManager, String function, boolean usbDataUnlocked) {
        usbManager.setCurrentFunction(function, usbDataUnlocked);
    }
}
