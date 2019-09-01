package com.android.systemui.statusbar;

import android.util.SparseArray;
import com.android.systemui.R;

public class Icons {
    private static SparseArray<Integer> sMapping;
    private static SparseArray<Integer> sSignalHalfMapping;

    public static int get(Integer originalId, boolean enable) {
        if (sMapping == null) {
            sMapping = new SparseArray<>();
            sMapping.put(R.drawable.stat_sys_alarm, Integer.valueOf(R.drawable.stat_sys_alarm_darkmode));
            sMapping.put(R.drawable.stat_sys_data_bluetooth, Integer.valueOf(R.drawable.stat_sys_data_bluetooth_darkmode));
            sMapping.put(R.drawable.stat_sys_data_bluetooth_connected, Integer.valueOf(R.drawable.stat_sys_data_bluetooth_connected_darkmode));
            sMapping.put(R.drawable.stat_sys_data_bluetooth_in, Integer.valueOf(R.drawable.stat_sys_data_bluetooth_in_darkmode));
            sMapping.put(R.drawable.stat_sys_data_bluetooth_inout, Integer.valueOf(R.drawable.stat_sys_data_bluetooth_inout_darkmode));
            sMapping.put(R.drawable.stat_sys_data_bluetooth_out, Integer.valueOf(R.drawable.stat_sys_data_bluetooth_out_darkmode));
            sMapping.put(R.drawable.stat_sys_data_connected_roam, Integer.valueOf(R.drawable.stat_sys_data_connected_roam_darkmode));
            sMapping.put(R.drawable.stat_sys_speech_hd, Integer.valueOf(R.drawable.stat_sys_speech_hd_darkmode));
            sMapping.put(R.drawable.stat_sys_vowifi, Integer.valueOf(R.drawable.stat_sys_vowifi_darkmode));
            sMapping.put(R.drawable.stat_sys_gps_acquiring_anim, Integer.valueOf(R.drawable.stat_sys_gps_acquiring_anim_darkmode));
            sMapping.put(R.drawable.stat_sys_dual_gps_acquiring_anim, Integer.valueOf(R.drawable.stat_sys_dual_gps_acquiring_anim_darkmode));
            sMapping.put(R.drawable.stat_sys_gps_on, Integer.valueOf(R.drawable.stat_sys_gps_on_darkmode));
            sMapping.put(R.drawable.stat_sys_gps_acquiring, Integer.valueOf(R.drawable.stat_sys_gps_acquiring_darkmode));
            sMapping.put(R.drawable.stat_sys_dual_gps_on, Integer.valueOf(R.drawable.stat_sys_dual_gps_on_darkmode));
            sMapping.put(R.drawable.stat_sys_dual_gps_acquiring, Integer.valueOf(R.drawable.stat_sys_dual_gps_acquiring_darkmode));
            sMapping.put(R.drawable.stat_sys_headset, Integer.valueOf(R.drawable.stat_sys_headset_darkmode));
            sMapping.put(R.drawable.stat_sys_headset_without_mic, Integer.valueOf(R.drawable.stat_sys_headset_without_mic_darkmode));
            sMapping.put(R.drawable.stat_sys_no_sim, Integer.valueOf(R.drawable.stat_sys_no_sim_darkmode));
            sMapping.put(R.drawable.stat_sys_ringer_silent, Integer.valueOf(R.drawable.stat_sys_ringer_silent_darkmode));
            sMapping.put(R.drawable.stat_sys_ringer_vibrate, Integer.valueOf(R.drawable.stat_sys_ringer_vibrate_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_null, Integer.valueOf(R.drawable.stat_sys_signal_null_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_0, Integer.valueOf(R.drawable.stat_sys_signal_0_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_1, Integer.valueOf(R.drawable.stat_sys_signal_1_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_2, Integer.valueOf(R.drawable.stat_sys_signal_2_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_3, Integer.valueOf(R.drawable.stat_sys_signal_3_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_4, Integer.valueOf(R.drawable.stat_sys_signal_4_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_5, Integer.valueOf(R.drawable.stat_sys_signal_5_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_flightmode, Integer.valueOf(R.drawable.stat_sys_signal_flightmode_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_in, Integer.valueOf(R.drawable.stat_sys_signal_in_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_inout, Integer.valueOf(R.drawable.stat_sys_signal_inout_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_out, Integer.valueOf(R.drawable.stat_sys_signal_out_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_data, Integer.valueOf(R.drawable.stat_sys_signal_data_darkmode));
            sMapping.put(R.drawable.stat_sys_sync_active, Integer.valueOf(R.drawable.stat_sys_sync_active_darkmode));
            sMapping.put(R.drawable.stat_sys_sync_error, Integer.valueOf(R.drawable.stat_sys_sync_error_darkmode));
            sMapping.put(R.drawable.stat_sys_bluetooth_handsfree_battery, Integer.valueOf(R.drawable.stat_sys_bluetooth_handsfree_battery_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_signal_null, Integer.valueOf(R.drawable.stat_sys_wifi_signal_null_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_signal_0, Integer.valueOf(R.drawable.stat_sys_wifi_signal_0_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_signal_1, Integer.valueOf(R.drawable.stat_sys_wifi_signal_1_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_signal_2, Integer.valueOf(R.drawable.stat_sys_wifi_signal_2_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_signal_3, Integer.valueOf(R.drawable.stat_sys_wifi_signal_3_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_signal_4, Integer.valueOf(R.drawable.stat_sys_wifi_signal_4_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_ap, Integer.valueOf(R.drawable.stat_sys_wifi_ap_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_inout, Integer.valueOf(R.drawable.stat_sys_wifi_inout_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_in, Integer.valueOf(R.drawable.stat_sys_wifi_in_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_out, Integer.valueOf(R.drawable.stat_sys_wifi_out_darkmode));
            sMapping.put(R.drawable.stat_sys_wifi_ap_on, Integer.valueOf(R.drawable.stat_sys_wifi_ap_on_darkmode));
            sMapping.put(R.drawable.stat_sys_warning, Integer.valueOf(R.drawable.stat_sys_warning_darkmode));
            sMapping.put(R.drawable.stat_sys_vpn, Integer.valueOf(R.drawable.stat_sys_vpn_darkmode));
            sMapping.put(R.drawable.stat_sys_data_connected_roam_small, Integer.valueOf(R.drawable.stat_sys_data_connected_roam_small_darkmode));
            sMapping.put(R.drawable.stat_sys_speakerphone, Integer.valueOf(R.drawable.stat_sys_speakerphone_darkmode));
            sMapping.put(R.drawable.stat_sys_call_record, Integer.valueOf(R.drawable.stat_sys_call_record_darkmode));
            sMapping.put(R.drawable.stat_sys_roaming_cdma_0, Integer.valueOf(R.drawable.stat_sys_roaming_cdma_0_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_0_half, Integer.valueOf(R.drawable.stat_sys_signal_0_half_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_1_half, Integer.valueOf(R.drawable.stat_sys_signal_1_half_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_2_half, Integer.valueOf(R.drawable.stat_sys_signal_2_half_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_3_half, Integer.valueOf(R.drawable.stat_sys_signal_3_half_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_4_half, Integer.valueOf(R.drawable.stat_sys_signal_4_half_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_5_half, Integer.valueOf(R.drawable.stat_sys_signal_5_half_darkmode));
            sMapping.put(R.drawable.stat_notify_more, Integer.valueOf(R.drawable.stat_notify_more_darkmode));
            sMapping.put(R.drawable.stat_sys_speakerphone, Integer.valueOf(R.drawable.stat_sys_speakerphone_darkmode));
            sMapping.put(R.drawable.stat_notify_call_mute, Integer.valueOf(R.drawable.stat_notify_call_mute_darkmode));
            sMapping.put(R.drawable.stat_sys_quiet_mode, Integer.valueOf(R.drawable.stat_sys_quiet_mode_darkmode));
            sMapping.put(R.drawable.stat_sys_usb_share, Integer.valueOf(R.drawable.stat_sys_usb_share_darkmode));
            sMapping.put(R.drawable.stat_sys_missed_call, Integer.valueOf(R.drawable.stat_sys_missed_call_darkmode));
            sMapping.put(R.drawable.stat_sys_battery_charging, Integer.valueOf(R.drawable.stat_sys_battery_charging_darkmode));
            sMapping.put(R.drawable.stat_sys_quick_charging, Integer.valueOf(R.drawable.stat_sys_quick_charging_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_4g_lte, Integer.valueOf(R.drawable.stat_sys_signal_4g_lte_darkmode));
            sMapping.put(R.drawable.stat_sys_sos, Integer.valueOf(R.drawable.stat_sys_sos_darkmode));
            sMapping.put(R.drawable.stat_sys_managed_profile_status, Integer.valueOf(R.drawable.stat_sys_managed_profile_status_darkmode));
            sMapping.put(R.drawable.stat_sys_managed_profile_status_off, Integer.valueOf(R.drawable.stat_sys_managed_profile_status_off_darkmode));
            sMapping.put(R.drawable.stat_sys_managed_profile_xspace_user, Integer.valueOf(R.drawable.stat_sys_managed_profile_xspace_user_darkmode));
            sMapping.put(R.drawable.stat_sys_managed_profile_not_owner_user, Integer.valueOf(R.drawable.stat_sys_managed_profile_not_owner_user_darkmode));
            sMapping.put(R.drawable.sim1, Integer.valueOf(R.drawable.sim1_darkmode));
            sMapping.put(R.drawable.sim2, Integer.valueOf(R.drawable.sim2_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_upgrade, Integer.valueOf(R.drawable.stat_sys_signal_upgrade_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_volte, Integer.valueOf(R.drawable.stat_sys_signal_volte_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_volte_1, Integer.valueOf(R.drawable.stat_sys_signal_volte_1_darkmode));
            sMapping.put(R.drawable.stat_sys_volte_no_service, Integer.valueOf(R.drawable.stat_sys_volte_no_service_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_hd_notch, Integer.valueOf(R.drawable.stat_sys_signal_hd_notch_darkmode));
            sMapping.put(R.drawable.stat_sys_signal_null_half, Integer.valueOf(R.drawable.stat_sys_signal_null_half_darkmode));
            sMapping.put(R.drawable.stat_sys_battery_charging, Integer.valueOf(R.drawable.stat_sys_battery_charging_darkmode));
            sMapping.put(R.drawable.ble_unlock_statusbar_icon_unverified, Integer.valueOf(R.drawable.ble_unlock_statusbar_icon_unverified_dark));
            sMapping.put(R.drawable.ble_unlock_statusbar_icon_verified_near, Integer.valueOf(R.drawable.ble_unlock_statusbar_icon_verified_near_dark));
            sMapping.put(R.drawable.ble_unlock_statusbar_icon_verified_far, Integer.valueOf(R.drawable.ble_unlock_statusbar_icon_verified_far_dark));
        }
        Integer resultId = enable ? sMapping.get(originalId.intValue()) : originalId;
        if (resultId == null) {
            return 0;
        }
        return resultId.intValue();
    }

    public static int getSignalHalfId(Integer originalId) {
        if (sSignalHalfMapping == null) {
            sSignalHalfMapping = new SparseArray<>();
            sSignalHalfMapping.put(R.drawable.stat_sys_signal_0, Integer.valueOf(R.drawable.stat_sys_signal_0_half));
            sSignalHalfMapping.put(R.drawable.stat_sys_signal_1, Integer.valueOf(R.drawable.stat_sys_signal_1_half));
            sSignalHalfMapping.put(R.drawable.stat_sys_signal_2, Integer.valueOf(R.drawable.stat_sys_signal_2_half));
            sSignalHalfMapping.put(R.drawable.stat_sys_signal_3, Integer.valueOf(R.drawable.stat_sys_signal_3_half));
            sSignalHalfMapping.put(R.drawable.stat_sys_signal_4, Integer.valueOf(R.drawable.stat_sys_signal_4_half));
            sSignalHalfMapping.put(R.drawable.stat_sys_signal_5, Integer.valueOf(R.drawable.stat_sys_signal_5_half));
            sSignalHalfMapping.put(R.drawable.stat_sys_signal_null, Integer.valueOf(R.drawable.stat_sys_signal_null_half));
        }
        Integer resultId = sSignalHalfMapping.get(originalId.intValue());
        if (resultId == null) {
            return 0;
        }
        return resultId.intValue();
    }
}
