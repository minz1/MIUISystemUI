package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class BackButton extends Button {
    private BackButtonCallback mBackButtonCallback;

    public interface BackButtonCallback {
        void onBackButtonClicked();
    }

    public BackButton(Context context) {
        this(context, null);
    }

    public BackButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BackButton.this.takeBackAction();
            }
        });
    }

    public void takeBackAction() {
        if (this.mBackButtonCallback != null) {
            this.mBackButtonCallback.onBackButtonClicked();
        }
    }

    public void setCallback(BackButtonCallback callback) {
        this.mBackButtonCallback = callback;
    }
}
