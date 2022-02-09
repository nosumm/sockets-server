import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UdpClient {
    private DatagramSocket socket = null;
    private InetAddress serverAddress = null;

    final int HEADER_LENGTH = 12;
    final int TIMEOUT = 1000;
    final int STUDENT_NUMBER = 799;

    public UdpClient(String serverAddress) throws UnknownHostException, SocketException {
        socket = new DatagramSocket();
        socket.setSoTimeout(TIMEOUT);
        this.serverAddress = InetAddress.getByName(serverAddress);
    }

    public void write(byte[] payload, int port, int psecret) throws IOException {
        ByteBuffer txPacket = PayloadCreator.constructPayloadWithHeader(payload, psecret, (short) 1);

        DatagramPacket txPacketDatagram = new DatagramPacket(txPacket.array(), txPacket.array().length,
                this.serverAddress, port);
        this.socket.send(txPacketDatagram);
    }

    public ByteBuffer read(int payloadLength) throws IOException {
        ByteBuffer rxPacket = ByteBuffer.allocate(HEADER_LENGTH + payloadLength);
        DatagramPacket dgramPacket = new DatagramPacket(rxPacket.array(), rxPacket.array().length);
        socket.receive(dgramPacket);
        return ByteBuffer.wrap(Arrays.copyOfRange(rxPacket.array(), HEADER_LENGTH, rxPacket.array().length));
    }

    public void close() {
        socket.close();
    }
}
