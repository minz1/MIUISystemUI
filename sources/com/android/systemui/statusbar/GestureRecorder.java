package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class GestureRecorder {
    public static final String TAG = GestureRecorder.class.getSimpleName();
    private Gesture mCurrentGesture;
    private LinkedList<Gesture> mGestures;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 6351) {
                GestureRecorder.this.save();
            }
        }
    };
    private int mLastSaveLen = -1;
    private String mLogfile;

    public class Gesture {
        boolean mComplete = false;
        long mDownTime = -1;
        private LinkedList<Record> mRecords = new LinkedList<>();
        private HashSet<String> mTags = new HashSet<>();

        public class MotionEventRecord extends Record {
            public MotionEvent event;

            public MotionEventRecord(long when, MotionEvent event2) {
                super();
                this.time = when;
                this.event = MotionEvent.obtain(event2);
            }

            /* access modifiers changed from: package-private */
            public String actionName(int action) {
                switch (action) {
                    case 0:
                        return "down";
                    case 1:
                        return "up";
                    case 2:
                        return "move";
                    case 3:
                        return "cancel";
                    default:
                        return String.valueOf(action);
                }
            }

            public String toJson() {
                return String.format("{\"type\":\"motion\", \"time\":%d, \"action\":\"%s\", \"x\":%.2f, \"y\":%.2f, \"s\":%.2f, \"p\":%.2f}", new Object[]{Long.valueOf(this.time), actionName(this.event.getAction()), Float.valueOf(this.event.getRawX()), Float.valueOf(this.event.getRawY()), Float.valueOf(this.event.getSize()), Float.valueOf(this.event.getPressure())});
            }
        }

        public abstract class Record {
            long time;

            public abstract String toJson();

            public Record() {
            }
        }

        public class TagRecord extends Record {
            public String info;
            public String tag;

            public TagRecord(long when, String tag2, String info2) {
                super();
                this.time = when;
                this.tag = tag2;
                this.info = info2;
            }

            public String toJson() {
                return String.format("{\"type\":\"tag\", \"time\":%d, \"tag\":\"%s\", \"info\":\"%s\"}", new Object[]{Long.valueOf(this.time), this.tag, this.info});
            }
        }

        public Gesture() {
        }

        public void add(MotionEvent ev) {
            this.mRecords.add(new MotionEventRecord(ev.getEventTime(), ev));
            if (this.mDownTime < 0) {
                this.mDownTime = ev.getDownTime();
            } else if (this.mDownTime != ev.getDownTime()) {
                String str = GestureRecorder.TAG;
                Log.w(str, "Assertion failure in GestureRecorder: event downTime (" + ev.getDownTime() + ") does not match gesture downTime (" + this.mDownTime + ")");
            }
            int actionMasked = ev.getActionMasked();
            if (actionMasked == 1 || actionMasked == 3) {
                this.mComplete = true;
            }
        }

        public void tag(long when, String tag, String info) {
            LinkedList<Record> linkedList = this.mRecords;
            TagRecord tagRecord = new TagRecord(when, tag, info);
            linkedList.add(tagRecord);
            this.mTags.add(tag);
        }

        public boolean isComplete() {
            return this.mComplete;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            sb.append("[");
            Iterator it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = (Record) it.next();
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(r.toJson());
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public GestureRecorder(String filename) {
        this.mLogfile = filename;
        this.mGestures = new LinkedList<>();
        this.mCurrentGesture = null;
    }

    public void add(MotionEvent ev) {
        synchronized (this.mGestures) {
            if (this.mCurrentGesture == null || this.mCurrentGesture.isComplete()) {
                this.mCurrentGesture = new Gesture();
                this.mGestures.add(this.mCurrentGesture);
            }
            this.mCurrentGesture.add(ev);
        }
        saveLater();
    }

    public void tag(long when, String tag, String info) {
        synchronized (this.mGestures) {
            if (this.mCurrentGesture == null) {
                this.mCurrentGesture = new Gesture();
                this.mGestures.add(this.mCurrentGesture);
            }
            this.mCurrentGesture.tag(when, tag, info);
        }
        saveLater();
    }

    public void tag(String tag, String info) {
        tag(SystemClock.uptimeMillis(), tag, info);
    }

    public String toJsonLocked() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("[");
        int count = 0;
        Iterator it = this.mGestures.iterator();
        while (it.hasNext()) {
            Gesture g = (Gesture) it.next();
            if (g.isComplete()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(g.toJson());
                count++;
            }
        }
        this.mLastSaveLen = count;
        sb.append("]");
        return sb.toString();
    }

    public void saveLater() {
        this.mHandler.removeMessages(6351);
        this.mHandler.sendEmptyMessageDelayed(6351, 5000);
    }

    public void save() {
        synchronized (this.mGestures) {
            try {
                BufferedWriter w = new BufferedWriter(new FileWriter(this.mLogfile, true));
                w.append(toJsonLocked() + "\n");
                w.close();
                this.mGestures.clear();
                if (this.mCurrentGesture != null && !this.mCurrentGesture.isComplete()) {
                    this.mGestures.add(this.mCurrentGesture);
                }
                Log.v(TAG, String.format("Wrote %d complete gestures to %s", new Object[]{Integer.valueOf(this.mLastSaveLen), this.mLogfile}));
            } catch (IOException e) {
                Log.e(TAG, String.format("Couldn't write gestures to %s", new Object[]{this.mLogfile}), e);
                this.mLastSaveLen = -1;
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        save();
        if (this.mLastSaveLen >= 0) {
            pw.println(String.valueOf(this.mLastSaveLen) + " gestures written to " + this.mLogfile);
            return;
        }
        pw.println("error writing gestures");
    }
}
