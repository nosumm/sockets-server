import java.nio.ByteBuffer;

public class PacketVerifier {
    public static final int HEADER_LENGTH = 2 * Integer.BYTES + 2 * Short.BYTES;

    /**
     * @param buffer The buffer received from the client.
     * @param actualPacketLength The length of the received packet. This varies from the length of
     * the buffer (which can be arbitrary). Should be byte-aligned to 4 bytes.
     * @param expectedPayloadLength The expected length of the payload, not including any padding.
     * @param pSecret The expected pSecret that the packet header should contain.
     * @return `true` if the packet has been verified successfully, `false` otherwise.
     */
    public static boolean verifyPacket(ByteBuffer buffer, int actualPacketLength, int expectedPayloadLength, int pSecret) {
        // each packet must include a header
        if (actualPacketLength < HEADER_LENGTH)
            return false;

        // check that the payload length is the same as the expected payload length
        int actualPayloadLength = buffer.getInt(0);
        if (expectedPayloadLength != actualPayloadLength)
            return false;

        // check psecret
        if (pSecret != buffer.getInt(Integer.BYTES))
            return false;

        // check step
        if (buffer.getShort(2 * Integer.BYTES) != 1)
            return false;

        // check that buffer length is aligned to 4 bytes
        if (actualPacketLength % 4 != 0)
            return false;

        // check that packet length is reasonable
        // specified packet length must be in (actualPacketLength - 4, actualPacketLength]
        int specifiedPacketLength = actualPayloadLength + HEADER_LENGTH;
        if (specifiedPacketLength <= actualPacketLength - 4 || specifiedPacketLength > actualPacketLength)
            return false;

        return true;
    }
}
