package com.android.systemui.statusbar.phone;

public class StatusBarFactory {
    private static StatusBarFactory sFactory = new StatusBarFactory();

    public static StatusBarFactory getInstance() {
        return sFactory;
    }

    private StatusBarFactory() {
    }

    /* access modifiers changed from: package-private */
    public CollapsedStatusBarFragmentController getCollapsedStatusBarFragmentController() {
        return new CollapsedStatusBarFragmentControllerImpl();
    }

    /* access modifiers changed from: package-private */
    public KeyguardStatusBarViewController getKeyguardStatusBarViewController() {
        return new KeyguardStatusBarViewControllerImpl();
    }

    public SignalClusterViewController getSignalClusterViewController() {
        return new SignalClusterViewControllerImpl();
    }
}
