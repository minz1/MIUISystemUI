package android.support.v7.app;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public abstract class AppCompatDelegate {
    private static int sDefaultNightMode = -1;

    public abstract void addContentView(View view, ViewGroup.LayoutParams layoutParams);

    public abstract boolean applyDayNight();

    public abstract <T extends View> T findViewById(int i);

    public abstract void installViewFactory();

    public abstract void invalidateOptionsMenu();

    public abstract void onCreate(Bundle bundle);

    public abstract void onStop();

    public abstract boolean requestWindowFeature(int i);

    public abstract void setContentView(int i);

    public abstract void setContentView(View view);

    public abstract void setContentView(View view, ViewGroup.LayoutParams layoutParams);

    public abstract void setTitle(CharSequence charSequence);

    public static AppCompatDelegate create(Dialog dialog, AppCompatCallback callback) {
        return new AppCompatDelegateImpl(dialog.getContext(), dialog.getWindow(), callback);
    }

    AppCompatDelegate() {
    }

    public static int getDefaultNightMode() {
        return sDefaultNightMode;
    }
}
