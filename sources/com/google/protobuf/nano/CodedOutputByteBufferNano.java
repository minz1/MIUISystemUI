package com.google.protobuf.nano;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;

public final class CodedOutputByteBufferNano {
    private final ByteBuffer buffer;

    public static class OutOfSpaceException extends IOException {
        private static final long serialVersionUID = -6947486886997889499L;

        OutOfSpaceException(int position, int limit) {
            super("CodedOutputStream was writing to a flat byte array and ran out of space (pos " + position + " limit " + limit + ").");
        }
    }

    private CodedOutputByteBufferNano(byte[] buffer2, int offset, int length) {
        this(ByteBuffer.wrap(buffer2, offset, length));
    }

    private CodedOutputByteBufferNano(ByteBuffer buffer2) {
        this.buffer = buffer2;
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public static CodedOutputByteBufferNano newInstance(byte[] flatArray) {
        return newInstance(flatArray, 0, flatArray.length);
    }

    public static CodedOutputByteBufferNano newInstance(byte[] flatArray, int offset, int length) {
        return new CodedOutputByteBufferNano(flatArray, offset, length);
    }

    public void writeFloat(int fieldNumber, float value) throws IOException {
        writeTag(fieldNumber, 5);
        writeFloatNoTag(value);
    }

    public void writeUInt64(int fieldNumber, long value) throws IOException {
        writeTag(fieldNumber, 0);
        writeUInt64NoTag(value);
    }

    public void writeInt32(int fieldNumber, int value) throws IOException {
        writeTag(fieldNumber, 0);
        writeInt32NoTag(value);
    }

    public void writeBool(int fieldNumber, boolean value) throws IOException {
        writeTag(fieldNumber, 0);
        writeBoolNoTag(value);
    }

    public void writeString(int fieldNumber, String value) throws IOException {
        writeTag(fieldNumber, 2);
        writeStringNoTag(value);
    }

    public void writeMessage(int fieldNumber, MessageNano value) throws IOException {
        writeTag(fieldNumber, 2);
        writeMessageNoTag(value);
    }

    public void writeFloatNoTag(float value) throws IOException {
        writeRawLittleEndian32(Float.floatToIntBits(value));
    }

    public void writeUInt64NoTag(long value) throws IOException {
        writeRawVarint64(value);
    }

    public void writeInt32NoTag(int value) throws IOException {
        if (value >= 0) {
            writeRawVarint32(value);
        } else {
            writeRawVarint64((long) value);
        }
    }

    public void writeBoolNoTag(boolean value) throws IOException {
        writeRawByte((int) value);
    }

    public void writeStringNoTag(String value) throws IOException {
        try {
            int minLengthVarIntSize = computeRawVarint32Size(value.length());
            if (minLengthVarIntSize == computeRawVarint32Size(value.length() * 3)) {
                int oldPosition = this.buffer.position();
                if (this.buffer.remaining() >= minLengthVarIntSize) {
                    this.buffer.position(oldPosition + minLengthVarIntSize);
                    encode(value, this.buffer);
                    int newPosition = this.buffer.position();
                    this.buffer.position(oldPosition);
                    writeRawVarint32((newPosition - oldPosition) - minLengthVarIntSize);
                    this.buffer.position(newPosition);
                    return;
                }
                throw new OutOfSpaceException(oldPosition + minLengthVarIntSize, this.buffer.limit());
            }
            writeRawVarint32(encodedLength(value));
            encode(value, this.buffer);
        } catch (BufferOverflowException e) {
            OutOfSpaceException outOfSpaceException = new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
            outOfSpaceException.initCause(e);
            throw outOfSpaceException;
        }
    }

    private static int encodedLength(CharSequence sequence) {
        int utf16Length = sequence.length();
        int utf8Length = utf16Length;
        int i = 0;
        while (i < utf16Length && sequence.charAt(i) < 128) {
            i++;
        }
        while (true) {
            if (i >= utf16Length) {
                break;
            }
            char c = sequence.charAt(i);
            if (c >= 2048) {
                utf8Length += encodedLengthGeneral(sequence, i);
                break;
            }
            utf8Length += (127 - c) >>> 31;
            i++;
        }
        if (utf8Length >= utf16Length) {
            return utf8Length;
        }
        throw new IllegalArgumentException("UTF-8 length does not fit in int: " + (((long) utf8Length) + 4294967296L));
    }

    private static int encodedLengthGeneral(CharSequence sequence, int start) {
        int utf16Length = sequence.length();
        int utf8Length = 0;
        int i = start;
        while (i < utf16Length) {
            char c = sequence.charAt(i);
            if (c < 2048) {
                utf8Length += (127 - c) >>> 31;
            } else {
                utf8Length += 2;
                if (55296 <= c && c <= 57343) {
                    if (Character.codePointAt(sequence, i) >= 65536) {
                        i++;
                    } else {
                        throw new IllegalArgumentException("Unpaired surrogate at index " + i);
                    }
                }
            }
            i++;
        }
        return utf8Length;
    }

    private static void encode(CharSequence sequence, ByteBuffer byteBuffer) {
        if (byteBuffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        } else if (byteBuffer.hasArray()) {
            try {
                byteBuffer.position(encode(sequence, byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining()) - byteBuffer.arrayOffset());
            } catch (ArrayIndexOutOfBoundsException e) {
                BufferOverflowException boe = new BufferOverflowException();
                boe.initCause(e);
                throw boe;
            }
        } else {
            encodeDirect(sequence, byteBuffer);
        }
    }

    private static void encodeDirect(CharSequence sequence, ByteBuffer byteBuffer) {
        int utf16Length = sequence.length();
        int i = 0;
        while (i < utf16Length) {
            char c = sequence.charAt(i);
            if (c < 128) {
                byteBuffer.put((byte) c);
            } else if (c < 2048) {
                byteBuffer.put((byte) (960 | (c >>> 6)));
                byteBuffer.put((byte) (128 | ('?' & c)));
            } else if (c < 55296 || 57343 < c) {
                byteBuffer.put((byte) (480 | (c >>> 12)));
                byteBuffer.put((byte) (((c >>> 6) & 63) | 128));
                byteBuffer.put((byte) (128 | ('?' & c)));
            } else {
                if (i + 1 != sequence.length()) {
                    i++;
                    char charAt = sequence.charAt(i);
                    char low = charAt;
                    if (Character.isSurrogatePair(c, charAt)) {
                        int codePoint = Character.toCodePoint(c, low);
                        byteBuffer.put((byte) (240 | (codePoint >>> 18)));
                        byteBuffer.put((byte) (((codePoint >>> 12) & 63) | 128));
                        byteBuffer.put((byte) (((codePoint >>> 6) & 63) | 128));
                        byteBuffer.put((byte) (128 | (63 & codePoint)));
                    }
                }
                throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
            }
            i++;
        }
    }

    private static int encode(CharSequence sequence, byte[] bytes, int offset, int length) {
        int j;
        int utf16Length = sequence.length();
        int j2 = offset;
        int i = 0;
        int limit = offset + length;
        while (i < utf16Length && i + j2 < limit) {
            char charAt = sequence.charAt(i);
            char c = charAt;
            if (charAt >= 128) {
                break;
            }
            bytes[j2 + i] = (byte) c;
            i++;
        }
        if (i == utf16Length) {
            return j2 + utf16Length;
        }
        int j3 = j2 + i;
        while (i < utf16Length) {
            char c2 = sequence.charAt(i);
            if (c2 < 128 && j3 < limit) {
                j = j3 + 1;
                bytes[j3] = (byte) c2;
            } else if (c2 < 2048 && j3 <= limit - 2) {
                int j4 = j3 + 1;
                bytes[j3] = (byte) (960 | (c2 >>> 6));
                j3 = j4 + 1;
                bytes[j4] = (byte) (('?' & c2) | 128);
                i++;
            } else if ((c2 < 55296 || 57343 < c2) && j3 <= limit - 3) {
                int j5 = j3 + 1;
                bytes[j3] = (byte) (480 | (c2 >>> 12));
                int j6 = j5 + 1;
                bytes[j5] = (byte) (((c2 >>> 6) & 63) | 128);
                j = j6 + 1;
                bytes[j6] = (byte) (('?' & c2) | 128);
            } else if (j3 <= limit - 4) {
                if (i + 1 != sequence.length()) {
                    i++;
                    char charAt2 = sequence.charAt(i);
                    char low = charAt2;
                    if (Character.isSurrogatePair(c2, charAt2)) {
                        int codePoint = Character.toCodePoint(c2, low);
                        int j7 = j3 + 1;
                        bytes[j3] = (byte) (240 | (codePoint >>> 18));
                        int j8 = j7 + 1;
                        bytes[j7] = (byte) (((codePoint >>> 12) & 63) | 128);
                        int j9 = j8 + 1;
                        bytes[j8] = (byte) (((codePoint >>> 6) & 63) | 128);
                        j3 = j9 + 1;
                        bytes[j9] = (byte) ((63 & codePoint) | 128);
                        i++;
                    }
                }
                throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
            } else {
                throw new ArrayIndexOutOfBoundsException("Failed writing " + c2 + " at index " + j3);
            }
            j3 = j;
            i++;
        }
        return j3;
    }

    public void writeGroupNoTag(MessageNano value) throws IOException {
        value.writeTo(this);
    }

    public void writeMessageNoTag(MessageNano value) throws IOException {
        writeRawVarint32(value.getCachedSize());
        value.writeTo(this);
    }

    public static int computeFloatSize(int fieldNumber, float value) {
        return computeTagSize(fieldNumber) + computeFloatSizeNoTag(value);
    }

    public static int computeUInt64Size(int fieldNumber, long value) {
        return computeTagSize(fieldNumber) + computeUInt64SizeNoTag(value);
    }

    public static int computeInt32Size(int fieldNumber, int value) {
        return computeTagSize(fieldNumber) + computeInt32SizeNoTag(value);
    }

    public static int computeBoolSize(int fieldNumber, boolean value) {
        return computeTagSize(fieldNumber) + computeBoolSizeNoTag(value);
    }

    public static int computeStringSize(int fieldNumber, String value) {
        return computeTagSize(fieldNumber) + computeStringSizeNoTag(value);
    }

    public static int computeGroupSize(int fieldNumber, MessageNano value) {
        return (computeTagSize(fieldNumber) * 2) + computeGroupSizeNoTag(value);
    }

    public static int computeMessageSize(int fieldNumber, MessageNano value) {
        return computeTagSize(fieldNumber) + computeMessageSizeNoTag(value);
    }

    public static int computeFloatSizeNoTag(float value) {
        return 4;
    }

    public static int computeUInt64SizeNoTag(long value) {
        return computeRawVarint64Size(value);
    }

    public static int computeInt32SizeNoTag(int value) {
        if (value >= 0) {
            return computeRawVarint32Size(value);
        }
        return 10;
    }

    public static int computeBoolSizeNoTag(boolean value) {
        return 1;
    }

    public static int computeStringSizeNoTag(String value) {
        int length = encodedLength(value);
        return computeRawVarint32Size(length) + length;
    }

    public static int computeGroupSizeNoTag(MessageNano value) {
        return value.getSerializedSize();
    }

    public static int computeMessageSizeNoTag(MessageNano value) {
        int size = value.getSerializedSize();
        return computeRawVarint32Size(size) + size;
    }

    public int spaceLeft() {
        return this.buffer.remaining();
    }

    public void checkNoSpaceLeft() {
        if (spaceLeft() != 0) {
            throw new IllegalStateException("Did not write as much data as expected.");
        }
    }

    public void writeRawByte(byte value) throws IOException {
        if (this.buffer.hasRemaining()) {
            this.buffer.put(value);
            return;
        }
        throw new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
    }

    public void writeRawByte(int value) throws IOException {
        writeRawByte((byte) value);
    }

    public void writeRawBytes(byte[] value) throws IOException {
        writeRawBytes(value, 0, value.length);
    }

    public void writeRawBytes(byte[] value, int offset, int length) throws IOException {
        if (this.buffer.remaining() >= length) {
            this.buffer.put(value, offset, length);
            return;
        }
        throw new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
    }

    public void writeTag(int fieldNumber, int wireType) throws IOException {
        writeRawVarint32(WireFormatNano.makeTag(fieldNumber, wireType));
    }

    public static int computeTagSize(int fieldNumber) {
        return computeRawVarint32Size(WireFormatNano.makeTag(fieldNumber, 0));
    }

    public void writeRawVarint32(int value) throws IOException {
        while ((value & -128) != 0) {
            writeRawByte((value & 127) | 128);
            value >>>= 7;
        }
        writeRawByte(value);
    }

    public static int computeRawVarint32Size(int value) {
        if ((value & -128) == 0) {
            return 1;
        }
        if ((value & -16384) == 0) {
            return 2;
        }
        if ((-2097152 & value) == 0) {
            return 3;
        }
        if ((-268435456 & value) == 0) {
            return 4;
        }
        return 5;
    }

    public void writeRawVarint64(long value) throws IOException {
        while ((-128 & value) != 0) {
            writeRawByte((((int) value) & 127) | 128);
            value >>>= 7;
        }
        writeRawByte((int) value);
    }

    public static int computeRawVarint64Size(long value) {
        if ((-128 & value) == 0) {
            return 1;
        }
        if ((-16384 & value) == 0) {
            return 2;
        }
        if ((-2097152 & value) == 0) {
            return 3;
        }
        if ((-268435456 & value) == 0) {
            return 4;
        }
        if ((-34359738368L & value) == 0) {
            return 5;
        }
        if ((-4398046511104L & value) == 0) {
            return 6;
        }
        if ((-562949953421312L & value) == 0) {
            return 7;
        }
        if ((-72057594037927936L & value) == 0) {
            return 8;
        }
        if ((Long.MIN_VALUE & value) == 0) {
            return 9;
        }
        return 10;
    }

    public void writeRawLittleEndian32(int value) throws IOException {
        if (this.buffer.remaining() >= 4) {
            this.buffer.putInt(value);
            return;
        }
        throw new OutOfSpaceException(this.buffer.position(), this.buffer.limit());
    }
}
