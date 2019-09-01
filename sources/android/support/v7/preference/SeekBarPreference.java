package android.support.v7.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends Preference {
    /* access modifiers changed from: private */
    public boolean mAdjustable;
    private int mMax;
    /* access modifiers changed from: private */
    public int mMin;
    /* access modifiers changed from: private */
    public SeekBar mSeekBar;
    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener;
    private int mSeekBarIncrement;
    private View.OnKeyListener mSeekBarKeyListener;
    /* access modifiers changed from: private */
    public int mSeekBarValue;
    private TextView mSeekBarValueTextView;
    private boolean mShowSeekBarValue;
    /* access modifiers changed from: private */
    public boolean mTrackingTouch;

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int max;
        int min;
        int seekBarValue;

        public SavedState(Parcel source) {
            super(source);
            this.seekBarValue = source.readInt();
            this.min = source.readInt();
            this.max = source.readInt();
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.seekBarValue);
            dest.writeInt(this.min);
            dest.writeInt(this.max);
        }
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !SeekBarPreference.this.mTrackingTouch) {
                    SeekBarPreference.this.syncValueInternal(seekBar);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                boolean unused = SeekBarPreference.this.mTrackingTouch = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                boolean unused = SeekBarPreference.this.mTrackingTouch = false;
                if (seekBar.getProgress() + SeekBarPreference.this.mMin != SeekBarPreference.this.mSeekBarValue) {
                    SeekBarPreference.this.syncValueInternal(seekBar);
                }
            }
        };
        this.mSeekBarKeyListener = new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() != 0) {
                    return false;
                }
                if ((!SeekBarPreference.this.mAdjustable && (keyCode == 21 || keyCode == 22)) || keyCode == 23 || keyCode == 66) {
                    return false;
                }
                if (SeekBarPreference.this.mSeekBar != null) {
                    return SeekBarPreference.this.mSeekBar.onKeyDown(keyCode, event);
                }
                Log.e("SeekBarPreference", "SeekBar view is null and hence cannot be adjusted.");
                return false;
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, defStyleAttr, defStyleRes);
        this.mMin = a.getInt(R.styleable.SeekBarPreference_min, 0);
        setMax(a.getInt(R.styleable.SeekBarPreference_android_max, 100));
        setSeekBarIncrement(a.getInt(R.styleable.SeekBarPreference_seekBarIncrement, 0));
        this.mAdjustable = a.getBoolean(R.styleable.SeekBarPreference_adjustable, true);
        this.mShowSeekBarValue = a.getBoolean(R.styleable.SeekBarPreference_showSeekBarValue, true);
        a.recycle();
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.seekBarPreferenceStyle);
    }

    /* access modifiers changed from: protected */
    public Object onGetDefaultValue(TypedArray a, int index) {
        return Integer.valueOf(a.getInt(index, 0));
    }

    public final void setMax(int max) {
        if (max < this.mMin) {
            max = this.mMin;
        }
        if (max != this.mMax) {
            this.mMax = max;
            notifyChanged();
        }
    }

    public final void setSeekBarIncrement(int seekBarIncrement) {
        if (seekBarIncrement != this.mSeekBarIncrement) {
            this.mSeekBarIncrement = Math.min(this.mMax - this.mMin, Math.abs(seekBarIncrement));
            notifyChanged();
        }
    }

    private void setValueInternal(int seekBarValue, boolean notifyChanged) {
        if (seekBarValue < this.mMin) {
            seekBarValue = this.mMin;
        }
        if (seekBarValue > this.mMax) {
            seekBarValue = this.mMax;
        }
        if (seekBarValue != this.mSeekBarValue) {
            this.mSeekBarValue = seekBarValue;
            if (this.mSeekBarValueTextView != null) {
                this.mSeekBarValueTextView.setText(String.valueOf(this.mSeekBarValue));
            }
            persistInt(seekBarValue);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void syncValueInternal(SeekBar seekBar) {
        int seekBarValue = this.mMin + seekBar.getProgress();
        if (seekBarValue == this.mSeekBarValue) {
            return;
        }
        if (callChangeListener(Integer.valueOf(seekBarValue))) {
            setValueInternal(seekBarValue, false);
        } else {
            seekBar.setProgress(this.mSeekBarValue - this.mMin);
        }
    }
}
