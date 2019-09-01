package android.widget;

import android.app.PendingIntent;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

public class AbstractOnClickHandler extends RemoteViews.OnClickHandler {
    public boolean onClickHandler(View view, PendingIntent pendingIntent, Intent fillInIntent, int launchStackId) {
        return AbstractOnClickHandler.super.onClickHandler(view, pendingIntent, fillInIntent, launchStackId);
    }
}
