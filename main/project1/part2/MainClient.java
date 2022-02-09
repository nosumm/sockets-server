import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class MainClient {
    public static final int HEADER_LENGTH = 2 * Integer.BYTES + 2 * Short.BYTES;

    private Random generator = new Random();
    private DatagramSocket udpSocket = null;

    /**
     * Blocking loop that handles all clients that initially connect to the client.
     * Validates that the client sends the stage A information correctly and if so,
     * spawns a server thread handler to handle stages B-D
     */
    public void stageA() throws IOException {
        final byte[] expectedPayload = "hello world\0".getBytes();
        final int RX_BUFF_SIZE = HEADER_LENGTH + expectedPayload.length + 1;
        final int UDP_PORT = 12235;

        udpSocket = new DatagramSocket(UDP_PORT);

        while (true) {
            ByteBuffer rxBuffer = ByteBuffer.allocate(RX_BUFF_SIZE);

            DatagramPacket rxPacket = new DatagramPacket(rxBuffer.array(), rxBuffer.array().length);
            udpSocket.receive(rxPacket);

            // verify stuff in header, including the packet and payload length, is
            // reasonable
            if (!PacketVerifier.verifyPacket(rxBuffer, rxPacket.getLength(), expectedPayload.length, 0)) {
                System.err.println("stage A header verification failed");
                continue;
            }

            // Verify payload contents are "hello world\0"
            byte[] receivedPayload = Arrays.copyOfRange(rxBuffer.array(), HEADER_LENGTH,
                    HEADER_LENGTH + expectedPayload.length);
            if (!Arrays.equals(receivedPayload, expectedPayload)) {
                System.err.println("receivedPayload not equal to expectedPayload");
                continue;
            }

            // everything is correct, send the goodies
            DatagramSocket partBSocket = new DatagramSocket();
            partBSocket.setSoTimeout(3000);
            ByteBuffer responsePayload = ByteBuffer.allocate(16);
            int stageBNum = generator.nextInt(10) + 10;
            int stageBLen = generator.nextInt(50);
            int secretA = generator.nextInt(100);

            responsePayload.putInt(stageBNum);
            responsePayload.putInt(stageBLen);
            responsePayload.putInt(partBSocket.getLocalPort());
            responsePayload.putInt(secretA);

            ByteBuffer txPacket = PayloadCreator.constructPayloadWithHeader(responsePayload.array(), 0, (short) 2);

            DatagramPacket txPacketDatagram = new DatagramPacket(
                txPacket.array(),
                txPacket.array().length,
                rxPacket.getAddress(),
                rxPacket.getPort());

            this.udpSocket.send(txPacketDatagram);

            System.out.println("num " + stageBNum + ", len " + stageBLen + ", udpPort " + partBSocket.getLocalPort() + ", secretA " + secretA);

            // stage A received correctly, spawn a server thread handler to handle stages B-D
            ServerThreadHandler threadHandler = new ServerThreadHandler(partBSocket, stageBNum, stageBLen, secretA);
            threadHandler.start();
        }
    }
}
