package android.support.v4.view;

import android.os.Build;
import android.support.compat.R;
import android.view.ViewGroup;

public final class ViewGroupCompat {
    public static boolean isTransitionGroup(ViewGroup group) {
        if (Build.VERSION.SDK_INT >= 21) {
            return group.isTransitionGroup();
        }
        Boolean explicit = (Boolean) group.getTag(R.id.tag_transition_group);
        return ((explicit == null || !explicit.booleanValue()) && group.getBackground() == null && ViewCompat.getTransitionName(group) == null) ? false : true;
    }
}
