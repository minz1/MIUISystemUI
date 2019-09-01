package com.android.systemui.statusbar.phone.nano;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InternalNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface TouchAnalyticsProto {

    public static final class Session extends MessageNano {
        private int bitField0_;
        private String build_;
        private long durationMillis_;
        public PhoneEvent[] phoneEvents;
        private int result_;
        public SensorEvent[] sensorEvents;
        private long startTimestampMillis_;
        private int touchAreaHeight_;
        private int touchAreaWidth_;
        public TouchEvent[] touchEvents;
        private int type_;

        public static final class PhoneEvent extends MessageNano {
            private static volatile PhoneEvent[] _emptyArray;
            private int bitField0_;
            private long timeOffsetNanos_;
            private int type_;

            public static PhoneEvent[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new PhoneEvent[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public PhoneEvent setType(int value) {
                this.type_ = value;
                this.bitField0_ |= 1;
                return this;
            }

            public PhoneEvent setTimeOffsetNanos(long value) {
                this.timeOffsetNanos_ = value;
                this.bitField0_ |= 2;
                return this;
            }

            public PhoneEvent() {
                clear();
            }

            public PhoneEvent clear() {
                this.bitField0_ = 0;
                this.type_ = 0;
                this.timeOffsetNanos_ = 0;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if ((this.bitField0_ & 1) != 0) {
                    output.writeInt32(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    output.writeUInt64(2, this.timeOffsetNanos_);
                }
                super.writeTo(output);
            }

            /* access modifiers changed from: protected */
            public int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if ((this.bitField0_ & 1) != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    return size + CodedOutputByteBufferNano.computeUInt64Size(2, this.timeOffsetNanos_);
                }
                return size;
            }
        }

        public static final class SensorEvent extends MessageNano {
            private static volatile SensorEvent[] _emptyArray;
            private int bitField0_;
            private long timeOffsetNanos_;
            private long timestamp_;
            private int type_;
            public float[] values;

            public static SensorEvent[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new SensorEvent[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public SensorEvent setType(int value) {
                this.type_ = value;
                this.bitField0_ |= 1;
                return this;
            }

            public SensorEvent setTimeOffsetNanos(long value) {
                this.timeOffsetNanos_ = value;
                this.bitField0_ |= 2;
                return this;
            }

            public SensorEvent setTimestamp(long value) {
                this.timestamp_ = value;
                this.bitField0_ |= 4;
                return this;
            }

            public SensorEvent() {
                clear();
            }

            public SensorEvent clear() {
                this.bitField0_ = 0;
                this.type_ = 1;
                this.timeOffsetNanos_ = 0;
                this.values = WireFormatNano.EMPTY_FLOAT_ARRAY;
                this.timestamp_ = 0;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if ((this.bitField0_ & 1) != 0) {
                    output.writeInt32(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    output.writeUInt64(2, this.timeOffsetNanos_);
                }
                if (this.values != null && this.values.length > 0) {
                    for (float writeFloat : this.values) {
                        output.writeFloat(3, writeFloat);
                    }
                }
                if ((this.bitField0_ & 4) != 0) {
                    output.writeUInt64(4, this.timestamp_);
                }
                super.writeTo(output);
            }

            /* access modifiers changed from: protected */
            public int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if ((this.bitField0_ & 1) != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.type_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    size += CodedOutputByteBufferNano.computeUInt64Size(2, this.timeOffsetNanos_);
                }
                if (this.values != null && this.values.length > 0) {
                    size = size + (this.values.length * 4) + (1 * this.values.length);
                }
                if ((this.bitField0_ & 4) != 0) {
                    return size + CodedOutputByteBufferNano.computeUInt64Size(4, this.timestamp_);
                }
                return size;
            }
        }

        public static final class TouchEvent extends MessageNano {
            private static volatile TouchEvent[] _emptyArray;
            private int actionIndex_;
            private int action_;
            private int bitField0_;
            public Pointer[] pointers;
            public BoundingBox removedBoundingBox;
            private boolean removedRedacted_;
            private long timeOffsetNanos_;

            public static final class BoundingBox extends MessageNano {
                private int bitField0_;
                private float height_;
                private float width_;

                public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                    if ((this.bitField0_ & 1) != 0) {
                        output.writeFloat(1, this.width_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        output.writeFloat(2, this.height_);
                    }
                    super.writeTo(output);
                }

                /* access modifiers changed from: protected */
                public int computeSerializedSize() {
                    int size = super.computeSerializedSize();
                    if ((this.bitField0_ & 1) != 0) {
                        size += CodedOutputByteBufferNano.computeFloatSize(1, this.width_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        return size + CodedOutputByteBufferNano.computeFloatSize(2, this.height_);
                    }
                    return size;
                }
            }

            public static final class Pointer extends MessageNano {
                private static volatile Pointer[] _emptyArray;
                private int bitField0_;
                private int id_;
                private float pressure_;
                public BoundingBox removedBoundingBox;
                private float removedLength_;
                private float size_;
                private float x_;
                private float y_;

                public static Pointer[] emptyArray() {
                    if (_emptyArray == null) {
                        synchronized (InternalNano.LAZY_INIT_LOCK) {
                            if (_emptyArray == null) {
                                _emptyArray = new Pointer[0];
                            }
                        }
                    }
                    return _emptyArray;
                }

                public Pointer setX(float value) {
                    this.x_ = value;
                    this.bitField0_ |= 1;
                    return this;
                }

                public Pointer setY(float value) {
                    this.y_ = value;
                    this.bitField0_ |= 2;
                    return this;
                }

                public Pointer setSize(float value) {
                    this.size_ = value;
                    this.bitField0_ |= 4;
                    return this;
                }

                public Pointer setPressure(float value) {
                    this.pressure_ = value;
                    this.bitField0_ |= 8;
                    return this;
                }

                public Pointer setId(int value) {
                    this.id_ = value;
                    this.bitField0_ |= 16;
                    return this;
                }

                public Pointer() {
                    clear();
                }

                public Pointer clear() {
                    this.bitField0_ = 0;
                    this.x_ = 0.0f;
                    this.y_ = 0.0f;
                    this.size_ = 0.0f;
                    this.pressure_ = 0.0f;
                    this.id_ = 0;
                    this.removedLength_ = 0.0f;
                    this.removedBoundingBox = null;
                    this.cachedSize = -1;
                    return this;
                }

                public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                    if ((this.bitField0_ & 1) != 0) {
                        output.writeFloat(1, this.x_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        output.writeFloat(2, this.y_);
                    }
                    if ((this.bitField0_ & 4) != 0) {
                        output.writeFloat(3, this.size_);
                    }
                    if ((this.bitField0_ & 8) != 0) {
                        output.writeFloat(4, this.pressure_);
                    }
                    if ((this.bitField0_ & 16) != 0) {
                        output.writeInt32(5, this.id_);
                    }
                    if ((this.bitField0_ & 32) != 0) {
                        output.writeFloat(6, this.removedLength_);
                    }
                    if (this.removedBoundingBox != null) {
                        output.writeMessage(7, this.removedBoundingBox);
                    }
                    super.writeTo(output);
                }

                /* access modifiers changed from: protected */
                public int computeSerializedSize() {
                    int size = super.computeSerializedSize();
                    if ((this.bitField0_ & 1) != 0) {
                        size += CodedOutputByteBufferNano.computeFloatSize(1, this.x_);
                    }
                    if ((this.bitField0_ & 2) != 0) {
                        size += CodedOutputByteBufferNano.computeFloatSize(2, this.y_);
                    }
                    if ((this.bitField0_ & 4) != 0) {
                        size += CodedOutputByteBufferNano.computeFloatSize(3, this.size_);
                    }
                    if ((this.bitField0_ & 8) != 0) {
                        size += CodedOutputByteBufferNano.computeFloatSize(4, this.pressure_);
                    }
                    if ((this.bitField0_ & 16) != 0) {
                        size += CodedOutputByteBufferNano.computeInt32Size(5, this.id_);
                    }
                    if ((this.bitField0_ & 32) != 0) {
                        size += CodedOutputByteBufferNano.computeFloatSize(6, this.removedLength_);
                    }
                    if (this.removedBoundingBox != null) {
                        return size + CodedOutputByteBufferNano.computeMessageSize(7, this.removedBoundingBox);
                    }
                    return size;
                }
            }

            public static TouchEvent[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new TouchEvent[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public TouchEvent setTimeOffsetNanos(long value) {
                this.timeOffsetNanos_ = value;
                this.bitField0_ |= 1;
                return this;
            }

            public TouchEvent setAction(int value) {
                this.action_ = value;
                this.bitField0_ |= 2;
                return this;
            }

            public TouchEvent setActionIndex(int value) {
                this.actionIndex_ = value;
                this.bitField0_ |= 4;
                return this;
            }

            public TouchEvent() {
                clear();
            }

            public TouchEvent clear() {
                this.bitField0_ = 0;
                this.timeOffsetNanos_ = 0;
                this.action_ = 0;
                this.actionIndex_ = 0;
                this.pointers = Pointer.emptyArray();
                this.removedRedacted_ = false;
                this.removedBoundingBox = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if ((this.bitField0_ & 1) != 0) {
                    output.writeUInt64(1, this.timeOffsetNanos_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    output.writeInt32(2, this.action_);
                }
                if ((this.bitField0_ & 4) != 0) {
                    output.writeInt32(3, this.actionIndex_);
                }
                if (this.pointers != null && this.pointers.length > 0) {
                    for (Pointer element : this.pointers) {
                        if (element != null) {
                            output.writeMessage(4, element);
                        }
                    }
                }
                if ((this.bitField0_ & 8) != 0) {
                    output.writeBool(5, this.removedRedacted_);
                }
                if (this.removedBoundingBox != null) {
                    output.writeMessage(6, this.removedBoundingBox);
                }
                super.writeTo(output);
            }

            /* access modifiers changed from: protected */
            public int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if ((this.bitField0_ & 1) != 0) {
                    size += CodedOutputByteBufferNano.computeUInt64Size(1, this.timeOffsetNanos_);
                }
                if ((this.bitField0_ & 2) != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(2, this.action_);
                }
                if ((this.bitField0_ & 4) != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(3, this.actionIndex_);
                }
                if (this.pointers != null && this.pointers.length > 0) {
                    for (Pointer element : this.pointers) {
                        if (element != null) {
                            size += CodedOutputByteBufferNano.computeMessageSize(4, element);
                        }
                    }
                }
                if ((this.bitField0_ & 8) != 0) {
                    size += CodedOutputByteBufferNano.computeBoolSize(5, this.removedRedacted_);
                }
                if (this.removedBoundingBox != null) {
                    return size + CodedOutputByteBufferNano.computeMessageSize(6, this.removedBoundingBox);
                }
                return size;
            }
        }

        public Session setStartTimestampMillis(long value) {
            this.startTimestampMillis_ = value;
            this.bitField0_ |= 1;
            return this;
        }

        public Session setDurationMillis(long value) {
            this.durationMillis_ = value;
            this.bitField0_ |= 2;
            return this;
        }

        public Session setBuild(String value) {
            if (value != null) {
                this.build_ = value;
                this.bitField0_ |= 4;
                return this;
            }
            throw new NullPointerException();
        }

        public Session setResult(int value) {
            this.result_ = value;
            this.bitField0_ |= 8;
            return this;
        }

        public Session setTouchAreaWidth(int value) {
            this.touchAreaWidth_ = value;
            this.bitField0_ |= 16;
            return this;
        }

        public Session setTouchAreaHeight(int value) {
            this.touchAreaHeight_ = value;
            this.bitField0_ |= 32;
            return this;
        }

        public Session setType(int value) {
            this.type_ = value;
            this.bitField0_ |= 64;
            return this;
        }

        public Session() {
            clear();
        }

        public Session clear() {
            this.bitField0_ = 0;
            this.startTimestampMillis_ = 0;
            this.durationMillis_ = 0;
            this.build_ = "";
            this.result_ = 0;
            this.touchEvents = TouchEvent.emptyArray();
            this.sensorEvents = SensorEvent.emptyArray();
            this.touchAreaWidth_ = 0;
            this.touchAreaHeight_ = 0;
            this.type_ = 0;
            this.phoneEvents = PhoneEvent.emptyArray();
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if ((this.bitField0_ & 1) != 0) {
                output.writeUInt64(1, this.startTimestampMillis_);
            }
            if ((this.bitField0_ & 2) != 0) {
                output.writeUInt64(2, this.durationMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                output.writeString(3, this.build_);
            }
            if ((this.bitField0_ & 8) != 0) {
                output.writeInt32(4, this.result_);
            }
            int i = 0;
            if (this.touchEvents != null && this.touchEvents.length > 0) {
                for (TouchEvent element : this.touchEvents) {
                    if (element != null) {
                        output.writeMessage(5, element);
                    }
                }
            }
            if (this.sensorEvents != null && this.sensorEvents.length > 0) {
                for (SensorEvent element2 : this.sensorEvents) {
                    if (element2 != null) {
                        output.writeMessage(6, element2);
                    }
                }
            }
            if ((this.bitField0_ & 16) != 0) {
                output.writeInt32(9, this.touchAreaWidth_);
            }
            if ((this.bitField0_ & 32) != 0) {
                output.writeInt32(10, this.touchAreaHeight_);
            }
            if ((this.bitField0_ & 64) != 0) {
                output.writeInt32(11, this.type_);
            }
            if (this.phoneEvents != null && this.phoneEvents.length > 0) {
                while (true) {
                    int i2 = i;
                    if (i2 >= this.phoneEvents.length) {
                        break;
                    }
                    PhoneEvent element3 = this.phoneEvents[i2];
                    if (element3 != null) {
                        output.writeMessage(12, element3);
                    }
                    i = i2 + 1;
                }
            }
            super.writeTo(output);
        }

        /* access modifiers changed from: protected */
        public int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if ((this.bitField0_ & 1) != 0) {
                size += CodedOutputByteBufferNano.computeUInt64Size(1, this.startTimestampMillis_);
            }
            if ((this.bitField0_ & 2) != 0) {
                size += CodedOutputByteBufferNano.computeUInt64Size(2, this.durationMillis_);
            }
            if ((this.bitField0_ & 4) != 0) {
                size += CodedOutputByteBufferNano.computeStringSize(3, this.build_);
            }
            if ((this.bitField0_ & 8) != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.result_);
            }
            int i = 0;
            if (this.touchEvents != null && this.touchEvents.length > 0) {
                int size2 = size;
                for (TouchEvent element : this.touchEvents) {
                    if (element != null) {
                        size2 += CodedOutputByteBufferNano.computeMessageSize(5, element);
                    }
                }
                size = size2;
            }
            if (this.sensorEvents != null && this.sensorEvents.length > 0) {
                int size3 = size;
                for (SensorEvent element2 : this.sensorEvents) {
                    if (element2 != null) {
                        size3 += CodedOutputByteBufferNano.computeMessageSize(6, element2);
                    }
                }
                size = size3;
            }
            if ((this.bitField0_ & 16) != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(9, this.touchAreaWidth_);
            }
            if ((this.bitField0_ & 32) != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(10, this.touchAreaHeight_);
            }
            if ((this.bitField0_ & 64) != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(11, this.type_);
            }
            if (this.phoneEvents != null && this.phoneEvents.length > 0) {
                while (true) {
                    int i2 = i;
                    if (i2 >= this.phoneEvents.length) {
                        break;
                    }
                    PhoneEvent element3 = this.phoneEvents[i2];
                    if (element3 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(12, element3);
                    }
                    i = i2 + 1;
                }
            }
            return size;
        }
    }
}
