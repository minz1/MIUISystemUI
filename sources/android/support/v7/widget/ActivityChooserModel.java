package android.support.v7.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.DataSetObservable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlSerializer;

class ActivityChooserModel extends DataSetObservable {
    static final String LOG_TAG = ActivityChooserModel.class.getSimpleName();
    private static final Map<String, ActivityChooserModel> sDataModelRegistry = new HashMap();
    private static final Object sRegistryLock = new Object();
    private final List<ActivityResolveInfo> mActivities;
    private OnChooseActivityListener mActivityChoserModelPolicy;
    private ActivitySorter mActivitySorter;
    boolean mCanReadHistoricalData;
    final Context mContext;
    private final List<HistoricalRecord> mHistoricalRecords;
    private boolean mHistoricalRecordsChanged;
    final String mHistoryFileName;
    private int mHistoryMaxSize;
    private final Object mInstanceLock;
    private Intent mIntent;
    private boolean mReadShareHistoryCalled;
    private boolean mReloadActivities;

    public static final class ActivityResolveInfo implements Comparable<ActivityResolveInfo> {
        public final ResolveInfo resolveInfo;
        public float weight;

        public ActivityResolveInfo(ResolveInfo resolveInfo2) {
            this.resolveInfo = resolveInfo2;
        }

        public int hashCode() {
            return 31 + Float.floatToIntBits(this.weight);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass() && Float.floatToIntBits(this.weight) == Float.floatToIntBits(((ActivityResolveInfo) obj).weight)) {
                return true;
            }
            return false;
        }

        public int compareTo(ActivityResolveInfo another) {
            return Float.floatToIntBits(another.weight) - Float.floatToIntBits(this.weight);
        }

