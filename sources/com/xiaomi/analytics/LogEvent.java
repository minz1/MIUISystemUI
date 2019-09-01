package com.xiaomi.analytics;

public class LogEvent {

    public enum LogType {
        TYPE_EVENT(0),
        TYPE_AD(1);
        
        private int mValue;

        private LogType(int value) {
            this.mValue = 0;
            this.mValue = value;
        }

        public int value() {
            return this.mValue;
        }
    }
}
