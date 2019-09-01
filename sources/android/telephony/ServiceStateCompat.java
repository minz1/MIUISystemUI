package android.telephony;

public class ServiceStateCompat {
    public static boolean isUsingCarrierAggregation(ServiceState serviceState) {
        return serviceState.isUsingCarrierAggregation();
    }
}
