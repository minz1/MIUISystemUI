package android.support.v4.media;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaSession2;
import java.util.ArrayList;
import java.util.List;

class MediaUtils2 {
    static final MediaBrowserServiceCompat.BrowserRoot sDefaultBrowserRoot = new MediaBrowserServiceCompat.BrowserRoot("android.media.MediaLibraryService2", null);

    static List<MediaItem2> convertToMediaItem2List(Parcelable[] itemParcelableList) {
        List<MediaItem2> playlist = new ArrayList<>();
        if (itemParcelableList != null) {
            for (int i = 0; i < itemParcelableList.length; i++) {
                if (itemParcelableList[i] instanceof Bundle) {
                    MediaItem2 item = MediaItem2.fromBundle(itemParcelableList[i]);
                    if (item != null) {
                        playlist.add(item);
                    }
                }
            }
        }
        return playlist;
    }

    static List<Bundle> convertToBundleList(Parcelable[] array) {
        if (array == null) {
            return null;
        }
        List<Bundle> bundleList = new ArrayList<>();
        for (Bundle add : array) {
            bundleList.add(add);
        }
        return bundleList;
    }

    static List<MediaSession2.CommandButton> convertToCommandButtonList(Parcelable[] list) {
        List<MediaSession2.CommandButton> layout = new ArrayList<>();
        for (int i = 0; i < list.length; i++) {
            if (list[i] instanceof Bundle) {
                MediaSession2.CommandButton button = MediaSession2.CommandButton.fromBundle(list[i]);
                if (button != null) {
                    layout.add(button);
                }
            }
        }
        return layout;
    }
}
