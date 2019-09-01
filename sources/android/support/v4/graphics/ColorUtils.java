package android.support.v4.graphics;

import android.graphics.Color;

public final class ColorUtils {
    private static final ThreadLocal<double[]> TEMP_ARRAY = new ThreadLocal<>();

    public static int compositeColors(int foreground, int background) {
        int bgAlpha = Color.alpha(background);
        int fgAlpha = Color.alpha(foreground);
        int a = compositeAlpha(fgAlpha, bgAlpha);
        return Color.argb(a, compositeComponent(Color.red(foreground), fgAlpha, Color.red(background), bgAlpha, a), compositeComponent(Color.green(foreground), fgAlpha, Color.green(background), bgAlpha, a), compositeComponent(Color.blue(foreground), fgAlpha, Color.blue(background), bgAlpha, a));
    }

    private static int compositeAlpha(int foregroundAlpha, int backgroundAlpha) {
        return 255 - (((255 - backgroundAlpha) * (255 - foregroundAlpha)) / 255);
    }

    private static int compositeComponent(int fgC, int fgA, int bgC, int bgA, int a) {
        if (a == 0) {
            return 0;
        }
        return (((255 * fgC) * fgA) + ((bgC * bgA) * (255 - fgA))) / (a * 255);
    }

    public static double calculateLuminance(int color) {
        double[] result = getTempDouble3Array();
        colorToXYZ(color, result);
        return result[1] / 100.0d;
    }

    public static void RGBToHSL(int r, int g, int b, float[] outHsl) {
        float h;
        float s;
        float rf = ((float) r) / 255.0f;
        float gf = ((float) g) / 255.0f;
        float bf = ((float) b) / 255.0f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float deltaMaxMin = max - min;
        float l = (max + min) / 2.0f;
        if (max == min) {
            s = 0.0f;
            h = 0.0f;
        } else {
            if (max == rf) {
                h = ((gf - bf) / deltaMaxMin) % 6.0f;
            } else if (max == gf) {
                h = ((bf - rf) / deltaMaxMin) + 2.0f;
            } else {
                h = ((rf - gf) / deltaMaxMin) + 4.0f;
            }
            s = deltaMaxMin / (1.0f - Math.abs((2.0f * l) - 1.0f));
        }
        float h2 = (60.0f * h) % 360.0f;
        if (h2 < 0.0f) {
            h2 += 360.0f;
        }
        outHsl[0] = constrain(h2, 0.0f, 360.0f);
        outHsl[1] = constrain(s, 0.0f, 1.0f);
        outHsl[2] = constrain(l, 0.0f, 1.0f);
    }

    public static void colorToHSL(int color, float[] outHsl) {
        RGBToHSL(Color.red(color), Color.green(color), Color.blue(color), outHsl);
    }

    public static int HSLToColor(float[] hsl) {
        float h = hsl[0];
        float s = hsl[1];
        float l = hsl[2];
        float c = (1.0f - Math.abs((2.0f * l) - 1.0f)) * s;
        float m = l - (0.5f * c);
        float x = (1.0f - Math.abs(((h / 60.0f) % 2.0f) - 1.0f)) * c;
        int r = 0;
        int g = 0;
        int b = 0;
        switch (((int) h) / 60) {
            case 0:
                r = Math.round((c + m) * 255.0f);
                g = Math.round((x + m) * 255.0f);
                b = Math.round(255.0f * m);
                break;
            case 1:
                r = Math.round((x + m) * 255.0f);
                g = Math.round((c + m) * 255.0f);
                b = Math.round(255.0f * m);
                break;
            case 2:
                r = Math.round(255.0f * m);
                g = Math.round((c + m) * 255.0f);
                b = Math.round(255.0f * (x + m));
                break;
            case 3:
                r = Math.round(255.0f * m);
                g = Math.round((x + m) * 255.0f);
                b = Math.round(255.0f * (c + m));
                break;
            case 4:
                r = Math.round((x + m) * 255.0f);
                g = Math.round(255.0f * m);
                b = Math.round(255.0f * (c + m));
                break;
            case 5:
            case 6:
                r = Math.round((c + m) * 255.0f);
                g = Math.round(255.0f * m);
                b = Math.round(255.0f * (x + m));
                break;
        }
        return Color.rgb(constrain(r, 0, 255), constrain(g, 0, 255), constrain(b, 0, 255));
    }

    public static int setAlphaComponent(int color, int alpha) {
        if (alpha >= 0 && alpha <= 255) {
            return (16777215 & color) | (alpha << 24);
        }
        throw new IllegalArgumentException("alpha must be between 0 and 255.");
    }

    public static void colorToXYZ(int color, double[] outXyz) {
        RGBToXYZ(Color.red(color), Color.green(color), Color.blue(color), outXyz);
    }

    public static void RGBToXYZ(int r, int g, int b, double[] outXyz) {
        double[] dArr = outXyz;
        if (dArr.length == 3) {
            double sr = ((double) r) / 255.0d;
            double sr2 = sr < 0.04045d ? sr / 12.92d : Math.pow((sr + 0.055d) / 1.055d, 2.4d);
            double sg = ((double) g) / 255.0d;
            double sg2 = sg < 0.04045d ? sg / 12.92d : Math.pow((sg + 0.055d) / 1.055d, 2.4d);
            double sb = ((double) b) / 255.0d;
            double sb2 = sb < 0.04045d ? sb / 12.92d : Math.pow((0.055d + sb) / 1.055d, 2.4d);
            dArr[0] = ((0.4124d * sr2) + (0.3576d * sg2) + (0.1805d * sb2)) * 100.0d;
            dArr[1] = ((0.2126d * sr2) + (0.7152d * sg2) + (0.0722d * sb2)) * 100.0d;
            dArr[2] = 100.0d * ((0.0193d * sr2) + (0.1192d * sg2) + (0.9505d * sb2));
            return;
        }
        int i = r;
        int i2 = g;
        int i3 = b;
        throw new IllegalArgumentException("outXyz must have a length of 3.");
    }

    private static float constrain(float amount, float low, float high) {
        if (amount < low) {
            return low;
        }
        return amount > high ? high : amount;
    }

    private static int constrain(int amount, int low, int high) {
        if (amount < low) {
            return low;
        }
        return amount > high ? high : amount;
    }

    static float circularInterpolate(float a, float b, float f) {
        if (Math.abs(b - a) > 180.0f) {
            if (b > a) {
                a += 360.0f;
            } else {
                b += 360.0f;
            }
        }
        return (((b - a) * f) + a) % 360.0f;
    }

    private static double[] getTempDouble3Array() {
        double[] result = TEMP_ARRAY.get();
        if (result != null) {
            return result;
        }
        double[] result2 = new double[3];
        TEMP_ARRAY.set(result2);
        return result2;
    }
}
