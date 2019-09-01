package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import android.content.Intent;
import android.provider.Settings;
import android.view.MotionEvent;
import com.android.keyguard.ChooseLockSettingsHelper;
import com.android.keyguard.KeyguardUpdateMonitor;
import java.util.Iterator;
import miui.maml.ScreenContext;
import miui.maml.ScreenElementRoot;
import miui.maml.data.BatteryVariableUpdater;
import miui.maml.data.IndexedVariable;
import miui.maml.data.VariableUpdaterManager;
import miui.maml.data.VolumeVariableUpdater;
import miui.maml.elements.ElementGroup;
import miui.maml.elements.ScreenElement;
import miui.maml.util.Utils;
import miui.os.Build;
import org.w3c.dom.Element;

public class LockScreenRoot extends ScreenElementRoot implements ScreenElementRoot.OnExternCommandListener {
    private String curCategory;
    private KeyguardUpdateMonitor.BatteryStatus mBatteryInfo;
    private IndexedVariable mBatteryLevel = new IndexedVariable("battery_level", this.mContext.mVariables, true);
    private IndexedVariable mBatteryState = new IndexedVariable("battery_state", this.mContext.mVariables, true);
    private IndexedVariable mBatteryType = new IndexedVariable("battery_type", this.mContext.mVariables, true);
    private float mFrameRateBatteryFull;
    private float mFrameRateBatteryLow;
    private float mFrameRateCharging;
    private boolean mInit;
    private LockscreenCallback mLockscreenCallback;
    private float mNormalFrameRate;

    public interface LockscreenCallback {
        int getPasswordMode();

        void haptic(int i);

        boolean isSecure();

        boolean isSoundEnable();

        void pokeWakelock();

        boolean unlockVerify(String str, int i);

        void unlocked(Intent intent, int i);
    }

    public LockScreenRoot(ScreenContext c) {
        super(c);
        setOnExternCommandListener(this);
        c.registerObjectFactory("BitmapProvider", new LockscreenBitmapProviderFactory());
        c.registerObjectFactory("ActionCommand", new LockscreenActionCommandFactory());
    }

    public void setLockscreenCallback(LockscreenCallback unlockerCallback) {
        this.mLockscreenCallback = unlockerCallback;
    }

    public boolean onTouch(MotionEvent event) {
        if (this.mInnerGroup != null && this.mInnerGroup.getElements().size() != 0) {
            return LockScreenRoot.super.onTouch(event);
        }
        this.mLockscreenCallback.unlocked(null, 0);
        return false;
    }

    public void pokeWakelock() {
        this.mLockscreenCallback.pokeWakelock();
    }

    /* access modifiers changed from: protected */
    public boolean shouldPlaySound() {
        return this.mLockscreenCallback.isSoundEnable();
    }

    public void haptic(int effectId) {
        this.mLockscreenCallback.haptic(effectId);
    }

    public void unlocked(Intent intent, int delay) {
        this.mLockscreenCallback.unlocked(intent, delay);
    }

    public boolean unlockVerify(String password, int delay) {
        return this.mLockscreenCallback.unlockVerify(password, delay);
    }

    public int getPasswordMode() {
        return this.mLockscreenCallback.getPasswordMode();
    }

    /* access modifiers changed from: protected */
    public void onAddVariableUpdater(VariableUpdaterManager m) {
        LockScreenRoot.super.onAddVariableUpdater(m);
        m.add(new BatteryVariableUpdater(m));
        m.add(new VolumeVariableUpdater(m));
    }

