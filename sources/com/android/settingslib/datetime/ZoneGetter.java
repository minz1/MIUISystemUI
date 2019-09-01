package com.android.settingslib.datetime;

import java.util.List;
import libcore.util.TimeZoneFinder;

public class ZoneGetter {

    public static final class ZoneGetterData {
        public List<String> lookupTimeZoneIdsByCountry(String country) {
            return TimeZoneFinder.getInstance().lookupTimeZoneIdsByCountry(country);
        }
    }
}
