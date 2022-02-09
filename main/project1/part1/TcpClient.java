import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TcpClient {
    Socket socket = null;

    public TcpClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
    }

    public void write(byte[] payload, int psecret) throws IOException {
        ByteBuffer txPacket = PayloadCreator.constructPayloadWithHeader(payload, psecret, (short) 1);
        socket.getOutputStream().write(txPacket.array());
    }

    public ByteBuffer read(int payloadLength) throws IOException {
        ByteBuffer rxPacket = ByteBuffer.allocate(PayloadCreator.HEADER_LENGTH + payloadLength);
        socket.getInputStream().read(rxPacket.array());
        return ByteBuffer.wrap(Arrays.copyOfRange(
            rxPacket.array(),
            PayloadCreator.HEADER_LENGTH,
            rxPacket.array().length));
    }

    public void close() throws IOException {
        socket.close();
    }
}
