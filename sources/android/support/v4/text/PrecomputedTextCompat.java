package android.support.v4.text;

import android.os.Build;
import android.support.v4.os.BuildCompat;
import android.support.v4.util.ObjectsCompat;
import android.text.PrecomputedText;
import android.text.Spannable;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;
import java.util.concurrent.Executor;

public class PrecomputedTextCompat implements Spannable {
    private static Executor sExecutor = null;
    private static final Object sLock = new Object();
    private final Params mParams;
    private final Spannable mText;
    private final PrecomputedText mWrapped;

    public static final class Params {
        private final int mBreakStrategy;
        private final int mHyphenationFrequency;
        private final TextPaint mPaint;
        private final TextDirectionHeuristic mTextDir;
        private final PrecomputedText.Params mWrapped;

        public static class Builder {
            private int mBreakStrategy;
            private int mHyphenationFrequency;
            private final TextPaint mPaint;
            private TextDirectionHeuristic mTextDir;

            public Builder(TextPaint paint) {
                this.mPaint = paint;
                if (Build.VERSION.SDK_INT >= 23) {
                    this.mBreakStrategy = 1;
                    this.mHyphenationFrequency = 1;
                } else {
                    this.mHyphenationFrequency = 0;
                    this.mBreakStrategy = 0;
                }
                if (Build.VERSION.SDK_INT >= 18) {
                    this.mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
                } else {
                    this.mTextDir = null;
                }
            }

            public Builder setBreakStrategy(int strategy) {
                this.mBreakStrategy = strategy;
                return this;
            }

            public Builder setHyphenationFrequency(int frequency) {
                this.mHyphenationFrequency = frequency;
                return this;
            }

            public Builder setTextDirection(TextDirectionHeuristic textDir) {
                this.mTextDir = textDir;
                return this;
            }

            public Params build() {
                Params params = new Params(this.mPaint, this.mTextDir, this.mBreakStrategy, this.mHyphenationFrequency);
                return params;
            }
        }

        private Params(TextPaint paint, TextDirectionHeuristic textDir, int strategy, int frequency) {
            if (BuildCompat.isAtLeastP()) {
                this.mWrapped = new PrecomputedText.Params.Builder(paint).setBreakStrategy(strategy).setHyphenationFrequency(frequency).setTextDirection(textDir).build();
            } else {
                this.mWrapped = null;
            }
            this.mPaint = paint;
            this.mTextDir = textDir;
            this.mBreakStrategy = strategy;
            this.mHyphenationFrequency = frequency;
        }

        public Params(PrecomputedText.Params wrapped) {
            this.mPaint = wrapped.getTextPaint();
            this.mTextDir = wrapped.getTextDirection();
            this.mBreakStrategy = wrapped.getBreakStrategy();
            this.mHyphenationFrequency = wrapped.getHyphenationFrequency();
            this.mWrapped = wrapped;
        }

        public TextPaint getTextPaint() {
            return this.mPaint;
        }

        public TextDirectionHeuristic getTextDirection() {
            return this.mTextDir;
        }

        public int getBreakStrategy() {
            return this.mBreakStrategy;
        }

