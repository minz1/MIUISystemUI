package android.support.v4.graphics;

import android.graphics.Path;
import android.util.Log;
import java.util.ArrayList;

public class PathParser {

    private static class ExtractFloatResult {
        int mEndPosition;
        boolean mEndWithNegOrDot;

        ExtractFloatResult() {
        }
    }

    public static class PathDataNode {
        public float[] mParams;
        public char mType;

        PathDataNode(char type, float[] params) {
            this.mType = type;
            this.mParams = params;
        }

        PathDataNode(PathDataNode n) {
            this.mType = n.mType;
            this.mParams = PathParser.copyOfRange(n.mParams, 0, n.mParams.length);
        }

        public static void nodesToPath(PathDataNode[] node, Path path) {
            float[] current = new float[6];
            char previousCommand = 'm';
            for (int i = 0; i < node.length; i++) {
                addCommand(path, current, previousCommand, node[i].mType, node[i].mParams);
                previousCommand = node[i].mType;
            }
        }

        public void interpolatePathDataNode(PathDataNode nodeFrom, PathDataNode nodeTo, float fraction) {
            for (int i = 0; i < nodeFrom.mParams.length; i++) {
                this.mParams[i] = (nodeFrom.mParams[i] * (1.0f - fraction)) + (nodeTo.mParams[i] * fraction);
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:16:0x006d, code lost:
            r27 = r6;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:42:0x019c, code lost:
            r21 = r0;
            r22 = r1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:43:0x01a0, code lost:
            r14 = r7;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:79:0x033f, code lost:
            r21 = r0;
            r22 = r1;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private static void addCommand(android.graphics.Path r29, float[] r30, char r31, char r32, float[] r33) {
            /*
                r10 = r29
                r13 = r33
                r0 = 2
                r14 = 0
                r1 = r30[r14]
                r15 = 1
                r2 = r30[r15]
                r16 = 2
                r3 = r30[r16]
                r17 = 3
                r4 = r30[r17]
                r18 = 4
                r5 = r30[r18]
                r19 = 5
                r6 = r30[r19]
                switch(r32) {
                    case 65: goto L_0x0034;
                    case 67: goto L_0x0032;
                    case 72: goto L_0x0030;
                    case 76: goto L_0x002e;
                    case 77: goto L_0x002e;
                    case 81: goto L_0x002c;
                    case 83: goto L_0x002c;
                    case 84: goto L_0x002e;
                    case 86: goto L_0x0030;
                    case 90: goto L_0x0021;
                    case 97: goto L_0x0034;
                    case 99: goto L_0x0032;
                    case 104: goto L_0x0030;
                    case 108: goto L_0x002e;
                    case 109: goto L_0x002e;
                    case 113: goto L_0x002c;
                    case 115: goto L_0x002c;
                    case 116: goto L_0x002e;
                    case 118: goto L_0x0030;
                    case 122: goto L_0x0021;
                    default: goto L_0x001e;
                }
            L_0x001e:
                r20 = r0
                goto L_0x0036
            L_0x0021:
                r29.close()
                r1 = r5
                r2 = r6
                r3 = r5
                r4 = r6
                r10.moveTo(r1, r2)
                goto L_0x001e
            L_0x002c:
                r0 = 4
                goto L_0x001e
            L_0x002e:
                r0 = 2
                goto L_0x001e
            L_0x0030:
                r0 = 1
                goto L_0x001e
            L_0x0032:
                r0 = 6
                goto L_0x001e
            L_0x0034:
                r0 = 7
                goto L_0x001e
            L_0x0036:
                r7 = r31
                r9 = r1
                r8 = r2
                r21 = r3
                r22 = r4
                r23 = r5
                r24 = r6
                r0 = r14
            L_0x0043:
                r6 = r0
                int r0 = r13.length
                if (r6 >= r0) goto L_0x038c
                r0 = 115(0x73, float:1.61E-43)
                r1 = 99
                r2 = 84
                r3 = 81
                r4 = 116(0x74, float:1.63E-43)
                r5 = 113(0x71, float:1.58E-43)
                r25 = 1073741824(0x40000000, float:2.0)
                r15 = 0
                switch(r32) {
                    case 65: goto L_0x0344;
                    case 67: goto L_0x030d;
                    case 72: goto L_0x02fb;
                    case 76: goto L_0x02e1;
                    case 77: goto L_0x02b1;
                    case 81: goto L_0x0287;
                    case 83: goto L_0x0238;
                    case 84: goto L_0x0205;
                    case 86: goto L_0x01f3;
                    case 97: goto L_0x01a3;
                    case 99: goto L_0x0169;
                    case 104: goto L_0x015a;
                    case 108: goto L_0x0142;
                    case 109: goto L_0x0114;
                    case 113: goto L_0x00e9;
                    case 115: goto L_0x009e;
                    case 116: goto L_0x0071;
                    case 118: goto L_0x0060;
                    default: goto L_0x0059;
                }
            L_0x0059:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                goto L_0x0384
            L_0x0060:
                int r0 = r6 + 0
                r0 = r13[r0]
                r10.rLineTo(r15, r0)
                int r0 = r6 + 0
                r0 = r13[r0]
                float r8 = r8 + r0
            L_0x006d:
                r27 = r6
                goto L_0x01a0
            L_0x0071:
                r0 = 0
                r1 = 0
                if (r7 == r5) goto L_0x007b
                if (r7 == r4) goto L_0x007b
                if (r7 == r3) goto L_0x007b
                if (r7 != r2) goto L_0x007f
            L_0x007b:
                float r0 = r9 - r21
                float r1 = r8 - r22
            L_0x007f:
                int r2 = r6 + 0
                r2 = r13[r2]
                int r3 = r6 + 1
                r3 = r13[r3]
                r10.rQuadTo(r0, r1, r2, r3)
                float r2 = r9 + r0
                float r3 = r8 + r1
                int r4 = r6 + 0
                r4 = r13[r4]
                float r9 = r9 + r4
                int r4 = r6 + 1
                r4 = r13[r4]
                float r8 = r8 + r4
                r21 = r2
                r22 = r3
                goto L_0x006d
            L_0x009e:
                r2 = 0
                r3 = 0
                if (r7 == r1) goto L_0x00b1
                if (r7 == r0) goto L_0x00b1
                r0 = 67
                if (r7 == r0) goto L_0x00b1
                r0 = 83
                if (r7 != r0) goto L_0x00ad
                goto L_0x00b1
            L_0x00ad:
                r15 = r2
                r25 = r3
                goto L_0x00b8
            L_0x00b1:
                float r0 = r9 - r21
                float r1 = r8 - r22
                r15 = r0
                r25 = r1
            L_0x00b8:
                int r0 = r6 + 0
                r3 = r13[r0]
                int r0 = r6 + 1
                r4 = r13[r0]
                int r0 = r6 + 2
                r5 = r13[r0]
                int r0 = r6 + 3
                r26 = r13[r0]
                r0 = r10
                r1 = r15
                r2 = r25
                r27 = r6
                r6 = r26
                r0.rCubicTo(r1, r2, r3, r4, r5, r6)
                int r6 = r27 + 0
                r0 = r13[r6]
                float r0 = r0 + r9
                int r6 = r27 + 1
                r1 = r13[r6]
                float r1 = r1 + r8
                int r6 = r27 + 2
                r2 = r13[r6]
                float r9 = r9 + r2
                int r6 = r27 + 3
                r2 = r13[r6]
                float r8 = r8 + r2
                goto L_0x019c
            L_0x00e9:
                r27 = r6
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                int r6 = r27 + 2
                r2 = r13[r6]
                int r6 = r27 + 3
                r3 = r13[r6]
                r10.rQuadTo(r0, r1, r2, r3)
                int r6 = r27 + 0
                r0 = r13[r6]
                float r0 = r0 + r9
                int r6 = r27 + 1
                r1 = r13[r6]
                float r1 = r1 + r8
                int r6 = r27 + 2
                r2 = r13[r6]
                float r9 = r9 + r2
                int r6 = r27 + 3
                r2 = r13[r6]
                float r8 = r8 + r2
                goto L_0x019c
            L_0x0114:
                r27 = r6
                int r6 = r27 + 0
                r0 = r13[r6]
                float r9 = r9 + r0
                int r6 = r27 + 1
                r0 = r13[r6]
                float r8 = r8 + r0
                if (r27 <= 0) goto L_0x012f
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                r10.rLineTo(r0, r1)
                goto L_0x01a0
            L_0x012f:
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                r10.rMoveTo(r0, r1)
                r0 = r9
                r1 = r8
                r23 = r0
                r24 = r1
                goto L_0x01a0
            L_0x0142:
                r27 = r6
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                r10.rLineTo(r0, r1)
                int r6 = r27 + 0
                r0 = r13[r6]
                float r9 = r9 + r0
                int r6 = r27 + 1
                r0 = r13[r6]
                float r8 = r8 + r0
                goto L_0x01a0
            L_0x015a:
                r27 = r6
                int r6 = r27 + 0
                r0 = r13[r6]
                r10.rLineTo(r0, r15)
                int r6 = r27 + 0
                r0 = r13[r6]
                float r9 = r9 + r0
                goto L_0x01a0
            L_0x0169:
                r27 = r6
                int r6 = r27 + 0
                r1 = r13[r6]
                int r6 = r27 + 1
                r2 = r13[r6]
                int r6 = r27 + 2
                r3 = r13[r6]
                int r6 = r27 + 3
                r4 = r13[r6]
                int r6 = r27 + 4
                r5 = r13[r6]
                int r6 = r27 + 5
                r6 = r13[r6]
                r0 = r10
                r0.rCubicTo(r1, r2, r3, r4, r5, r6)
                int r6 = r27 + 2
                r0 = r13[r6]
                float r0 = r0 + r9
                int r6 = r27 + 3
                r1 = r13[r6]
                float r1 = r1 + r8
                int r6 = r27 + 4
                r2 = r13[r6]
                float r9 = r9 + r2
                int r6 = r27 + 5
                r2 = r13[r6]
                float r8 = r8 + r2
            L_0x019c:
                r21 = r0
                r22 = r1
            L_0x01a0:
                r14 = r7
                goto L_0x0384
            L_0x01a3:
                r27 = r6
                int r6 = r27 + 5
                r0 = r13[r6]
                float r3 = r0 + r9
                int r6 = r27 + 6
                r0 = r13[r6]
                float r4 = r0 + r8
                int r6 = r27 + 0
                r5 = r13[r6]
                int r6 = r27 + 1
                r6 = r13[r6]
                int r0 = r27 + 2
                r25 = r13[r0]
                int r0 = r27 + 3
                r0 = r13[r0]
                int r0 = (r0 > r15 ? 1 : (r0 == r15 ? 0 : -1))
                if (r0 == 0) goto L_0x01c8
                r26 = 1
                goto L_0x01ca
            L_0x01c8:
                r26 = r14
            L_0x01ca:
                int r0 = r27 + 4
                r0 = r13[r0]
                int r0 = (r0 > r15 ? 1 : (r0 == r15 ? 0 : -1))
                if (r0 == 0) goto L_0x01d4
                r15 = 1
                goto L_0x01d5
            L_0x01d4:
                r15 = r14
            L_0x01d5:
                r0 = r10
                r1 = r9
                r2 = r8
                r14 = r7
                r7 = r25
                r11 = r8
                r8 = r26
                r12 = r9
                r9 = r15
                drawArc(r0, r1, r2, r3, r4, r5, r6, r7, r8, r9)
                int r6 = r27 + 5
                r0 = r13[r6]
                float r9 = r12 + r0
                int r6 = r27 + 6
                r0 = r13[r6]
                float r8 = r11 + r0
                r0 = r9
                r1 = r8
                goto L_0x033f
            L_0x01f3:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                int r6 = r27 + 0
                r0 = r13[r6]
                r10.lineTo(r12, r0)
                int r6 = r27 + 0
                r8 = r13[r6]
                goto L_0x0384
            L_0x0205:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                r0 = r12
                r1 = r11
                if (r14 == r5) goto L_0x0214
                if (r14 == r4) goto L_0x0214
                if (r14 == r3) goto L_0x0214
                if (r14 != r2) goto L_0x021c
            L_0x0214:
                float r9 = r25 * r12
                float r0 = r9 - r21
                float r25 = r25 * r11
                float r1 = r25 - r22
            L_0x021c:
                int r6 = r27 + 0
                r2 = r13[r6]
                int r6 = r27 + 1
                r3 = r13[r6]
                r10.quadTo(r0, r1, r2, r3)
                r2 = r0
                r3 = r1
                int r6 = r27 + 0
                r9 = r13[r6]
                int r6 = r27 + 1
                r8 = r13[r6]
                r21 = r2
                r22 = r3
                goto L_0x0384
            L_0x0238:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                r2 = r12
                r3 = r11
                if (r14 == r1) goto L_0x0250
                if (r14 == r0) goto L_0x0250
                r0 = 67
                if (r14 == r0) goto L_0x0250
                r0 = 83
                if (r14 != r0) goto L_0x024c
                goto L_0x0250
            L_0x024c:
                r9 = r2
                r25 = r3
                goto L_0x0258
            L_0x0250:
                float r9 = r25 * r12
                float r9 = r9 - r21
                float r25 = r25 * r11
                float r25 = r25 - r22
            L_0x0258:
                int r6 = r27 + 0
                r3 = r13[r6]
                int r6 = r27 + 1
                r4 = r13[r6]
                int r6 = r27 + 2
                r5 = r13[r6]
                int r6 = r27 + 3
                r6 = r13[r6]
                r0 = r10
                r1 = r9
                r2 = r25
                r0.cubicTo(r1, r2, r3, r4, r5, r6)
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                int r6 = r27 + 2
                r2 = r13[r6]
                int r6 = r27 + 3
                r8 = r13[r6]
                r21 = r0
                r22 = r1
                r9 = r2
                goto L_0x0384
            L_0x0287:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                int r6 = r27 + 2
                r2 = r13[r6]
                int r6 = r27 + 3
                r3 = r13[r6]
                r10.quadTo(r0, r1, r2, r3)
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                int r6 = r27 + 2
                r9 = r13[r6]
                int r6 = r27 + 3
                r8 = r13[r6]
                goto L_0x033f
            L_0x02b1:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                int r6 = r27 + 0
                r9 = r13[r6]
                int r6 = r27 + 1
                r8 = r13[r6]
                if (r27 <= 0) goto L_0x02cd
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                r10.lineTo(r0, r1)
                goto L_0x0384
            L_0x02cd:
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                r10.moveTo(r0, r1)
                r0 = r9
                r1 = r8
                r23 = r0
                r24 = r1
                goto L_0x0384
            L_0x02e1:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                int r6 = r27 + 0
                r0 = r13[r6]
                int r6 = r27 + 1
                r1 = r13[r6]
                r10.lineTo(r0, r1)
                int r6 = r27 + 0
                r9 = r13[r6]
                int r6 = r27 + 1
                r8 = r13[r6]
                goto L_0x0384
            L_0x02fb:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                int r6 = r27 + 0
                r0 = r13[r6]
                r10.lineTo(r0, r11)
                int r6 = r27 + 0
                r9 = r13[r6]
                goto L_0x0384
            L_0x030d:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                int r6 = r27 + 0
                r1 = r13[r6]
                int r6 = r27 + 1
                r2 = r13[r6]
                int r6 = r27 + 2
                r3 = r13[r6]
                int r6 = r27 + 3
                r4 = r13[r6]
                int r6 = r27 + 4
                r5 = r13[r6]
                int r6 = r27 + 5
                r6 = r13[r6]
                r0 = r10
                r0.cubicTo(r1, r2, r3, r4, r5, r6)
                int r6 = r27 + 4
                r9 = r13[r6]
                int r6 = r27 + 5
                r8 = r13[r6]
                int r6 = r27 + 2
                r0 = r13[r6]
                int r6 = r27 + 3
                r1 = r13[r6]
            L_0x033f:
                r21 = r0
                r22 = r1
                goto L_0x0384
            L_0x0344:
                r27 = r6
                r14 = r7
                r11 = r8
                r12 = r9
                int r6 = r27 + 5
                r3 = r13[r6]
                int r6 = r27 + 6
                r4 = r13[r6]
                int r6 = r27 + 0
                r5 = r13[r6]
                int r6 = r27 + 1
                r6 = r13[r6]
                int r0 = r27 + 2
                r7 = r13[r0]
                int r0 = r27 + 3
                r0 = r13[r0]
                int r0 = (r0 > r15 ? 1 : (r0 == r15 ? 0 : -1))
                if (r0 == 0) goto L_0x0367
                r8 = 1
                goto L_0x0368
            L_0x0367:
                r8 = 0
            L_0x0368:
                int r0 = r27 + 4
                r0 = r13[r0]
                int r0 = (r0 > r15 ? 1 : (r0 == r15 ? 0 : -1))
                if (r0 == 0) goto L_0x0372
                r9 = 1
                goto L_0x0373
            L_0x0372:
                r9 = 0
            L_0x0373:
                r0 = r10
                r1 = r12
                r2 = r11
                drawArc(r0, r1, r2, r3, r4, r5, r6, r7, r8, r9)
                int r6 = r27 + 5
                r9 = r13[r6]
                int r6 = r27 + 6
                r8 = r13[r6]
                r0 = r9
                r1 = r8
                goto L_0x033f
            L_0x0384:
                r7 = r32
                int r0 = r27 + r20
                r14 = 0
                r15 = 1
                goto L_0x0043
            L_0x038c:
                r14 = r7
                r11 = r8
                r2 = r11
                r1 = 0
                r30[r1] = r9
                r1 = 1
                r30[r1] = r2
                r30[r16] = r21
                r30[r17] = r22
                r30[r18] = r23
                r30[r19] = r24
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: android.support.v4.graphics.PathParser.PathDataNode.addCommand(android.graphics.Path, float[], char, char, float[]):void");
        }

        private static void drawArc(Path p, float x0, float y0, float x1, float y1, float a, float b, float theta, boolean isMoreThanHalf, boolean isPositiveArc) {
            double cy;
            double cx;
            float f = x0;
            float f2 = y0;
            float adjust = x1;
            float f3 = y1;
            float f4 = a;
            float f5 = b;
            boolean z = isPositiveArc;
            float f6 = theta;
            double thetaD = Math.toRadians((double) f6);
            double cosTheta = Math.cos(thetaD);
            double sinTheta = Math.sin(thetaD);
            double x0p = ((((double) f) * cosTheta) + (((double) f2) * sinTheta)) / ((double) f4);
            double y0p = ((((double) (-f)) * sinTheta) + (((double) f2) * cosTheta)) / ((double) f5);
            double x1p = ((((double) adjust) * cosTheta) + (((double) f3) * sinTheta)) / ((double) f4);
            double y1p = ((((double) (-adjust)) * sinTheta) + (((double) f3) * cosTheta)) / ((double) f5);
            double dx = x0p - x1p;
            double dy = y0p - y1p;
            double xm = (x0p + x1p) / 2.0d;
            double ym = (y0p + y1p) / 2.0d;
            double dsq = (dx * dx) + (dy * dy);
            if (dsq == 0.0d) {
                Log.w("PathParser", " Points are coincident");
                return;
            }
            double disc = (1.0d / dsq) - 0.25d;
            if (disc < 0.0d) {
                Log.w("PathParser", "Points are too far apart " + dsq);
                float adjust2 = (float) (Math.sqrt(dsq) / 1.99999d);
                double d = disc;
                float f7 = adjust2;
                double d2 = dsq;
                double d3 = thetaD;
                drawArc(p, f, f2, adjust, f3, f4 * adjust2, f5 * adjust2, f6, isMoreThanHalf, isPositiveArc);
                return;
            }
            double d4 = dsq;
            double thetaD2 = thetaD;
            double s = Math.sqrt(disc);
            double sdx = s * dx;
            double sdy = s * dy;
            boolean z2 = isPositiveArc;
            if (isMoreThanHalf == z2) {
                cx = xm - sdy;
                cy = ym + sdx;
            } else {
                cx = xm + sdy;
                cy = ym - sdx;
            }
            double d5 = s;
            double eta0 = Math.atan2(y0p - cy, x0p - cx);
            double d6 = sdx;
            double eta1 = Math.atan2(y1p - cy, x1p - cx);
            double sweep = eta1 - eta0;
            if (z2 != (sweep >= 0.0d)) {
                if (sweep > 0.0d) {
                    sweep -= 6.283185307179586d;
                } else {
                    sweep += 6.283185307179586d;
                }
            }
            double d7 = sdy;
            float f8 = a;
            double cx2 = cx * ((double) f8);
            float f9 = b;
            double cy2 = cy * ((double) f9);
            double tcx = cx2;
            double d8 = tcx;
            arcToBezier(p, (cx2 * cosTheta) - (cy2 * sinTheta), (tcx * sinTheta) + (cy2 * cosTheta), (double) f8, (double) f9, (double) f, (double) f2, thetaD2, eta0, sweep);
        }

        private static void arcToBezier(Path p, double cx, double cy, double a, double b, double e1x, double e1y, double theta, double start, double sweep) {
            double eta1 = a;
            int numSegments = (int) Math.ceil(Math.abs((sweep * 4.0d) / 3.141592653589793d));
            double eta12 = start;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            double cosEta1 = Math.cos(eta12);
            double sinEta1 = Math.sin(eta12);
            double anglePerSegment = sweep / ((double) numSegments);
            int i = 0;
            double ep1y = ((-eta1) * sinTheta * sinEta1) + (b * cosTheta * cosEta1);
            double e1y2 = e1y;
            double ep1x = (((-eta1) * cosTheta) * sinEta1) - ((b * sinTheta) * cosEta1);
            double e1x2 = e1x;
            while (true) {
                int i2 = i;
                if (i2 < numSegments) {
                    int i3 = i2;
                    double eta2 = eta12 + anglePerSegment;
                    double sinEta2 = Math.sin(eta2);
                    double cosEta2 = Math.cos(eta2);
                    double anglePerSegment2 = anglePerSegment;
                    double e2x = (cx + ((eta1 * cosTheta) * cosEta2)) - ((b * sinTheta) * sinEta2);
                    double e2y = cy + (eta1 * sinTheta * cosEta2) + (b * cosTheta * sinEta2);
                    double ep2x = (((-eta1) * cosTheta) * sinEta2) - ((b * sinTheta) * cosEta2);
                    double ep2y = ((-eta1) * sinTheta * sinEta2) + (b * cosTheta * cosEta2);
                    double tanDiff2 = Math.tan((eta2 - eta12) / 2.0d);
                    double d = tanDiff2;
                    double alpha = (Math.sin(eta2 - eta12) * (Math.sqrt(4.0d + ((3.0d * tanDiff2) * tanDiff2)) - 1.0d)) / 3.0d;
                    double q1x = e1x2 + (alpha * ep1x);
                    int numSegments2 = numSegments;
                    double d2 = eta12;
                    double q1y = e1y2 + (alpha * ep1y);
                    double q2x = e2x - (alpha * ep2x);
                    double d3 = alpha;
                    double q2y = e2y - (alpha * ep2y);
                    double sinTheta2 = sinTheta;
                    Path path = p;
                    path.rLineTo(0.0f, 0.0f);
                    double d4 = q1x;
                    float f = (float) q2y;
                    double d5 = q2y;
                    double d6 = q1y;
                    double q2y2 = e2x;
                    double d7 = q2x;
                    double e2y2 = e2y;
                    path.cubicTo((float) q1x, (float) q1y, (float) q2x, f, (float) q2y2, (float) e2y2);
                    e1x2 = q2y2;
                    e1y2 = e2y2;
                    ep1x = ep2x;
                    ep1y = ep2y;
                    i = i3 + 1;
                    eta12 = eta2;
                    anglePerSegment = anglePerSegment2;
                    numSegments = numSegments2;
                    cosTheta = cosTheta;
                    sinTheta = sinTheta2;
                    eta1 = a;
                } else {
                    int i4 = numSegments;
                    double d8 = eta12;
                    double d9 = cosTheta;
                    double d10 = sinTheta;
                    Path path2 = p;
                    return;
                }
            }
        }
    }

    static float[] copyOfRange(float[] original, int start, int end) {
        if (start <= end) {
            int originalLength = original.length;
            if (start < 0 || start > originalLength) {
                throw new ArrayIndexOutOfBoundsException();
            }
            int resultLength = end - start;
            float[] result = new float[resultLength];
            System.arraycopy(original, start, result, 0, Math.min(resultLength, originalLength - start));
            return result;
        }
        throw new IllegalArgumentException();
    }

    public static Path createPathFromPathData(String pathData) {
        Path path = new Path();
        PathDataNode[] nodes = createNodesFromPathData(pathData);
        if (nodes == null) {
            return null;
        }
        try {
            PathDataNode.nodesToPath(nodes, path);
            return path;
        } catch (RuntimeException e) {
            throw new RuntimeException("Error in parsing " + pathData, e);
        }
    }

    public static PathDataNode[] createNodesFromPathData(String pathData) {
        if (pathData == null) {
            return null;
        }
        int start = 0;
        int end = 1;
        ArrayList<PathDataNode> list = new ArrayList<>();
        while (end < pathData.length()) {
            int end2 = nextStart(pathData, end);
            String s = pathData.substring(start, end2).trim();
            if (s.length() > 0) {
                addNode(list, s.charAt(0), getFloats(s));
            }
            start = end2;
            end = end2 + 1;
        }
        if (end - start == 1 && start < pathData.length()) {
            addNode(list, pathData.charAt(start), new float[0]);
        }
        return (PathDataNode[]) list.toArray(new PathDataNode[list.size()]);
    }

    public static PathDataNode[] deepCopyNodes(PathDataNode[] source) {
        if (source == null) {
            return null;
        }
        PathDataNode[] copy = new PathDataNode[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = new PathDataNode(source[i]);
        }
        return copy;
    }

    public static boolean canMorph(PathDataNode[] nodesFrom, PathDataNode[] nodesTo) {
        if (nodesFrom == null || nodesTo == null || nodesFrom.length != nodesTo.length) {
            return false;
        }
        for (int i = 0; i < nodesFrom.length; i++) {
            if (nodesFrom[i].mType != nodesTo[i].mType || nodesFrom[i].mParams.length != nodesTo[i].mParams.length) {
                return false;
            }
        }
        return true;
    }

    public static void updateNodes(PathDataNode[] target, PathDataNode[] source) {
        for (int i = 0; i < source.length; i++) {
            target[i].mType = source[i].mType;
            for (int j = 0; j < source[i].mParams.length; j++) {
                target[i].mParams[j] = source[i].mParams[j];
            }
        }
    }

    private static int nextStart(String s, int end) {
        while (end < s.length()) {
            char c = s.charAt(end);
            if (((c - 'A') * (c - 'Z') <= 0 || (c - 'a') * (c - 'z') <= 0) && c != 'e' && c != 'E') {
                return end;
            }
            end++;
        }
        return end;
    }

    private static void addNode(ArrayList<PathDataNode> list, char cmd, float[] val) {
        list.add(new PathDataNode(cmd, val));
    }

    private static float[] getFloats(String s) {
        if (s.charAt(0) == 'z' || s.charAt(0) == 'Z') {
            return new float[0];
        }
        try {
            float[] results = new float[s.length()];
            int count = 0;
            int startPosition = 1;
            ExtractFloatResult result = new ExtractFloatResult();
            int totalLength = s.length();
            while (startPosition < totalLength) {
                extract(s, startPosition, result);
                int endPosition = result.mEndPosition;
                if (startPosition < endPosition) {
                    results[count] = Float.parseFloat(s.substring(startPosition, endPosition));
                    count++;
                }
                if (result.mEndWithNegOrDot != 0) {
                    startPosition = endPosition;
                } else {
                    startPosition = endPosition + 1;
                }
            }
            return copyOfRange(results, 0, count);
        } catch (NumberFormatException e) {
            throw new RuntimeException("error in parsing \"" + s + "\"", e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x003b A[LOOP:0: B:1:0x0007->B:20:0x003b, LOOP_END] */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x003e A[SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void extract(java.lang.String r7, int r8, android.support.v4.graphics.PathParser.ExtractFloatResult r9) {
        /*
            r0 = r8
            r1 = 0
            r2 = 0
            r9.mEndWithNegOrDot = r2
            r3 = 0
        L_0x0007:
            int r4 = r7.length()
            if (r0 >= r4) goto L_0x003e
            r4 = r2
            r2 = 0
            char r5 = r7.charAt(r0)
            r6 = 32
            if (r5 == r6) goto L_0x0036
            r6 = 69
            if (r5 == r6) goto L_0x0034
            r6 = 101(0x65, float:1.42E-43)
            if (r5 == r6) goto L_0x0034
            r6 = 1
            switch(r5) {
                case 44: goto L_0x0036;
                case 45: goto L_0x002c;
                case 46: goto L_0x0024;
                default: goto L_0x0023;
            }
        L_0x0023:
            goto L_0x0038
        L_0x0024:
            if (r3 != 0) goto L_0x0028
            r3 = 1
            goto L_0x0038
        L_0x0028:
            r1 = 1
            r9.mEndWithNegOrDot = r6
            goto L_0x0038
        L_0x002c:
            if (r0 == r8) goto L_0x0038
            if (r4 != 0) goto L_0x0038
            r1 = 1
            r9.mEndWithNegOrDot = r6
            goto L_0x0038
        L_0x0034:
            r2 = 1
            goto L_0x0038
        L_0x0036:
            r1 = 1
        L_0x0038:
            if (r1 == 0) goto L_0x003b
            goto L_0x003e
        L_0x003b:
            int r0 = r0 + 1
            goto L_0x0007
        L_0x003e:
            r9.mEndPosition = r0
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.graphics.PathParser.extract(java.lang.String, int, android.support.v4.graphics.PathParser$ExtractFloatResult):void");
    }
}
