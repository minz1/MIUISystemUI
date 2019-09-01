package com.android.systemui.pip.phone;

import android.os.IBinder;
import android.os.RemoteException;
import android.view.IWindowManager;
import android.view.InputChannel;

public class InputConsumerControllerHelper {
    public static void createInputConsumer(IWindowManager manager, IBinder token, String name, InputChannel inputChannel) {
        try {
            manager.createInputConsumer(token, name, inputChannel);
        } catch (RemoteException e) {
        }
    }
}
