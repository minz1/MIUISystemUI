package com.android.systemui.miui.widget;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class StyleSavedView extends LinearLayout {

    private static class SavedState extends View.BaseSavedState {
        private final Drawable mDrawable;
        private final int mLayoutGravity;

        private SavedState(Parcelable superState, Drawable drawable, int layoutGravity) {
            super(superState);
            this.mDrawable = drawable;
            this.mLayoutGravity = layoutGravity;
        }

        public Drawable getDrawalbe() {
            return this.mDrawable;
        }

        public int getLayoutGravity() {
            return this.mLayoutGravity;
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(((BitmapDrawable) this.mDrawable).getBitmap(), flags);
            dest.writeInt(this.mLayoutGravity);
        }
    }

    public StyleSavedView(Context context) {
        super(context);
    }

    public StyleSavedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StyleSavedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), getBackground(), ((FrameLayout.LayoutParams) getLayoutParams()).gravity);
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setBackground(ss.getDrawalbe());
        FrameLayout.LayoutParams mlp = (FrameLayout.LayoutParams) getLayoutParams();
        mlp.gravity = ss.getLayoutGravity();
        setLayoutParams(mlp);
    }
}
