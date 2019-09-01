package com.android.systemui.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import java.lang.Thread;
import java.util.LinkedList;

public class NotificationPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    /* access modifiers changed from: private */
    public AudioManager mAudioManagerWithAudioFocus;
    /* access modifiers changed from: private */
    public LinkedList<Command> mCmdQueue = new LinkedList<>();
    private final Object mCompletionHandlingLock = new Object();
    private CreationAndCompletionThread mCompletionThread;
    /* access modifiers changed from: private */
    public Looper mLooper;
    /* access modifiers changed from: private */
    public int mNotificationRampTimeMs = 0;
    /* access modifiers changed from: private */
    public MediaPlayer mPlayer;
    /* access modifiers changed from: private */
    public final Object mQueueAudioFocusLock = new Object();
    private int mState = 2;
    /* access modifiers changed from: private */
    public String mTag;
    /* access modifiers changed from: private */
    public CmdThread mThread;
    private PowerManager.WakeLock mWakeLock;

    private final class CmdThread extends Thread {
        CmdThread() {
            super("NotificationPlayer-" + r3.mTag);
        }

        public void run() {
            Command cmd;
            while (true) {
                synchronized (NotificationPlayer.this.mCmdQueue) {
                    cmd = (Command) NotificationPlayer.this.mCmdQueue.removeFirst();
                }
                switch (cmd.code) {
                    case 1:
                        NotificationPlayer.this.startSound(cmd);
                        break;
                    case 2:
                        NotificationPlayer.this.stopSound(cmd);
                        break;
                }
                synchronized (NotificationPlayer.this.mCmdQueue) {
                    if (NotificationPlayer.this.mCmdQueue.size() == 0) {
                        CmdThread unused = NotificationPlayer.this.mThread = null;
                        NotificationPlayer.this.releaseWakeLock();
                        return;
                    }
                }
            }
            while (true) {
            }
        }
    }

    private static final class Command {
        AudioAttributes attributes;
        int code;
        Context context;
        boolean looping;
        long requestTime;
        Uri uri;

        private Command() {
        }

        public String toString() {
            return "{ code=" + this.code + " looping=" + this.looping + " attributes=" + this.attributes + " uri=" + this.uri + " }";
        }
    }

    private final class CreationAndCompletionThread extends Thread {
        public Command mCmd;

        public CreationAndCompletionThread(Command cmd) {
            this.mCmd = cmd;
        }

        public void run() {
            Looper.prepare();
            Looper unused = NotificationPlayer.this.mLooper = Looper.myLooper();
            synchronized (this) {
                AudioManager audioManager = (AudioManager) this.mCmd.context.getSystemService("audio");
                try {
                    MediaPlayer player = new MediaPlayer();
                    if (this.mCmd.attributes == null) {
                        this.mCmd.attributes = new AudioAttributes.Builder().setUsage(5).setContentType(4).build();
                    }
                    player.setAudioAttributes(this.mCmd.attributes);
                    player.setDataSource(this.mCmd.context, this.mCmd.uri);
                    player.setLooping(this.mCmd.looping);
                    player.setOnCompletionListener(NotificationPlayer.this);
                    player.setOnErrorListener(NotificationPlayer.this);
                    player.prepare();
                    if (this.mCmd.uri != null && this.mCmd.uri.getEncodedPath() != null && this.mCmd.uri.getEncodedPath().length() > 0 && !audioManager.isMusicActiveRemotely()) {
                        synchronized (NotificationPlayer.this.mQueueAudioFocusLock) {
                            if (NotificationPlayer.this.mAudioManagerWithAudioFocus == null) {
                                int focusGain = 3;
                                if (this.mCmd.looping) {
                                    focusGain = 1;
                                }
                                int unused2 = NotificationPlayer.this.mNotificationRampTimeMs = NotificationPlayer.getFocusRampTimeMs(focusGain, this.mCmd.attributes);
                                audioManager.requestAudioFocus(null, this.mCmd.attributes, focusGain, 0);
                                AudioManager unused3 = NotificationPlayer.this.mAudioManagerWithAudioFocus = audioManager;
                            }
                        }
                    }
                    try {
                        Thread.sleep((long) NotificationPlayer.this.mNotificationRampTimeMs);
                        String access$500 = NotificationPlayer.this.mTag;
                        Log.i(access$500, "player.start mNotificationRampTimeMs:" + NotificationPlayer.this.mNotificationRampTimeMs);
                        player.start();
                    } catch (InterruptedException e) {
                        Log.e(NotificationPlayer.this.mTag, "Exception while sleeping to sync notification playback with ducking", e);
                    }
                    if (NotificationPlayer.this.mPlayer != null) {
                        NotificationPlayer.this.mPlayer.release();
                    }
                    MediaPlayer unused4 = NotificationPlayer.this.mPlayer = player;
                } catch (Exception e2) {
                    String access$5002 = NotificationPlayer.this.mTag;
                    Log.w(access$5002, "error loading sound for " + this.mCmd.uri, e2);
                }
                notify();
            }
            Looper.loop();
        }
    }

    /* access modifiers changed from: private */
    public void startSound(Command cmd) {
        try {
            synchronized (this.mCompletionHandlingLock) {
                if (!(this.mLooper == null || this.mLooper.getThread().getState() == Thread.State.TERMINATED)) {
                    this.mLooper.quit();
                }
                this.mCompletionThread = new CreationAndCompletionThread(cmd);
                synchronized (this.mCompletionThread) {
                    this.mCompletionThread.start();
                    this.mCompletionThread.wait();
                }
            }
            long delay = SystemClock.uptimeMillis() - cmd.requestTime;
            if (delay > 1000) {
                String str = this.mTag;
                Log.w(str, "Notification sound delayed by " + delay + "msecs");
            }
        } catch (Exception e) {
            String str2 = this.mTag;
            Log.w(str2, "error loading sound for " + cmd.uri, e);
        }
    }

    /* access modifiers changed from: private */
    public void stopSound(Command cmd) {
        if (this.mPlayer != null) {
            long delay = SystemClock.uptimeMillis() - cmd.requestTime;
            if (delay > 1000) {
                String str = this.mTag;
                Log.w(str, "Notification stop delayed by " + delay + "msecs");
            }
            this.mPlayer.stop();
            this.mPlayer.release();
            this.mPlayer = null;
            synchronized (this.mQueueAudioFocusLock) {
                if (this.mAudioManagerWithAudioFocus != null) {
                    this.mAudioManagerWithAudioFocus.abandonAudioFocus(null);
                    this.mAudioManagerWithAudioFocus = null;
                }
            }
            if (!(this.mLooper == null || this.mLooper.getThread().getState() == Thread.State.TERMINATED)) {
                this.mLooper.quit();
            }
            Log.i(this.mTag, "stopSound finished.");
            return;
        }
        Log.w(this.mTag, "STOP command without a player");
    }

    public void onCompletion(MediaPlayer mp) {
        synchronized (this.mQueueAudioFocusLock) {
            if (this.mAudioManagerWithAudioFocus != null) {
                this.mAudioManagerWithAudioFocus.abandonAudioFocus(null);
                this.mAudioManagerWithAudioFocus = null;
            }
        }
        synchronized (this.mCmdQueue) {
            if (this.mCmdQueue.size() == 0) {
                synchronized (this.mCompletionHandlingLock) {
                    if (this.mLooper != null) {
                        this.mLooper.quit();
                    }
                    this.mCompletionThread = null;
                }
            }
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        String str = this.mTag;
        Log.e(str, "error " + what + " (extra=" + extra + ") playing notification");
        onCompletion(mp);
        return true;
    }

    public NotificationPlayer(String tag) {
        if (tag != null) {
            this.mTag = tag;
        } else {
            this.mTag = "NotificationPlayer";
        }
    }

    public void play(Context context, Uri uri, boolean looping, AudioAttributes attributes) {
        Command cmd = new Command();
        cmd.requestTime = SystemClock.uptimeMillis();
        cmd.code = 1;
        cmd.context = context;
        cmd.uri = uri;
        cmd.looping = looping;
        cmd.attributes = attributes;
        synchronized (this.mCmdQueue) {
            enqueueLocked(cmd);
            this.mState = 1;
        }
    }

    public void stop() {
        synchronized (this.mCmdQueue) {
            if (this.mState != 2) {
                Command cmd = new Command();
                cmd.requestTime = SystemClock.uptimeMillis();
                cmd.code = 2;
                enqueueLocked(cmd);
                this.mState = 2;
            }
        }
    }

    private void enqueueLocked(Command cmd) {
        this.mCmdQueue.add(cmd);
        if (this.mThread == null) {
            acquireWakeLock();
            this.mThread = new CmdThread();
            this.mThread.start();
        }
    }

    public void setUsesWakeLock(Context context) {
        if (this.mWakeLock == null && this.mThread == null) {
            this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, this.mTag);
            return;
        }
        throw new RuntimeException("assertion failed mWakeLock=" + this.mWakeLock + " mThread=" + this.mThread);
    }

    private void acquireWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.acquire();
        }
    }

    /* access modifiers changed from: private */
    public void releaseWakeLock() {
        if (this.mWakeLock != null) {
            this.mWakeLock.release();
        }
    }

    /* access modifiers changed from: private */
    public static int getFocusRampTimeMs(int focusGain, AudioAttributes attr) {
        switch (attr.getUsage()) {
            case 1:
            case 14:
                return 1000;
            case 2:
            case 3:
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
            case 13:
                return 500;
            case 4:
            case 6:
            case 11:
            case 12:
            case 16:
                return 700;
            default:
                return 0;
        }
    }
}
