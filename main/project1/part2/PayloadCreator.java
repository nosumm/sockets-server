import java.io.IOException;
import java.nio.ByteBuffer;

public class PayloadCreator {
    public static final int HEADER_LENGTH = 12;
    public static final short STUDENT_NUMBER = 799;

    public static ByteBuffer constructPayloadWithHeader(byte[] payload, int psecret, short step) throws IOException {
        // Number of bytes to add to the payload to align the payload
        int alignBytes = (payload.length % 4 == 0) ? 0 : 4 - (payload.length % 4);

        int lenAligned = payload.length + alignBytes;

        ByteBuffer txPacket = ByteBuffer.allocate(HEADER_LENGTH + lenAligned);

        // Construct the header
        txPacket.putInt(payload.length);
        txPacket.putInt(psecret);
        txPacket.putShort(step);
        txPacket.putShort(STUDENT_NUMBER);
        txPacket.put(payload);

        return txPacket;
    }
}
