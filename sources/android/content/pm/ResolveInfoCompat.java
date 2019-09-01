package android.content.pm;

public class ResolveInfoCompat {
    public static ComponentInfo getComponentInfo(ResolveInfo ri) {
        if (ri.activityInfo != null) {
            return ri.activityInfo;
        }
        if (ri.serviceInfo != null) {
            return ri.serviceInfo;
        }
        if (ri.providerInfo != null) {
            return ri.providerInfo;
        }
        throw new IllegalStateException("Missing ComponentInfo!");
    }
}
