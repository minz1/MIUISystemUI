package com.android.systemui.media;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.IAudioService;
import android.media.IRingtonePlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.util.Log;
import com.android.systemui.SystemUI;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class RingtonePlayer extends SystemUI {
    /* access modifiers changed from: private */
    public static final Uri INCALL_NOTIFICATION_URI = Uri.parse("file:///system/media/audio/ui/InCallNotification.ogg");
    /* access modifiers changed from: private */
    public final NotificationPlayer mAsyncPlayer = new NotificationPlayer("RingtonePlayer");
    private IAudioService mAudioService;
    private IRingtonePlayer mCallback = new IRingtonePlayer.Stub() {
        public void play(IBinder token, Uri uri, AudioAttributes aa, float volume, boolean looping) throws RemoteException {
            Client client;
            if (!RingtonePlayer.INCALL_NOTIFICATION_URI.equals(uri)) {
                synchronized (RingtonePlayer.this.mClients) {
                    client = (Client) RingtonePlayer.this.mClients.get(token);
                    if (client == null) {
                        Client client2 = new Client(token, uri, Binder.getCallingUserHandle(), aa);
                        client = client2;
                        token.linkToDeath(client, 0);
                        RingtonePlayer.this.mClients.put(token, client);
                    }
                }
                Client client3 = client;
                client3.mRingtone.setLooping(looping);
                client3.mRingtone.setVolume(volume);
                client3.mRingtone.play();
            }
        }

        public void stop(IBinder token) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.remove(token);
            }
            if (client != null) {
                client.mToken.unlinkToDeath(client, 0);
                client.mRingtone.stop();
            }
        }

        public boolean isPlaying(IBinder token) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(token);
            }
            if (client != null) {
                return client.mRingtone.isPlaying();
            }
            return false;
        }

        public void setPlaybackProperties(IBinder token, float volume, boolean looping) {
            Client client;
            synchronized (RingtonePlayer.this.mClients) {
                client = (Client) RingtonePlayer.this.mClients.get(token);
            }
            if (client != null) {
                client.mRingtone.setVolume(volume);
                client.mRingtone.setLooping(looping);
            }
        }

        public void playAsync(Uri uri, UserHandle user, boolean looping, AudioAttributes aa) {
            if (Binder.getCallingUid() == 1000) {
                if (UserHandle.ALL.equals(user)) {
                    user = UserHandleCompat.SYSTEM;
                }
                RingtonePlayer.this.mAsyncPlayer.play(RingtonePlayer.this.getContextForUser(user), RingtonePlayer.this.fallbackNotificationUri(uri, aa), looping, aa);
                return;
            }
            throw new SecurityException("Async playback only available from system UID.");
        }

        public void stopAsync() {
            if (Binder.getCallingUid() == 1000) {
                RingtonePlayer.this.mAsyncPlayer.stop();
                return;
            }
            throw new SecurityException("Async playback only available from system UID.");
        }

        public String getTitle(Uri uri) {
            return Ringtone.getTitle(RingtonePlayer.this.getContextForUser(Binder.getCallingUserHandle()), uri, false, false);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:24:0x0066, code lost:
            r4 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:28:0x006a, code lost:
            if (r2 != null) goto L_0x006c;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:29:0x006c, code lost:
            if (r3 != null) goto L_0x006e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:?, code lost:
            r2.close();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x0072, code lost:
            r5 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:33:0x0073, code lost:
            r3.addSuppressed(r5);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:34:0x0077, code lost:
            r2.close();
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public android.os.ParcelFileDescriptor openRingtone(android.net.Uri r9) {
            /*
                r8 = this;
                android.os.UserHandle r0 = android.os.Binder.getCallingUserHandle()
                com.android.systemui.media.RingtonePlayer r1 = com.android.systemui.media.RingtonePlayer.this
                android.content.Context r1 = r1.getContextForUser(r0)
                android.content.ContentResolver r1 = r1.getContentResolver()
                java.lang.String r2 = r9.toString()
                android.net.Uri r3 = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                java.lang.String r3 = r3.toString()
                boolean r2 = r2.startsWith(r3)
                if (r2 == 0) goto L_0x007b
                java.lang.String r2 = "is_ringtone"
                java.lang.String r3 = "is_alarm"
                java.lang.String r4 = "is_notification"
                java.lang.String[] r4 = new java.lang.String[]{r2, r3, r4}
                r5 = 0
                r6 = 0
                r7 = 0
                r2 = r1
                r3 = r9
                android.database.Cursor r2 = r2.query(r3, r4, r5, r6, r7)
                r3 = 0
                boolean r4 = r2.moveToFirst()     // Catch:{ Throwable -> 0x0068 }
                if (r4 == 0) goto L_0x0060
                r4 = 0
                int r4 = r2.getInt(r4)     // Catch:{ Throwable -> 0x0068 }
                if (r4 != 0) goto L_0x004d
                r4 = 1
                int r4 = r2.getInt(r4)     // Catch:{ Throwable -> 0x0068 }
                if (r4 != 0) goto L_0x004d
                r4 = 2
                int r4 = r2.getInt(r4)     // Catch:{ Throwable -> 0x0068 }
                if (r4 == 0) goto L_0x0060
            L_0x004d:
                java.lang.String r4 = "r"
                android.os.ParcelFileDescriptor r4 = r1.openFileDescriptor(r9, r4)     // Catch:{ IOException -> 0x0059 }
                if (r2 == 0) goto L_0x0058
                r2.close()
            L_0x0058:
                return r4
            L_0x0059:
                r4 = move-exception
                java.lang.SecurityException r5 = new java.lang.SecurityException     // Catch:{ Throwable -> 0x0068 }
                r5.<init>(r4)     // Catch:{ Throwable -> 0x0068 }
                throw r5     // Catch:{ Throwable -> 0x0068 }
            L_0x0060:
                if (r2 == 0) goto L_0x007b
                r2.close()
                goto L_0x007b
            L_0x0066:
                r4 = move-exception
                goto L_0x006a
            L_0x0068:
                r3 = move-exception
                throw r3     // Catch:{ all -> 0x0066 }
            L_0x006a:
                if (r2 == 0) goto L_0x007a
                if (r3 == 0) goto L_0x0077
                r2.close()     // Catch:{ Throwable -> 0x0072 }
                goto L_0x007a
            L_0x0072:
                r5 = move-exception
                r3.addSuppressed(r5)
                goto L_0x007a
            L_0x0077:
                r2.close()
            L_0x007a:
                throw r4
            L_0x007b:
                java.lang.SecurityException r2 = new java.lang.SecurityException
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "Uri is not ringtone, alarm, or notification: "
                r3.append(r4)
                r3.append(r9)
                java.lang.String r3 = r3.toString()
                r2.<init>(r3)
                throw r2
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.media.RingtonePlayer.AnonymousClass1.openRingtone(android.net.Uri):android.os.ParcelFileDescriptor");
        }
    };
    /* access modifiers changed from: private */
    public final HashMap<IBinder, Client> mClients = new HashMap<>();

    private class Client implements IBinder.DeathRecipient {
        /* access modifiers changed from: private */
        public final Ringtone mRingtone;
        /* access modifiers changed from: private */
        public final IBinder mToken;

        public Client(IBinder token, Uri uri, UserHandle user, AudioAttributes aa) {
            this.mToken = token;
            this.mRingtone = new Ringtone(RingtonePlayer.this.getContextForUser(user), false);
            this.mRingtone.setAudioAttributes(aa);
            this.mRingtone.setUri(RingtonePlayer.this.fallbackNotificationUri(uri, aa));
        }

        public void binderDied() {
            synchronized (RingtonePlayer.this.mClients) {
                RingtonePlayer.this.mClients.remove(this.mToken);
            }
            this.mRingtone.stop();
        }
    }

    public void start() {
        this.mAsyncPlayer.setUsesWakeLock(this.mContext);
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        try {
            this.mAudioService.setRingtonePlayer(this.mCallback);
        } catch (RemoteException e) {
            Log.e("RingtonePlayer", "Problem registering RingtonePlayer: " + e);
        }
    }

    /* access modifiers changed from: private */
    public Context getContextForUser(UserHandle user) {
        if (999 == user.getIdentifier()) {
            user = UserHandleCompat.SYSTEM;
        }
        try {
            return this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /* access modifiers changed from: private */
    public Uri fallbackNotificationUri(Uri original, AudioAttributes aa) {
        if (original != null) {
            if (aa.getUsage() == 5) {
                String scheme = original.getScheme();
                if ((scheme == null || scheme.equals("file")) && !new File(original.getPath()).exists()) {
                    return RingtoneManager.getDefaultUri(2);
                }
            } else if (aa.getUsage() == 6) {
                String scheme2 = original.getScheme();
                if ((scheme2 == null || scheme2.equals("file")) && !new File(original.getPath()).exists()) {
                    return RingtoneManager.getDefaultUri(1);
                }
            }
        }
        return original;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Clients:");
        synchronized (this.mClients) {
            for (Client client : this.mClients.values()) {
                pw.print("  mToken=");
                pw.print(client.mToken);
                pw.print(" mUri=");
                pw.println(client.mRingtone.getUri());
            }
        }
    }
}
