package android.service.notification;

import android.service.notification.NotificationListenerService;
import java.util.List;

public final class RankingCompat {
    public static List<SnoozeCriterion> getSnoozeCriteria(NotificationListenerService.Ranking ranking) {
        return ranking.getSnoozeCriteria();
    }
}
