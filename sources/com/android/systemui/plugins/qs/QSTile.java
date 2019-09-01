package com.android.systemui.plugins.qs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import java.util.Objects;

@ProvidesInterface(version = 1)
public interface QSTile {
    public static final int STATE_ACTIVE = 2;
    public static final int STATE_INACTIVE = 1;
    public static final int STATE_UNAVAILABLE = 0;
    public static final int VERSION = 1;

    @ProvidesInterface(version = 1)
    public static class AirplaneBooleanState extends BooleanState {
        public static final int VERSION = 1;
        public boolean isAirplaneMode;

        public boolean copyTo(State other) {
            AirplaneBooleanState o = (AirplaneBooleanState) other;
            boolean changed = super.copyTo(other) || o.isAirplaneMode != this.isAirplaneMode;
            o.isAirplaneMode = this.isAirplaneMode;
            return changed;
        }

        public State copy() {
            AirplaneBooleanState state = new AirplaneBooleanState();
            copyTo(state);
            return state;
        }
    }

    @ProvidesInterface(version = 1)
    public static class BooleanState extends State {
        public static final int VERSION = 1;
        public boolean value;

        public boolean copyTo(State other) {
            BooleanState o = (BooleanState) other;
            boolean changed = super.copyTo(other) || o.value != this.value;
            o.value = this.value;
            return changed;
        }

        /* access modifiers changed from: protected */
        public StringBuilder toStringBuilder() {
            StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",value=" + this.value);
            return rt;
        }

        public State copy() {
            BooleanState state = new BooleanState();
            copyTo(state);
            return state;
        }
    }

    @ProvidesInterface(version = 1)
    public interface Callback {
        public static final int VERSION = 1;

        void onAnnouncementRequested(CharSequence charSequence);

        void onScanStateChanged(boolean z);

        void onShowDetail(boolean z);

        void onShowEdit(boolean z);

        void onStateChanged(State state);

        void onToggleStateChanged(boolean z);
    }

    @ProvidesInterface(version = 1)
    public static abstract class Icon {
        public static final int VERSION = 1;

        public abstract Drawable getDrawable(Context context);

        public Drawable getInvisibleDrawable(Context context) {
            return getDrawable(context);
        }

        public int hashCode() {
            return Icon.class.hashCode();
        }

        public int getPadding() {
            return 0;
        }
    }

    @ProvidesInterface(version = 1)
    public static final class SignalState extends BooleanState {
        public static final int VERSION = 1;
        public boolean activityIn;
        public boolean activityOut;
        public boolean isOverlayIconWide;
        public int overlayIconId;

        public boolean copyTo(State other) {
            SignalState o = (SignalState) other;
            boolean changed = (o.activityIn == this.activityIn && o.activityOut == this.activityOut && o.isOverlayIconWide == this.isOverlayIconWide && o.overlayIconId == this.overlayIconId) ? false : true;
            o.activityIn = this.activityIn;
            o.activityOut = this.activityOut;
            o.isOverlayIconWide = this.isOverlayIconWide;
            o.overlayIconId = this.overlayIconId;
            if (super.copyTo(other) || changed) {
                return true;
            }
            return false;
        }

        /* access modifiers changed from: protected */
        public StringBuilder toStringBuilder() {
            StringBuilder rt = super.toStringBuilder();
            rt.insert(rt.length() - 1, ",activityIn=" + this.activityIn);
            rt.insert(rt.length() - 1, ",activityOut=" + this.activityOut);
            return rt;
        }

        public State copy() {
            SignalState state = new SignalState();
            copyTo(state);
            return state;
        }
    }

    @ProvidesInterface(version = 1)
    public static class State {
        public static final int VERSION = 1;
        public CharSequence contentDescription;
        public boolean disabledByPolicy;
        public CharSequence dualLabelContentDescription;
        public boolean dualTarget = false;
        public String expandedAccessibilityClassName;
        public Icon icon;
        public boolean isTransient = false;
        public CharSequence label;
        public CharSequence secondaryLabel;
        public int state = 2;

        public boolean copyTo(State other) {
            if (other == null) {
                throw new IllegalArgumentException();
            } else if (other.getClass().equals(getClass())) {
                boolean changed = !Objects.equals(other.icon, this.icon) || !Objects.equals(other.label, this.label) || !Objects.equals(other.secondaryLabel, this.secondaryLabel) || !Objects.equals(other.contentDescription, this.contentDescription) || !Objects.equals(other.dualLabelContentDescription, this.dualLabelContentDescription) || !Objects.equals(other.expandedAccessibilityClassName, this.expandedAccessibilityClassName) || !Objects.equals(Boolean.valueOf(other.disabledByPolicy), Boolean.valueOf(this.disabledByPolicy)) || !Objects.equals(Integer.valueOf(other.state), Integer.valueOf(this.state)) || !Objects.equals(Boolean.valueOf(other.isTransient), Boolean.valueOf(this.isTransient)) || !Objects.equals(Boolean.valueOf(other.dualTarget), Boolean.valueOf(this.dualTarget));
                other.icon = this.icon;
                other.label = this.label;
                other.secondaryLabel = this.secondaryLabel;
                other.contentDescription = this.contentDescription;
                other.dualLabelContentDescription = this.dualLabelContentDescription;
                other.expandedAccessibilityClassName = this.expandedAccessibilityClassName;
                other.disabledByPolicy = this.disabledByPolicy;
                other.state = this.state;
                other.dualTarget = this.dualTarget;
                other.isTransient = this.isTransient;
                return changed;
            } else {
                throw new IllegalArgumentException();
            }
        }

        public String toString() {
            return toStringBuilder().toString();
        }

        /* access modifiers changed from: protected */
        public StringBuilder toStringBuilder() {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
            sb.append(",icon=");
            sb.append(this.icon);
            sb.append(",label=");
            sb.append(this.label);
            sb.append(",secondaryLabel=");
            sb.append(this.secondaryLabel);
            sb.append(",contentDescription=");
            sb.append(this.contentDescription);
            sb.append(",dualLabelContentDescription=");
            sb.append(this.dualLabelContentDescription);
            sb.append(",expandedAccessibilityClassName=");
            sb.append(this.expandedAccessibilityClassName);
            sb.append(",disabledByPolicy=");
            sb.append(this.disabledByPolicy);
            sb.append(",dualTarget=");
            sb.append(this.dualTarget);
            sb.append(",isTransient=");
            sb.append(this.isTransient);
            sb.append(",state=");
            sb.append(this.state);
            sb.append(']');
            return sb;
        }

        public State copy() {
            State state2 = new State();
            copyTo(state2);
            return state2;
        }
    }

    void addCallback(Callback callback);

    void clearState();

    void click();

    void click(boolean z);

    QSIconView createTileView(Context context);

    void destroy();

    DetailAdapter getDetailAdapter();

    int getMetricsCategory();

    State getState();

    CharSequence getTileLabel();

    String getTileSpec();

    boolean isAvailable();

    void longClick();

    LogMaker populate(LogMaker logMaker);

    void refreshState();

    void removeCallback(Callback callback);

    void removeCallbacks();

    void secondaryClick();

    void setDetailListening(boolean z);

    void setListening(Object obj, boolean z);

    void setTileSpec(String str);

    void userSwitch(int i);
}
