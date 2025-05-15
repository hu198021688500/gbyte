package com.electric.gbyte.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;

import java.nio.ByteOrder;

import static io.netty.util.internal.ObjectUtil.*;

/**
 * 查找完整的协议报文
 *
 * @author bingo
 * <p>
 * {@link io.netty.handler.codec.LengthFieldBasedFrameDecoder}
 */
public abstract class BaseDecoder extends ByteToMessageDecoder {

    private final ByteOrder byteOrder;

    private final int maxFrameLength;

    private final int lengthFieldOffset;

    private final int lengthFieldLength;

    private final int lengthFieldEndOffset;

    private final int lengthAdjustment;

    private final int initialBytesToStrip;

    private final boolean failFast;

    private boolean discardingTooLongFrame;

    private long tooLongFrameLength;

    private long bytesToDiscard;

    protected final int headerValue;

    protected final int headerLength;

    public BaseDecoder(ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast, int headerValue, int headerLength) {

        this.byteOrder = checkNotNull(byteOrder, "byteOrder");

        checkPositive(maxFrameLength, "maxFrameLength");

        checkPositiveOrZero(lengthFieldOffset, "lengthFieldOffset");

        checkPositiveOrZero(initialBytesToStrip, "initialBytesToStrip");

        checkInRange(headerLength, 0, 2, "headerLength");

        if (lengthFieldOffset > maxFrameLength - lengthFieldLength) {
            throw new IllegalArgumentException("maxFrameLength (" + maxFrameLength + ") must be equal to or greater than lengthFieldOffset (" + lengthFieldOffset + ") + lengthFieldLength (" + lengthFieldLength + ").");
        }

        this.maxFrameLength = maxFrameLength;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        this.lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.initialBytesToStrip = initialBytesToStrip;
        this.failFast = failFast;

        this.headerValue = headerValue;
        this.headerLength = headerLength;
    }

    private boolean findHeaderPos(ByteBuf in) {
        if (in.readableBytes() < this.headerLength) {
            return false;
        }

        while (true) {
            in.markReaderIndex();

            if (this.headerLength == 1) {
                if (in.readUnsignedByte() == this.headerValue) {
                    break;
                }
            } else if (this.headerLength == 2) {
                if (in.readUnsignedShort() == this.headerValue) {
                    break;
                }
            }

            in.resetReaderIndex();
            in.skipBytes(1);

            if (in.readableBytes() < this.headerLength) {
                return false;
            }
        }

        in.resetReaderIndex();

        return true;
    }

    protected ByteBuf findCompletePacket(ByteBuf in) {
        if (this.headerLength > 0) {
            if (!this.findHeaderPos(in)) {
                return null;
            }
        }

        return this.findCompletePacketData(in);
    }

    private void discardingTooLongFrame(ByteBuf in) {
        long bytesToDiscard = this.bytesToDiscard;
        int localBytesToDiscard = (int) Math.min(bytesToDiscard, in.readableBytes());
        in.skipBytes(localBytesToDiscard);
        bytesToDiscard -= localBytesToDiscard;
        this.bytesToDiscard = bytesToDiscard;

        failIfNecessary(false);
    }

    private static void failOnNegativeLengthField(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException("negative pre-adjustment length field: " + frameLength);
    }

    private static void failOnFrameLengthLessThanLengthFieldEndOffset(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less than lengthFieldEndOffset: " + lengthFieldEndOffset);
    }

    private void exceededFrameLength(ByteBuf in, long frameLength) {
        long discard = frameLength - in.readableBytes();
        tooLongFrameLength = frameLength;

        if (discard < 0) {
            // buffer contains more bytes than the frameLength, so we can discard all now
            in.skipBytes((int) frameLength);
        } else {
            // Enter the discard mode and discard everything received so far.
            discardingTooLongFrame = true;
            bytesToDiscard = discard;
            in.skipBytes(in.readableBytes());
        }
        failIfNecessary(true);
    }

    private static void failOnFrameLengthLessThanInitialBytesToStrip(ByteBuf in, long frameLength, int initialBytesToStrip) {
        in.skipBytes((int) frameLength);
        throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less than initialBytesToStrip: " + initialBytesToStrip);
    }

    protected ByteBuf findCompletePacketData(ByteBuf in) {
        if (discardingTooLongFrame) {
            discardingTooLongFrame(in);
        }

        if (in.readableBytes() < lengthFieldEndOffset) {
            return null;
        }

        int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
        long frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, byteOrder);

        if (frameLength < 0) {
            failOnNegativeLengthField(in, frameLength, lengthFieldEndOffset);
        }

        frameLength += lengthAdjustment + lengthFieldEndOffset;

        if (frameLength < lengthFieldEndOffset) {
            failOnFrameLengthLessThanLengthFieldEndOffset(in, frameLength, lengthFieldEndOffset);
        }

        if (frameLength > maxFrameLength) {
            exceededFrameLength(in, frameLength);
            return null;
        }

        // never overflows because it's less than maxFrameLength
        int frameLengthInt = (int) frameLength;
        if (in.readableBytes() < frameLengthInt) {
            return null;
        }

        if (initialBytesToStrip > frameLengthInt) {
            failOnFrameLengthLessThanInitialBytesToStrip(in, frameLength, initialBytesToStrip);
        }
        in.skipBytes(initialBytesToStrip);

        // extract frame
        int readerIndex = in.readerIndex();
        int actualFrameLength = frameLengthInt - initialBytesToStrip;
        ByteBuf frame = in.slice(readerIndex, actualFrameLength);
        in.readerIndex(readerIndex + actualFrameLength);
        return frame;
    }

    protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
        return switch (length) {
            case 1 -> buf.getUnsignedByte(offset);
            case 2 -> ByteOrder.LITTLE_ENDIAN == order ? buf.getUnsignedShortLE(offset) : buf.getUnsignedShort(offset);
            case 3 -> ByteOrder.LITTLE_ENDIAN == order ? buf.getUnsignedMediumLE(offset) : buf.getUnsignedMedium(offset);
            case 4 -> ByteOrder.LITTLE_ENDIAN == order ? buf.getUnsignedIntLE(offset) : buf.getUnsignedInt(offset);
            case 8 -> ByteOrder.LITTLE_ENDIAN == order ? buf.getLongLE(offset) : buf.getLong(offset);
            default -> throw new DecoderException("unsupported lengthFieldLength: " + lengthFieldLength + " (expected: 1, 2, 3, 4, or 8)");
        };
    }

    private void failIfNecessary(boolean firstDetectionOfTooLongFrame) {
        if (bytesToDiscard == 0) {
            // Reset to the initial state and tell the handlers that
            // the frame was too large.
            long tooLongFrameLength = this.tooLongFrameLength;
            this.tooLongFrameLength = 0;
            discardingTooLongFrame = false;
            if (!failFast || firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        } else {
            // Keep discarding and notify handlers if necessary.
            if (failFast && firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        }
    }

    private void fail(long frameLength) {
        if (frameLength > 0) {
            throw new TooLongFrameException("Adjusted frame length exceeds " + maxFrameLength + ": " + frameLength + " - discarded");
        } else {
            throw new TooLongFrameException("Adjusted frame length exceeds " + maxFrameLength + " - discarding");
        }
    }

}