        public int getHyphenationFrequency() {
            return this.mHyphenationFrequency;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o == null || !(o instanceof Params)) {
                return false;
            }
            Params other = (Params) o;
            if (this.mWrapped != null) {
                return this.mWrapped.equals(other.mWrapped);
            }
            if (Build.VERSION.SDK_INT >= 23 && (this.mBreakStrategy != other.getBreakStrategy() || this.mHyphenationFrequency != other.getHyphenationFrequency())) {
                return false;
            }
            if ((Build.VERSION.SDK_INT >= 18 && this.mTextDir != other.getTextDirection()) || this.mPaint.getTextSize() != other.getTextPaint().getTextSize() || this.mPaint.getTextScaleX() != other.getTextPaint().getTextScaleX() || this.mPaint.getTextSkewX() != other.getTextPaint().getTextSkewX()) {
                return false;
            }
            if ((Build.VERSION.SDK_INT >= 21 && (this.mPaint.getLetterSpacing() != other.getTextPaint().getLetterSpacing() || !TextUtils.equals(this.mPaint.getFontFeatureSettings(), other.getTextPaint().getFontFeatureSettings()))) || this.mPaint.getFlags() != other.getTextPaint().getFlags()) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= 24) {
                if (!this.mPaint.getTextLocales().equals(other.getTextPaint().getTextLocales())) {
                    return false;
                }
            } else if (Build.VERSION.SDK_INT >= 17 && !this.mPaint.getTextLocale().equals(other.getTextPaint().getTextLocale())) {
                return false;
            }
            if (this.mPaint.getTypeface() == null) {
                if (other.getTextPaint().getTypeface() != null) {
                    return false;
                }
            } else if (!this.mPaint.getTypeface().equals(other.getTextPaint().getTypeface())) {
                return false;
            }
            return true;
        }

        public int hashCode() {
            if (Build.VERSION.SDK_INT >= 24) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Float.valueOf(this.mPaint.getLetterSpacing()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocales(), this.mPaint.getTypeface(), Boolean.valueOf(this.mPaint.isElegantTextHeight()), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else if (Build.VERSION.SDK_INT >= 21) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Float.valueOf(this.mPaint.getLetterSpacing()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocale(), this.mPaint.getTypeface(), Boolean.valueOf(this.mPaint.isElegantTextHeight()), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else if (Build.VERSION.SDK_INT >= 18) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocale(), this.mPaint.getTypeface(), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else if (Build.VERSION.SDK_INT >= 17) {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTextLocale(), this.mPaint.getTypeface(), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            } else {
                return ObjectsCompat.hash(Float.valueOf(this.mPaint.getTextSize()), Float.valueOf(this.mPaint.getTextScaleX()), Float.valueOf(this.mPaint.getTextSkewX()), Integer.valueOf(this.mPaint.getFlags()), this.mPaint.getTypeface(), this.mTextDir, Integer.valueOf(this.mBreakStrategy), Integer.valueOf(this.mHyphenationFrequency));
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("textSize=" + this.mPaint.getTextSize());
            sb.append(", textScaleX=" + this.mPaint.getTextScaleX());
            sb.append(", textSkewX=" + this.mPaint.getTextSkewX());
            if (Build.VERSION.SDK_INT >= 21) {
                sb.append(", letterSpacing=" + this.mPaint.getLetterSpacing());
                sb.append(", elegantTextHeight=" + this.mPaint.isElegantTextHeight());
            }
            if (Build.VERSION.SDK_INT >= 24) {
                sb.append(", textLocale=" + this.mPaint.getTextLocales());
            } else if (Build.VERSION.SDK_INT >= 17) {
                sb.append(", textLocale=" + this.mPaint.getTextLocale());
            }
            sb.append(", typeface=" + this.mPaint.getTypeface());
            if (Build.VERSION.SDK_INT >= 26) {
                sb.append(", variationSettings=" + this.mPaint.getFontVariationSettings());
            }
            sb.append(", textDir=" + this.mTextDir);
            sb.append(", breakStrategy=" + this.mBreakStrategy);
            sb.append(", hyphenationFrequency=" + this.mHyphenationFrequency);
            sb.append("}");
            return sb.toString();
        }
    }

    public PrecomputedText getPrecomputedText() {
        if (this.mText instanceof PrecomputedText) {
            return (PrecomputedText) this.mText;
        }
        return null;
    }

    public Params getParams() {
        return this.mParams;
    }

    public void setSpan(Object what, int start, int end, int flags) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be set to PrecomputedText.");
        } else if (BuildCompat.isAtLeastP()) {
            this.mWrapped.setSpan(what, start, end, flags);
        } else {
            this.mText.setSpan(what, start, end, flags);
        }
    }

    public void removeSpan(Object what) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException("MetricAffectingSpan can not be removed from PrecomputedText.");
        } else if (BuildCompat.isAtLeastP()) {
            this.mWrapped.removeSpan(what);
        } else {
            this.mText.removeSpan(what);
        }
    }

    public <T> T[] getSpans(int start, int end, Class<T> type) {
        if (BuildCompat.isAtLeastP()) {
            return this.mWrapped.getSpans(start, end, type);
        }
        return this.mText.getSpans(start, end, type);
    }

    public int getSpanStart(Object tag) {
        return this.mText.getSpanStart(tag);
    }

    public int getSpanEnd(Object tag) {
        return this.mText.getSpanEnd(tag);
    }

    public int getSpanFlags(Object tag) {
        return this.mText.getSpanFlags(tag);
    }

    public int nextSpanTransition(int start, int limit, Class type) {
        return this.mText.nextSpanTransition(start, limit, type);
    }

    public int length() {
        return this.mText.length();
    }

    public char charAt(int index) {
        return this.mText.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return this.mText.subSequence(start, end);
    }

    public String toString() {
        return this.mText.toString();
    }
}
