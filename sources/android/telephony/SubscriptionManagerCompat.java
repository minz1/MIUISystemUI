package android.telephony;

public class SubscriptionManagerCompat {
    public static int getSlotIndex(int subId) {
        return SubscriptionManager.getSlotIndex(subId);
    }
}