        public String toString() {
            return "[" + "resolveInfo:" + this.resolveInfo.toString() + "; weight:" + new BigDecimal((double) this.weight) + "]";
        }
    }

    public interface ActivitySorter {
        void sort(Intent intent, List<ActivityResolveInfo> list, List<HistoricalRecord> list2);
    }

    public static final class HistoricalRecord {
        public final ComponentName activity;
        public final long time;
        public final float weight;

        public HistoricalRecord(String activityName, long time2, float weight2) {
            this(ComponentName.unflattenFromString(activityName), time2, weight2);
        }

        public HistoricalRecord(ComponentName activityName, long time2, float weight2) {
            this.activity = activityName;
            this.time = time2;
            this.weight = weight2;
        }

        public int hashCode() {
            return (31 * ((31 * ((31 * 1) + (this.activity == null ? 0 : this.activity.hashCode()))) + ((int) (this.time ^ (this.time >>> 32))))) + Float.floatToIntBits(this.weight);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HistoricalRecord other = (HistoricalRecord) obj;
            if (this.activity == null) {
                if (other.activity != null) {
                    return false;
                }
            } else if (!this.activity.equals(other.activity)) {
                return false;
            }
            if (this.time == other.time && Float.floatToIntBits(this.weight) == Float.floatToIntBits(other.weight)) {
                return true;
            }
            return false;
        }

        public String toString() {
            return "[" + "; activity:" + this.activity + "; time:" + this.time + "; weight:" + new BigDecimal((double) this.weight) + "]";
        }
    }

    public interface OnChooseActivityListener {
        boolean onChooseActivity(ActivityChooserModel activityChooserModel, Intent intent);
    }

    private final class PersistHistoryAsyncTask extends AsyncTask<Object, Void, Void> {
        PersistHistoryAsyncTask() {
        }

        public Void doInBackground(Object... args) {
            List<HistoricalRecord> historicalRecords = args[0];
            try {
                FileOutputStream fos = ActivityChooserModel.this.mContext.openFileOutput(args[1], 0);
                XmlSerializer serializer = Xml.newSerializer();
                try {
                    serializer.setOutput(fos, null);
                    serializer.startDocument("UTF-8", true);
                    serializer.startTag(null, "historical-records");
                    int recordCount = historicalRecords.size();
                    for (int i = 0; i < recordCount; i++) {
                        HistoricalRecord record = historicalRecords.remove(0);
                        serializer.startTag(null, "historical-record");
                        serializer.attribute(null, "activity", record.activity.flattenToString());
                        serializer.attribute(null, "time", String.valueOf(record.time));
                        serializer.attribute(null, "weight", String.valueOf(record.weight));
                        serializer.endTag(null, "historical-record");
                    }
                    serializer.endTag(null, "historical-records");
                    serializer.endDocument();
                    ActivityChooserModel.this.mCanReadHistoricalData = true;
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                        }
                    }
                } catch (IllegalArgumentException iae) {
                    Log.e(ActivityChooserModel.LOG_TAG, "Error writing historical record file: " + ActivityChooserModel.this.mHistoryFileName, iae);
                    ActivityChooserModel.this.mCanReadHistoricalData = true;
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IllegalStateException ise) {
                    Log.e(ActivityChooserModel.LOG_TAG, "Error writing historical record file: " + ActivityChooserModel.this.mHistoryFileName, ise);
                    ActivityChooserModel.this.mCanReadHistoricalData = true;
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException ioe) {
                    Log.e(ActivityChooserModel.LOG_TAG, "Error writing historical record file: " + ActivityChooserModel.this.mHistoryFileName, ioe);
                    ActivityChooserModel.this.mCanReadHistoricalData = true;
                    if (fos != null) {
                        fos.close();
                    }
                } catch (Throwable th) {
                    ActivityChooserModel.this.mCanReadHistoricalData = true;
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e2) {
                        }
                    }
                    throw th;
                }
                return null;
            } catch (FileNotFoundException fnfe) {
                Log.e(ActivityChooserModel.LOG_TAG, "Error writing historical record file: " + historyFileName, fnfe);
                return null;
            }
        }
    }

    public int getActivityCount() {
        int size;
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            size = this.mActivities.size();
        }
        return size;
    }

    public ResolveInfo getActivity(int index) {
        ResolveInfo resolveInfo;
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            resolveInfo = this.mActivities.get(index).resolveInfo;
        }
        return resolveInfo;
    }

    public int getActivityIndex(ResolveInfo activity) {
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            List<ActivityResolveInfo> activities = this.mActivities;
            int activityCount = activities.size();
            for (int i = 0; i < activityCount; i++) {
                if (activities.get(i).resolveInfo == activity) {
                    return i;
                }
            }
            return -1;
        }
    }

    public Intent chooseActivity(int index) {
        synchronized (this.mInstanceLock) {
            if (this.mIntent == null) {
                return null;
            }
            ensureConsistentState();
            ActivityResolveInfo chosenActivity = this.mActivities.get(index);
            ComponentName chosenName = new ComponentName(chosenActivity.resolveInfo.activityInfo.packageName, chosenActivity.resolveInfo.activityInfo.name);
            Intent choiceIntent = new Intent(this.mIntent);
            choiceIntent.setComponent(chosenName);
            if (this.mActivityChoserModelPolicy != null) {
                if (this.mActivityChoserModelPolicy.onChooseActivity(this, new Intent(choiceIntent))) {
                    return null;
                }
            }
            addHistoricalRecord(new HistoricalRecord(chosenName, System.currentTimeMillis(), 1.0f));
            return choiceIntent;
        }
    }

    public ResolveInfo getDefaultActivity() {
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            if (this.mActivities.isEmpty()) {
                return null;
            }
            ResolveInfo resolveInfo = this.mActivities.get(0).resolveInfo;
            return resolveInfo;
        }
    }

    public void setDefaultActivity(int index) {
        float weight;
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            ActivityResolveInfo newDefaultActivity = this.mActivities.get(index);
            ActivityResolveInfo oldDefaultActivity = this.mActivities.get(0);
            if (oldDefaultActivity != null) {
                weight = (oldDefaultActivity.weight - newDefaultActivity.weight) + 5.0f;
            } else {
                weight = 1.0f;
            }
            addHistoricalRecord(new HistoricalRecord(new ComponentName(newDefaultActivity.resolveInfo.activityInfo.packageName, newDefaultActivity.resolveInfo.activityInfo.name), System.currentTimeMillis(), weight));
        }
    }

    private void persistHistoricalDataIfNeeded() {
        if (!this.mReadShareHistoryCalled) {
            throw new IllegalStateException("No preceding call to #readHistoricalData");
        } else if (this.mHistoricalRecordsChanged) {
            this.mHistoricalRecordsChanged = false;
            if (!TextUtils.isEmpty(this.mHistoryFileName)) {
                new PersistHistoryAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[]{new ArrayList(this.mHistoricalRecords), this.mHistoryFileName});
            }
        }
    }

    public int getHistorySize() {
        int size;
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            size = this.mHistoricalRecords.size();
        }
        return size;
    }

    private void ensureConsistentState() {
        boolean stateChanged = loadActivitiesIfNeeded() | readHistoricalDataIfNeeded();
        pruneExcessiveHistoricalRecordsIfNeeded();
        if (stateChanged) {
            sortActivitiesIfNeeded();
            notifyChanged();
        }
    }

    private boolean sortActivitiesIfNeeded() {
        if (this.mActivitySorter == null || this.mIntent == null || this.mActivities.isEmpty() || this.mHistoricalRecords.isEmpty()) {
            return false;
        }
        this.mActivitySorter.sort(this.mIntent, this.mActivities, Collections.unmodifiableList(this.mHistoricalRecords));
        return true;
    }

    private boolean loadActivitiesIfNeeded() {
        if (!this.mReloadActivities || this.mIntent == null) {
            return false;
        }
        this.mReloadActivities = false;
        this.mActivities.clear();
        List<ResolveInfo> resolveInfos = this.mContext.getPackageManager().queryIntentActivities(this.mIntent, 0);
        int resolveInfoCount = resolveInfos.size();
        for (int i = 0; i < resolveInfoCount; i++) {
            this.mActivities.add(new ActivityResolveInfo(resolveInfos.get(i)));
        }
        return true;
    }

    private boolean readHistoricalDataIfNeeded() {
        if (!this.mCanReadHistoricalData || !this.mHistoricalRecordsChanged || TextUtils.isEmpty(this.mHistoryFileName)) {
            return false;
        }
        this.mCanReadHistoricalData = false;
        this.mReadShareHistoryCalled = true;
        readHistoricalDataImpl();
        return true;
    }

    private boolean addHistoricalRecord(HistoricalRecord historicalRecord) {
        boolean added = this.mHistoricalRecords.add(historicalRecord);
        if (added) {
            this.mHistoricalRecordsChanged = true;
            pruneExcessiveHistoricalRecordsIfNeeded();
            persistHistoricalDataIfNeeded();
            sortActivitiesIfNeeded();
            notifyChanged();
        }
        return added;
    }

    private void pruneExcessiveHistoricalRecordsIfNeeded() {
        int pruneCount = this.mHistoricalRecords.size() - this.mHistoryMaxSize;
        if (pruneCount > 0) {
            this.mHistoricalRecordsChanged = true;
            for (int i = 0; i < pruneCount; i++) {
                HistoricalRecord remove = this.mHistoricalRecords.remove(0);
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x003b, code lost:
        if (r1 == null) goto L_0x00ca;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:?, code lost:
        r1.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readHistoricalDataImpl() {
        /*
            r12 = this;
            r0 = 0
            r1 = r0
            android.content.Context r2 = r12.mContext     // Catch:{ FileNotFoundException -> 0x00d3 }
            java.lang.String r3 = r12.mHistoryFileName     // Catch:{ FileNotFoundException -> 0x00d3 }
            java.io.FileInputStream r2 = r2.openFileInput(r3)     // Catch:{ FileNotFoundException -> 0x00d3 }
            r1 = r2
            org.xmlpull.v1.XmlPullParser r2 = android.util.Xml.newPullParser()     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            java.lang.String r3 = "UTF-8"
            r2.setInput(r1, r3)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            r3 = 0
        L_0x0016:
            r4 = 1
            if (r3 == r4) goto L_0x0022
            r5 = 2
            if (r3 == r5) goto L_0x0022
            int r4 = r2.next()     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            r3 = r4
            goto L_0x0016
        L_0x0022:
            java.lang.String r5 = "historical-records"
            java.lang.String r6 = r2.getName()     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            boolean r5 = r5.equals(r6)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            if (r5 == 0) goto L_0x0080
            java.util.List<android.support.v7.widget.ActivityChooserModel$HistoricalRecord> r5 = r12.mHistoricalRecords     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            r5.clear()     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
        L_0x0033:
            int r6 = r2.next()     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            r3 = r6
            if (r3 != r4) goto L_0x0042
            if (r1 == 0) goto L_0x00ca
            r1.close()     // Catch:{ IOException -> 0x00c8 }
            goto L_0x00c7
        L_0x0042:
            r6 = 3
            if (r3 == r6) goto L_0x0033
            r6 = 4
            if (r3 != r6) goto L_0x0049
            goto L_0x0033
        L_0x0049:
            java.lang.String r6 = r2.getName()     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            java.lang.String r7 = "historical-record"
            boolean r7 = r7.equals(r6)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            if (r7 == 0) goto L_0x0078
            java.lang.String r7 = "activity"
            java.lang.String r7 = r2.getAttributeValue(r0, r7)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            java.lang.String r8 = "time"
            java.lang.String r8 = r2.getAttributeValue(r0, r8)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            long r8 = java.lang.Long.parseLong(r8)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            java.lang.String r10 = "weight"
            java.lang.String r10 = r2.getAttributeValue(r0, r10)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            float r10 = java.lang.Float.parseFloat(r10)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            android.support.v7.widget.ActivityChooserModel$HistoricalRecord r11 = new android.support.v7.widget.ActivityChooserModel$HistoricalRecord     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            r11.<init>((java.lang.String) r7, (long) r8, (float) r10)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            r5.add(r11)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            goto L_0x0033
        L_0x0078:
            org.xmlpull.v1.XmlPullParserException r0 = new org.xmlpull.v1.XmlPullParserException     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            java.lang.String r4 = "Share records file not well-formed."
            r0.<init>(r4)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            throw r0     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
        L_0x0080:
            org.xmlpull.v1.XmlPullParserException r0 = new org.xmlpull.v1.XmlPullParserException     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            java.lang.String r4 = "Share records file does not start with historical-records tag."
            r0.<init>(r4)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
            throw r0     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x008a }
        L_0x0088:
            r0 = move-exception
            goto L_0x00cb
        L_0x008a:
            r0 = move-exception
            java.lang.String r2 = LOG_TAG     // Catch:{ all -> 0x0088 }
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x0088 }
            r3.<init>()     // Catch:{ all -> 0x0088 }
            java.lang.String r4 = "Error reading historical recrod file: "
            r3.append(r4)     // Catch:{ all -> 0x0088 }
            java.lang.String r4 = r12.mHistoryFileName     // Catch:{ all -> 0x0088 }
            r3.append(r4)     // Catch:{ all -> 0x0088 }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x0088 }
            android.util.Log.e(r2, r3, r0)     // Catch:{ all -> 0x0088 }
            if (r1 == 0) goto L_0x00ca
            r1.close()     // Catch:{ IOException -> 0x00c8 }
            goto L_0x00c7
        L_0x00a9:
            r0 = move-exception
            java.lang.String r2 = LOG_TAG     // Catch:{ all -> 0x0088 }
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ all -> 0x0088 }
            r3.<init>()     // Catch:{ all -> 0x0088 }
            java.lang.String r4 = "Error reading historical recrod file: "
            r3.append(r4)     // Catch:{ all -> 0x0088 }
            java.lang.String r4 = r12.mHistoryFileName     // Catch:{ all -> 0x0088 }
            r3.append(r4)     // Catch:{ all -> 0x0088 }
            java.lang.String r3 = r3.toString()     // Catch:{ all -> 0x0088 }
            android.util.Log.e(r2, r3, r0)     // Catch:{ all -> 0x0088 }
            if (r1 == 0) goto L_0x00ca
            r1.close()     // Catch:{ IOException -> 0x00c8 }
        L_0x00c7:
            goto L_0x00ca
        L_0x00c8:
            r0 = move-exception
            goto L_0x00c7
        L_0x00ca:
            return
        L_0x00cb:
            if (r1 == 0) goto L_0x00d2
            r1.close()     // Catch:{ IOException -> 0x00d1 }
            goto L_0x00d2
        L_0x00d1:
            r2 = move-exception
        L_0x00d2:
            throw r0
        L_0x00d3:
            r0 = move-exception
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.ActivityChooserModel.readHistoricalDataImpl():void");
    }
}