    public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
        int state;
        String newCategory;
        if (!this.mInit) {
            this.mBatteryInfo = status;
            return;
        }
        this.mBatteryLevel.set((double) status.level);
        this.mBatteryType.set((double) status.plugged);
        String str = this.curCategory;
        if (!status.isPluggedIn() && !status.isBatteryLow()) {
            newCategory = "Normal";
            state = 0;
            this.mFrameRate = this.mNormalFrameRate;
        } else if (!status.isPluggedIn()) {
            newCategory = "BatteryLow";
            state = 2;
            this.mFrameRate = this.mFrameRateBatteryLow;
        } else if (status.level >= 100) {
            newCategory = "BatteryFull";
            state = 3;
            this.mFrameRate = this.mFrameRateBatteryFull;
        } else {
            newCategory = "Charging";
            state = 1;
            this.mFrameRate = this.mFrameRateCharging;
        }
        if (newCategory != this.curCategory) {
            requestFramerate(this.mFrameRate);
            requestUpdate();
            this.mBatteryState.set((double) state);
            showCategory("BatteryFull", false);
            showCategory("Charging", false);
            showCategory("BatteryLow", false);
            showCategory("Normal", false);
            showCategory(newCategory, true);
            this.curCategory = newCategory;
        }
    }

    public void init() {
        new ChooseLockSettingsHelper(this.mContext.mContext);
        boolean showSmsBodySetting = Settings.System.getIntForUser(this.mContext.mContext.getContentResolver(), "pref_key_enable_notification_body", 1, KeyguardUpdateMonitor.getCurrentUser()) == 1 && !this.mLockscreenCallback.isSecure();
        Utils.putVariableNumber("sms_body_preview", this.mContext.mVariables, showSmsBodySetting ? 1.0d : 0.0d);
        this.mInit = true;
        if (!showSmsBodySetting) {
            this.mVariableBinderManager.acceptVisitor(new BlockedColumnsSetter("content://sms/inbox", "body"));
        }
        putRawAttr("__is_secure", String.valueOf(this.mLockscreenCallback.isSecure()));
        double operator = 0.0d;
        if (Build.IS_CU_CUSTOMIZATION) {
            operator = 1.0d;
        } else if (Build.IS_CM_CUSTOMIZATION) {
            operator = 2.0d;
        } else if (Build.IS_CT_CUSTOMIZATION) {
            operator = 3.0d;
        }
        Utils.putVariableNumber("operator_customization", this.mContext.mVariables, operator);
        LockScreenRoot.super.init();
        if (this.mBatteryInfo != null) {
            onRefreshBatteryInfo(this.mBatteryInfo);
            this.mBatteryInfo = null;
        }
    }

    /* access modifiers changed from: protected */
    public boolean onLoad(Element root) {
        if (!LockScreenRoot.super.onLoad(root)) {
            return false;
        }
        this.mNormalFrameRate = Utils.getAttrAsFloat(root, "frameRate", this.DEFAULT_FRAME_RATE);
        this.mFrameRateCharging = Utils.getAttrAsFloat(root, "frameRateCharging", this.mNormalFrameRate);
        this.mFrameRateBatteryLow = Utils.getAttrAsFloat(root, "frameRateBatteryLow", this.mNormalFrameRate);
        this.mFrameRateBatteryFull = Utils.getAttrAsFloat(root, "frameRateBatteryFull", this.mNormalFrameRate);
        setClearCanvas(!"false".equalsIgnoreCase(root.getAttribute("clearCanvas")));
        BuiltinVariableBinders.fill(this.mVariableBinderManager);
        this.mFrameRate = this.mNormalFrameRate;
        return true;
    }

    public void finish() {
        LockScreenRoot.super.finish();
        this.curCategory = null;
        this.mInit = false;
        this.mBatteryInfo = null;
    }

    public void startUnlockMoving(UnlockerScreenElement ele) {
        startUnlockMoving(this.mInnerGroup, ele);
    }

    public void endUnlockMoving(UnlockerScreenElement ele) {
        endUnlockMoving(this.mInnerGroup, ele);
    }

    private void startUnlockMoving(ElementGroup g, UnlockerScreenElement ele) {
        if (g != null) {
            Iterator it = g.getElements().iterator();
            while (it.hasNext()) {
                UnlockerScreenElement unlockerScreenElement = (ScreenElement) it.next();
                if (unlockerScreenElement instanceof UnlockerScreenElement) {
                    unlockerScreenElement.startUnlockMoving(ele);
                } else if (unlockerScreenElement instanceof ElementGroup) {
                    startUnlockMoving(unlockerScreenElement, ele);
                }
            }
        }
    }

    private void endUnlockMoving(ElementGroup g, UnlockerScreenElement ele) {
        if (g != null) {
            Iterator it = g.getElements().iterator();
            while (it.hasNext()) {
                UnlockerScreenElement unlockerScreenElement = (ScreenElement) it.next();
                if (unlockerScreenElement instanceof UnlockerScreenElement) {
                    unlockerScreenElement.endUnlockMoving(ele);
                } else if (unlockerScreenElement instanceof ElementGroup) {
                    endUnlockMoving(unlockerScreenElement, ele);
                }
            }
        }
    }

    public void onUIInteractive(ScreenElement e, String action) {
        this.mLockscreenCallback.pokeWakelock();
    }

    public void onCommand(String command, Double para1, String para2) {
        if ("unlock".equals(command)) {
            unlocked(null, 0);
        } else if ("pokewakelock".equals(command)) {
            pokeWakelock();
        }
    }
}
