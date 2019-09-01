package com.android.systemui.qs.tileimpl;

import android.content.Context;
import android.util.Log;
import com.android.systemui.plugins.qs.QSFactory;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.AutoBrightnessTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.DataSaverTile;
import com.android.systemui.qs.tiles.DriveModeTile;
import com.android.systemui.qs.tiles.EditTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.GpsTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.MuteTile;
import com.android.systemui.qs.tiles.NfcTile;
import com.android.systemui.qs.tiles.PaperModeTile;
import com.android.systemui.qs.tiles.PowerModeTile;
import com.android.systemui.qs.tiles.PowerSaverExtremeTile;
import com.android.systemui.qs.tiles.PowerSaverTile;
import com.android.systemui.qs.tiles.QuietModeTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.ScreenButtonTile;
import com.android.systemui.qs.tiles.ScreenLockTile;
import com.android.systemui.qs.tiles.ScreenShotTile;
import com.android.systemui.qs.tiles.SyncTile;
import com.android.systemui.qs.tiles.UserTile;
import com.android.systemui.qs.tiles.VibrateTile;
import com.android.systemui.qs.tiles.WifiTile;

public class QSFactoryImpl implements QSFactory {
    private final QSTileHost mHost;

    public QSFactoryImpl(QSTileHost host) {
        this.mHost = host;
    }

    public QSTile createTile(String tileSpec) {
        return createTileInternal(tileSpec);
    }

    private QSTileImpl createTileInternal(String tileSpec) {
        if (tileSpec.equals("wifi")) {
            return new WifiTile(this.mHost);
        }
        if (tileSpec.equals("bt")) {
            return new BluetoothTile(this.mHost);
        }
        if (tileSpec.equals("cell")) {
            return new CellularTile(this.mHost);
        }
        if (tileSpec.equals("airplane")) {
            return new AirplaneModeTile(this.mHost);
        }
        if (tileSpec.equals("rotation")) {
            return new RotationLockTile(this.mHost);
        }
        if (tileSpec.equals("flashlight")) {
            return new FlashlightTile(this.mHost);
        }
        if (tileSpec.equals("gps")) {
            return new GpsTile(this.mHost);
        }
        if (tileSpec.equals("hotspot")) {
            return new HotspotTile(this.mHost);
        }
        if (tileSpec.equals("user")) {
            return new UserTile(this.mHost);
        }
        if (tileSpec.equals("saver")) {
            return new DataSaverTile(this.mHost);
        }
        if (tileSpec.equals("nfc")) {
            return new NfcTile(this.mHost);
        }
        if (tileSpec.equals("screenlock")) {
            return new ScreenLockTile(this.mHost);
        }
        if (tileSpec.equals("screenshot")) {
            return new ScreenShotTile(this.mHost);
        }
        if (tileSpec.equals("papermode")) {
            return new PaperModeTile(this.mHost);
        }
        if (tileSpec.equals("autobrightness")) {
            return new AutoBrightnessTile(this.mHost);
        }
        if (tileSpec.equals("vibrate")) {
            return new VibrateTile(this.mHost);
        }
        if (tileSpec.equals("sync")) {
            return new SyncTile(this.mHost);
        }
        if (tileSpec.equals("quietmode")) {
            return new QuietModeTile(this.mHost);
        }
        if (tileSpec.equals("mute")) {
            return new MuteTile(this.mHost);
        }
        if (tileSpec.equals("edit")) {
            return new EditTile(this.mHost);
        }
        if (tileSpec.equals("powermode")) {
            return new PowerModeTile(this.mHost);
        }
        if (tileSpec.equals("screenbutton")) {
            return new ScreenButtonTile(this.mHost);
        }
        if (tileSpec.equals("batterysaver")) {
            return new PowerSaverTile(this.mHost);
        }
        if (tileSpec.equals("extremebatterysaver")) {
            return new PowerSaverExtremeTile(this.mHost);
        }
        if (tileSpec.equals("drivemode")) {
            return new DriveModeTile(this.mHost);
        }
        if (tileSpec.startsWith("intent(")) {
            return IntentTile.create(this.mHost, tileSpec);
        }
        if (tileSpec.startsWith("custom(")) {
            return CustomTile.create(this.mHost, tileSpec);
        }
        Log.w("QSFactory", "Bad tile spec: " + tileSpec);
        return null;
    }

    public QSTileView createTileView(QSTile tile, boolean collapsedView) {
        Context context = this.mHost.getContext();
        QSIconView icon = tile.createTileView(context);
        if (collapsedView) {
            return new QSTileBaseView(context, icon, collapsedView);
        }
        return new QSTileView(context, icon);
    }

    public int getVersion() {
        return -1;
    }

    public void onCreate(Context sysuiContext, Context pluginContext) {
    }

    public void onDestroy() {
    }
}
