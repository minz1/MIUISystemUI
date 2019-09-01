package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.util.Log;
import miui.maml.data.Expression;
import miui.maml.elements.AdvancedSlider;
import org.w3c.dom.Element;

public class UnlockerScreenElement extends AdvancedSlider {
    private boolean mAlwaysShow;
    private Expression mDelay;
    private boolean mNoUnlock;
    private float mPreX;
    private float mPreY;
    private boolean mUnlockingHide;

    public UnlockerScreenElement(Element node, LockScreenRoot root) {
        super(node, root);
        this.mAlwaysShow = Boolean.parseBoolean(node.getAttribute("alwaysShow"));
        this.mNoUnlock = Boolean.parseBoolean(node.getAttribute("noUnlock"));
        this.mDelay = Expression.build(getVariables(), node.getAttribute("delay"));
        this.mIsHaptic = this.mIsHaptic || node.getAttribute("haptic").isEmpty();
    }

    public void finish() {
        UnlockerScreenElement.super.finish();
        this.mUnlockingHide = false;
    }

    public boolean isVisible() {
        return UnlockerScreenElement.super.isVisible() && !this.mUnlockingHide;
    }

    public void endUnlockMoving(UnlockerScreenElement ele) {
        if (ele != this && !this.mAlwaysShow) {
            this.mUnlockingHide = false;
        }
    }

    public void startUnlockMoving(UnlockerScreenElement ele) {
        if (ele != this && !this.mAlwaysShow) {
            this.mUnlockingHide = true;
            resetInner();
        }
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        UnlockerScreenElement.super.onStart();
        getLockScreenRoot().startUnlockMoving(this);
        getLockScreenRoot().pokeWakelock();
    }

    private LockScreenRoot getLockScreenRoot() {
        return this.mRoot;
    }

    /* access modifiers changed from: protected */
    public void onCancel() {
        UnlockerScreenElement.super.onCancel();
        getLockScreenRoot().endUnlockMoving(this);
    }

    /* access modifiers changed from: protected */
    public void onMove(float x, float y) {
        UnlockerScreenElement.super.onMove(x, y);
        float dx = x - this.mPreX;
        float dy = y - this.mPreY;
        if ((dx * dx) + (dy * dy) >= 50.0f) {
            getLockScreenRoot().pokeWakelock();
            this.mPreX = x;
            this.mPreY = y;
        }
    }

    /* access modifiers changed from: protected */
    public boolean onLaunch(String name, Intent intent) {
        UnlockerScreenElement.super.onLaunch(name, intent);
        int i = 0;
        if (!this.mNoUnlock || intent != null) {
            getLockScreenRoot().endUnlockMoving(this);
            try {
                LockScreenRoot lockScreenRoot = getLockScreenRoot();
                if (this.mDelay != null) {
                    i = (int) this.mDelay.evaluate();
                }
                lockScreenRoot.unlocked(intent, i);
            } catch (ActivityNotFoundException e) {
                Log.e("LockScreen_UnlockerScreenElement", e.toString());
                e.printStackTrace();
            }
            return true;
        }
        getLockScreenRoot().pokeWakelock();
        return false;
    }
}
