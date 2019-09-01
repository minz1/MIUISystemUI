package android.service.quicksettings;

import android.content.ComponentName;

public class TileCompat {
    public static Tile newTile(ComponentName componentName) {
        return new Tile();
    }
}
