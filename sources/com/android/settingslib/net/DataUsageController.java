package com.android.settingslib.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class DataUsageController {
    private static final boolean DEBUG = Log.isLoggable("DataUsageController", 3);
    private static final StringBuilder PERIOD_BUILDER = new StringBuilder(50);
    private static final Formatter PERIOD_FORMATTER = new Formatter(PERIOD_BUILDER, Locale.getDefault());
    private Callback mCallback;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private NetworkNameProvider mNetworkController;
    private final NetworkPolicyManager mPolicyManager = NetworkPolicyManager.from(this.mContext);
    private INetworkStatsSession mSession;
    private final INetworkStatsService mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
    private final TelephonyManager mTelephonyManager;

    public interface Callback {
        void onMobileDataEnabled(boolean z);
    }

    public static class DataUsageInfo {
        public String carrier;
        public long cycleEnd;
        public long cycleStart;
        public long limitLevel;
        public String period;
        public long startDate;
        public long usageLevel;
        public long warningLevel;
    }

    public interface NetworkNameProvider {
        String getMobileDataNetworkName();
    }

    public DataUsageController(Context context) {
        this.mContext = context;
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mConnectivityManager = ConnectivityManager.from(context);
    }

    public void setNetworkController(NetworkNameProvider networkController) {
        this.mNetworkController = networkController;
    }

    public long getDefaultWarningLevel() {
        return 1048576 * ((long) this.mContext.getResources().getInteger(17694948));
    }

    private INetworkStatsSession getSession() {
        if (this.mSession == null) {
            try {
                this.mSession = this.mStatsService.openSession();
            } catch (RemoteException e) {
                Log.w("DataUsageController", "Failed to open stats session", e);
            } catch (RuntimeException e2) {
                Log.w("DataUsageController", "Failed to open stats session", e2);
            }
        }
        return this.mSession;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    private DataUsageInfo warn(String msg) {
        Log.w("DataUsageController", "Failed to get data usage, " + msg);
        return null;
    }

    public DataUsageInfo getDataUsageInfo() {
        String subscriberId = getActiveSubscriberId(this.mContext);
        if (subscriberId == null) {
            return warn("no subscriber id");
        }
        return getDataUsageInfo(NetworkTemplate.normalize(NetworkTemplate.buildTemplateMobileAll(subscriberId), this.mTelephonyManager.getMergedSubscriberIds()));
    }

    public DataUsageInfo getDataUsageInfo(NetworkTemplate template) {
        long end;
        long end2;
        DataUsageController dataUsageController = this;
        INetworkStatsSession session = getSession();
        if (session == null) {
            return dataUsageController.warn("no stats session");
        }
        NetworkPolicy policy = findNetworkPolicy(template);
        try {
            NetworkStatsHistory history = session.getHistoryForNetwork(template, 10);
            long now = System.currentTimeMillis();
            if (policy != null) {
                try {
                    Pair<ZonedDateTime, ZonedDateTime> cycle = (Pair) NetworkPolicyManager.cycleIterator(policy).next();
                    end2 = ((ZonedDateTime) cycle.first).toInstant().toEpochMilli();
                    end = ((ZonedDateTime) cycle.second).toInstant().toEpochMilli();
                } catch (RemoteException e) {
                    INetworkStatsSession iNetworkStatsSession = session;
                    NetworkPolicy networkPolicy = policy;
                    return dataUsageController.warn("remote call failed");
                }
            } else {
                end = now;
                end2 = now - 2419200000L;
            }
            long start = end2;
            long callStart = System.currentTimeMillis();
            INetworkStatsSession iNetworkStatsSession2 = session;
            NetworkPolicy policy2 = policy;
            long start2 = start;
            long end3 = end;
            try {
                NetworkStatsHistory.Entry entry = history.getValues(start, end, now, null);
                long callEnd = System.currentTimeMillis();
                if (DEBUG) {
                    try {
                        long j = now;
                        Log.d("DataUsageController", String.format("history call from %s to %s now=%s took %sms: %s", new Object[]{new Date(start2), new Date(end3), new Date(now), Long.valueOf(callEnd - callStart), historyEntryToString(entry)}));
                    } catch (RemoteException e2) {
                        NetworkPolicy networkPolicy2 = policy2;
                        dataUsageController = this;
                    }
                } else {
                    long j2 = now;
                }
                if (entry == null) {
                    long j3 = end3;
                    dataUsageController = this;
                    try {
                        return dataUsageController.warn("no entry data");
                    } catch (RemoteException e3) {
                        return dataUsageController.warn("remote call failed");
                    }
                } else {
                    long end4 = end3;
                    dataUsageController = this;
                    try {
                        NetworkStatsHistory networkStatsHistory = history;
                        DataUsageInfo usage = new DataUsageInfo();
                        usage.startDate = start2;
                        usage.usageLevel = entry.rxBytes + entry.txBytes;
                        usage.period = dataUsageController.formatDateRange(start2, end4);
                        usage.cycleStart = start2;
                        usage.cycleEnd = end4;
                        if (policy2 != null) {
                            long j4 = start2;
                            NetworkPolicy policy3 = policy2;
                            try {
                                usage.limitLevel = policy3.limitBytes > 0 ? policy3.limitBytes : 0;
                                usage.warningLevel = policy3.warningBytes > 0 ? policy3.warningBytes : 0;
                            } catch (RemoteException e4) {
                                return dataUsageController.warn("remote call failed");
                            }
                        } else {
                            long j5 = start2;
                            NetworkPolicy networkPolicy3 = policy2;
                            usage.warningLevel = getDefaultWarningLevel();
                        }
                        if (dataUsageController.mNetworkController != null) {
                            usage.carrier = dataUsageController.mNetworkController.getMobileDataNetworkName();
                        }
                        return usage;
                    } catch (RemoteException e5) {
                        NetworkPolicy networkPolicy4 = policy2;
                        return dataUsageController.warn("remote call failed");
                    }
                }
            } catch (RemoteException e6) {
                NetworkPolicy networkPolicy5 = policy2;
                dataUsageController = this;
                return dataUsageController.warn("remote call failed");
            }
        } catch (RemoteException e7) {
            INetworkStatsSession iNetworkStatsSession3 = session;
            NetworkPolicy networkPolicy6 = policy;
            return dataUsageController.warn("remote call failed");
        }
    }

    private NetworkPolicy findNetworkPolicy(NetworkTemplate template) {
        if (this.mPolicyManager == null || template == null) {
            return null;
        }
        NetworkPolicy[] policies = this.mPolicyManager.getNetworkPolicies();
        if (policies == null) {
            return null;
        }
        for (NetworkPolicy policy : policies) {
            if (policy != null && template.equals(policy.template)) {
                return policy;
            }
        }
        return null;
    }

    private static String historyEntryToString(NetworkStatsHistory.Entry entry) {
        if (entry == null) {
            return null;
        }
        return "Entry[" + "bucketDuration=" + entry.bucketDuration + ",bucketStart=" + entry.bucketStart + ",activeTime=" + entry.activeTime + ",rxBytes=" + entry.rxBytes + ",rxPackets=" + entry.rxPackets + ",txBytes=" + entry.txBytes + ",txPackets=" + entry.txPackets + ",operations=" + entry.operations + ']';
    }

    public void setMobileDataEnabled(boolean enabled) {
        Log.d("DataUsageController", "setMobileDataEnabled: enabled=" + enabled);
        this.mTelephonyManager.setDataEnabled(enabled);
        if (this.mCallback != null) {
            this.mCallback.onMobileDataEnabled(enabled);
        }
    }

    public boolean isMobileDataSupported() {
        if (!this.mConnectivityManager.isNetworkSupported(0) || this.mTelephonyManager.getSimState() != 5) {
            return false;
        }
        return true;
    }

    public boolean isMobileDataEnabled() {
        return this.mTelephonyManager.getDataEnabled();
    }

    private static String getActiveSubscriberId(Context context) {
        return TelephonyManager.from(context).getSubscriberId(SubscriptionManager.getDefaultDataSubscriptionId());
    }

    private String formatDateRange(long start, long end) {
        synchronized (PERIOD_BUILDER) {
            try {
                PERIOD_BUILDER.setLength(0);
                String formatter = DateUtils.formatDateRange(this.mContext, PERIOD_FORMATTER, start, end, 65552, null).toString();
                return formatter;
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        }
    }
}
