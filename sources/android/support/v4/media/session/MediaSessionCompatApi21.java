package android.support.v4.media.session;

import android.media.session.MediaSession;

class MediaSessionCompatApi21 {

    static class QueueItem {
        public static Object getDescription(Object queueItem) {
            return ((MediaSession.QueueItem) queueItem).getDescription();
        }

        public static long getQueueId(Object queueItem) {
            return ((MediaSession.QueueItem) queueItem).getQueueId();
        }
    }
}
