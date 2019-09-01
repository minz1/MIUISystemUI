package com.android.systemui.statusbar.phone;

public class VelocityTrackerFactory {
    /* JADX WARNING: Removed duplicated region for block: B:12:0x0032  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x004e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static com.android.systemui.statusbar.phone.VelocityTrackerInterface obtain(android.content.Context r4) {
        /*
            android.content.res.Resources r0 = r4.getResources()
            r1 = 2131822390(0x7f110736, float:1.927755E38)
            java.lang.String r0 = r0.getString(r1)
            int r1 = r0.hashCode()
            r2 = 104998702(0x642272e, float:3.651613E-35)
            if (r1 == r2) goto L_0x0024
            r2 = 1874684019(0x6fbd6873, float:1.1723788E29)
            if (r1 == r2) goto L_0x001a
            goto L_0x002e
        L_0x001a:
            java.lang.String r1 = "platform"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L_0x002e
            r1 = 1
            goto L_0x002f
        L_0x0024:
            java.lang.String r1 = "noisy"
            boolean r1 = r0.equals(r1)
            if (r1 == 0) goto L_0x002e
            r1 = 0
            goto L_0x002f
        L_0x002e:
            r1 = -1
        L_0x002f:
            switch(r1) {
                case 0: goto L_0x004e;
                case 1: goto L_0x0049;
                default: goto L_0x0032;
            }
        L_0x0032:
            java.lang.IllegalStateException r1 = new java.lang.IllegalStateException
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Invalid tracker: "
            r2.append(r3)
            r2.append(r0)
            java.lang.String r2 = r2.toString()
            r1.<init>(r2)
            throw r1
        L_0x0049:
            com.android.systemui.statusbar.phone.PlatformVelocityTracker r1 = com.android.systemui.statusbar.phone.PlatformVelocityTracker.obtain()
            return r1
        L_0x004e:
            com.android.systemui.statusbar.phone.NoisyVelocityTracker r1 = com.android.systemui.statusbar.phone.NoisyVelocityTracker.obtain()
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.phone.VelocityTrackerFactory.obtain(android.content.Context):com.android.systemui.statusbar.phone.VelocityTrackerInterface");
    }
}
