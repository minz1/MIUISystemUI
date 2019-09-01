package android.support.v7.preference;

public abstract class PreferenceDataStore {
    public void putString(String key, String value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    public void putInt(String key, int value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    public void putBoolean(String key, boolean value) {
        throw new UnsupportedOperationException("Not implemented on this data store");
    }

    public String getString(String key, String defValue) {
        return defValue;
    }

    public int getInt(String key, int defValue) {
        return defValue;
    }

    public boolean getBoolean(String key, boolean defValue) {
        return defValue;
    }
}
