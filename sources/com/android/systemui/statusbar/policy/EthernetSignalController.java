package com.android.systemui.statusbar.policy;

import android.content.Context;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController;
import java.util.BitSet;

public class EthernetSignalController extends SignalController<SignalController.State, SignalController.IconGroup> {
    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public EthernetSignalController(Context context, CallbackHandler callbackHandler, NetworkControllerImpl networkController) {
        super("EthernetSignalController", context, 3, callbackHandler, networkController);
        SignalController.State state = this.mCurrentState;
        SignalController.State state2 = this.mLastState;
        SignalController.IconGroup iconGroup = new SignalController.IconGroup("Ethernet Icons", EthernetIcons.ETHERNET_ICONS, null, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES, 0, 0, 0, 0, AccessibilityContentDescriptions.ETHERNET_CONNECTION_VALUES[0]);
        state2.iconGroup = iconGroup;
        state.iconGroup = iconGroup;
    }

    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        this.mCurrentState.connected = connectedTransports.get(this.mTransportType);
        super.updateConnectivity(connectedTransports, validatedTransports);
    }

    public void notifyListeners(NetworkController.SignalCallback callback) {
        callback.setEthernetIndicators(new NetworkController.IconState(this.mCurrentState.connected, getCurrentIconId(), getStringIfExists(getContentDescription())));
    }

    public SignalController.State cleanState() {
        return new SignalController.State();
    }
}
