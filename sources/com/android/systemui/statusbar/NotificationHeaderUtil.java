package com.android.systemui.statusbar;

import android.app.Notification;
import android.app.NotificationCompat;
import android.graphics.PorterDuff;
import android.graphics.drawable.IconCompat;
import android.text.TextUtils;
import android.view.NotificationHeaderView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.SystemUICompat;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.notification.NotificationViewWrapperCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NotificationHeaderUtil {
    private static final ResultApplicator mGreyApplicator = new ResultApplicator() {
        public void apply(View view, boolean apply) {
            NotificationHeaderView header = (NotificationHeaderView) view;
            ImageView expand = NotificationViewWrapperCompat.findExpandButtonView(view);
            applyToChild((ImageView) view.findViewById(16908294), apply, header.getOriginalIconColor());
            applyToChild(expand, apply, header.getOriginalNotificationColor());
        }

        private void applyToChild(View view, boolean shouldApply, int originalColor) {
            if (originalColor != 1) {
                ImageView imageView = (ImageView) view;
                imageView.getDrawable().mutate();
                if (shouldApply) {
                    imageView.getDrawable().setColorFilter(view.getContext().getColor(SystemUICompat.getNotificationDefaultColor()), PorterDuff.Mode.SRC_ATOP);
                    return;
                }
                imageView.getDrawable().setColorFilter(originalColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    };
    private static final IconComparator sGreyComparator = new IconComparator() {
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            return !hasSameIcon(parentData, childData) || hasSameColor(parentData, childData);
        }
    };
    private static final DataExtractor sIconExtractor = new DataExtractor() {
        public Object extractData(ExpandableNotificationRow row) {
            return row.getStatusBarNotification().getNotification();
        }
    };
    private static final IconComparator sIconVisibilityComparator = new IconComparator() {
        public boolean compare(View parent, View child, Object parentData, Object childData) {
            return hasSameIcon(parentData, childData) && hasSameColor(parentData, childData);
        }
    };
    /* access modifiers changed from: private */
    public static final TextViewComparator sTextViewComparator = new TextViewComparator();
    /* access modifiers changed from: private */
    public static final VisibilityApplicator sVisibilityApplicator = new VisibilityApplicator();
    private final ArrayList<HeaderProcessor> mComparators = new ArrayList<>();
    private final HashSet<Integer> mDividers = new HashSet<>();
    private final ExpandableNotificationRow mRow;

    private interface DataExtractor {
        Object extractData(ExpandableNotificationRow expandableNotificationRow);
    }

    private static class HeaderProcessor {
        private final ResultApplicator mApplicator;
        private boolean mApply;
        private ViewComparator mComparator;
        private final DataExtractor mExtractor;
        private final int mId;
        private Object mParentData;
        private final ExpandableNotificationRow mParentRow;
        private View mParentView;

        public static HeaderProcessor forTextView(ExpandableNotificationRow row, int id) {
            HeaderProcessor headerProcessor = new HeaderProcessor(row, id, null, NotificationHeaderUtil.sTextViewComparator, NotificationHeaderUtil.sVisibilityApplicator);
            return headerProcessor;
        }

        HeaderProcessor(ExpandableNotificationRow row, int id, DataExtractor extractor, ViewComparator comparator, ResultApplicator applicator) {
            this.mId = id;
            this.mExtractor = extractor;
            this.mApplicator = applicator;
            this.mComparator = comparator;
            this.mParentRow = row;
        }

        public void init() {
            this.mParentView = this.mParentRow.getNotificationHeader().findViewById(this.mId);
            this.mParentData = this.mExtractor == null ? null : this.mExtractor.extractData(this.mParentRow);
            this.mApply = !this.mComparator.isEmpty(this.mParentView);
        }

        public void compareToHeader(ExpandableNotificationRow row) {
            if (this.mApply) {
                NotificationHeaderView header = row.getNotificationHeader();
                if (header == null) {
                    this.mApply = false;
                } else {
                    this.mApply = this.mComparator.compare(this.mParentView, header.findViewById(this.mId), this.mParentData, this.mExtractor == null ? null : this.mExtractor.extractData(row));
                }
            }
        }

        public void apply(ExpandableNotificationRow row) {
            apply(row, false);
        }

        public void apply(ExpandableNotificationRow row, boolean reset) {
            boolean apply = this.mApply && !reset;
            if (row.isSummaryWithChildren()) {
                applyToView(apply, row.getNotificationHeader());
                return;
            }
            applyToView(apply, row.getPrivateLayout().getContractedChild());
            applyToView(apply, row.getPrivateLayout().getHeadsUpChild());
            applyToView(apply, row.getPrivateLayout().getExpandedChild());
        }

        private void applyToView(boolean apply, View parent) {
            if (parent != null) {
                View view = parent.findViewById(this.mId);
                if (view != null && !this.mComparator.isEmpty(view)) {
                    this.mApplicator.apply(view, apply);
                }
            }
        }
    }

    private static abstract class IconComparator implements ViewComparator {
        private IconComparator() {
        }

        public boolean compare(View parent, View child, Object parentData, Object childData) {
            return false;
        }

        /* access modifiers changed from: protected */
        public boolean hasSameIcon(Object parentData, Object childData) {
            return IconCompat.sameAs(((Notification) parentData).getSmallIcon(), ((Notification) childData).getSmallIcon());
        }

        /* access modifiers changed from: protected */
        public boolean hasSameColor(Object parentData, Object childData) {
            return ((Notification) parentData).color == ((Notification) childData).color;
        }

        public boolean isEmpty(View view) {
            return false;
        }
    }

    private interface ResultApplicator {
        void apply(View view, boolean z);
    }

    private static class TextViewComparator implements ViewComparator {
        private TextViewComparator() {
        }

        public boolean compare(View parent, View child, Object parentData, Object childData) {
            return ((TextView) parent).getText().equals(((TextView) child).getText());
        }

        public boolean isEmpty(View view) {
            return TextUtils.isEmpty(((TextView) view).getText());
        }
    }

    private interface ViewComparator {
        boolean compare(View view, View view2, Object obj, Object obj2);

        boolean isEmpty(View view);
    }

    private static class VisibilityApplicator implements ResultApplicator {
        private VisibilityApplicator() {
        }

        public void apply(View view, boolean apply) {
            view.setVisibility(apply ? 8 : 0);
        }
    }

    public NotificationHeaderUtil(ExpandableNotificationRow row) {
        this.mRow = row;
        ArrayList<HeaderProcessor> arrayList = this.mComparators;
        HeaderProcessor headerProcessor = new HeaderProcessor(this.mRow, 16908294, sIconExtractor, sIconVisibilityComparator, sVisibilityApplicator);
        arrayList.add(headerProcessor);
        ArrayList<HeaderProcessor> arrayList2 = this.mComparators;
        HeaderProcessor headerProcessor2 = new HeaderProcessor(this.mRow, 16909137, sIconExtractor, sGreyComparator, mGreyApplicator);
        arrayList2.add(headerProcessor2);
        if (NotificationUtil.showGoogleStyle()) {
            ArrayList<HeaderProcessor> arrayList3 = this.mComparators;
            HeaderProcessor headerProcessor3 = new HeaderProcessor(this.mRow, 16909233, null, new ViewComparator() {
                public boolean compare(View parent, View child, Object parentData, Object childData) {
                    return parent.getVisibility() != 8;
                }

                public boolean isEmpty(View view) {
                    boolean z = false;
                    if (!(view instanceof ImageView)) {
                        return false;
                    }
                    if (((ImageView) view).getDrawable() == null) {
                        z = true;
                    }
                    return z;
                }
            }, sVisibilityApplicator);
            arrayList3.add(headerProcessor3);
        }
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, 16908721));
        this.mComparators.add(HeaderProcessor.forTextView(this.mRow, 16908947));
        this.mDividers.add(16908948);
        this.mDividers.add(16909444);
    }

    public void updateChildrenHeaderAppearance() {
        List<ExpandableNotificationRow> notificationChildren = this.mRow.getNotificationChildren();
        if (notificationChildren != null) {
            for (int compI = 0; compI < this.mComparators.size(); compI++) {
                this.mComparators.get(compI).init();
            }
            for (int i = 0; i < notificationChildren.size(); i++) {
                ExpandableNotificationRow row = notificationChildren.get(i);
                for (int compI2 = 0; compI2 < this.mComparators.size(); compI2++) {
                    this.mComparators.get(compI2).compareToHeader(row);
                }
            }
            for (int i2 = 0; i2 < notificationChildren.size(); i2++) {
                ExpandableNotificationRow row2 = notificationChildren.get(i2);
                for (int compI3 = 0; compI3 < this.mComparators.size(); compI3++) {
                    this.mComparators.get(compI3).apply(row2);
                }
                sanitizeHeaderViews(row2);
            }
        }
    }

    private void sanitizeHeaderViews(ExpandableNotificationRow row) {
        if (row.isSummaryWithChildren()) {
            sanitizeHeader(row.getNotificationHeader());
            return;
        }
        NotificationContentView layout = row.getPrivateLayout();
        sanitizeChild(layout.getContractedChild());
        sanitizeChild(layout.getHeadsUpChild());
        sanitizeChild(layout.getExpandedChild());
    }

    private void sanitizeChild(View child) {
        if (child != null) {
            sanitizeHeader(NotificationViewWrapperCompat.findNotificationHeaderView(child));
        }
    }

    private void sanitizeHeader(NotificationHeaderView rowHeader) {
        if (rowHeader != null) {
            int childCount = rowHeader.getChildCount();
            View time = rowHeader.findViewById(16909440);
            if (time != null) {
                boolean hasVisibleText = false;
                int i = 1;
                while (true) {
                    if (i >= childCount - 1) {
                        break;
                    }
                    View child = rowHeader.getChildAt(i);
                    if ((child instanceof TextView) && child.getVisibility() != 8 && !this.mDividers.contains(Integer.valueOf(child.getId())) && child != time) {
                        hasVisibleText = true;
                        break;
                    }
                    i++;
                }
                time.setVisibility((!hasVisibleText || NotificationCompat.showsTime(this.mRow.getStatusBarNotification().getNotification())) ? 0 : 8);
            }
            View left = null;
            int i2 = 1;
            while (i2 < childCount - 1) {
                View child2 = rowHeader.getChildAt(i2);
                if (this.mDividers.contains(Integer.valueOf(child2.getId()))) {
                    boolean visible = false;
                    while (true) {
                        i2++;
                        if (i2 >= childCount - 1) {
                            break;
                        }
                        View right = rowHeader.getChildAt(i2);
                        if (this.mDividers.contains(Integer.valueOf(right.getId()))) {
                            i2--;
                            break;
                        } else if (right.getVisibility() != 8 && (right instanceof TextView)) {
                            visible = left != null;
                            left = right;
                        }
                    }
                    child2.setVisibility(visible ? 0 : 8);
                } else if (child2.getVisibility() != 8 && (child2 instanceof TextView)) {
                    left = child2;
                }
                i2++;
            }
        }
    }

    public void restoreNotificationHeader(ExpandableNotificationRow row) {
        for (int compI = 0; compI < this.mComparators.size(); compI++) {
            this.mComparators.get(compI).apply(row, true);
        }
        sanitizeHeaderViews(row);
    }
}
