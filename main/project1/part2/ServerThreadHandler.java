import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

// Class to handle 1 client connection

public class ServerThreadHandler implements Runnable {
    private Thread serverThread = null;
    private DatagramSocket partBSocket = null;
    private Random generator = new Random();

    private int secretA = 0;
    private int secretB = 0;
    private int secretC = 0;
    private int secretD = 0;

    private int stageBNum = 0;
    private int stageBLen = 0;

    private int stageCNum = 0;
    private int stageCLen = 0;
    private byte stageCChar = 0;

    private ServerSocket partCServerSocket = null;
    private Socket partCSocket = null;

    public ServerThreadHandler(DatagramSocket partBSocket, int stageBNum, int stageBLen, int secretA) {
        this.serverThread = new Thread(this);
        this.partBSocket = partBSocket;
        this.stageBNum = stageBNum;
        this.stageBLen = stageBLen;
        this.secretA = secretA;
    }

    @Override
    public void run() {
        try {
            if (!stageB())
                return;
            stageC();
            stageD();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void start() {
        this.serverThread.start();
    }

    public boolean stageB() throws IOException {
        DatagramPacket rxPacket = null;

        for (int i = 0; i < stageBNum; i++) {
            // +1 added so that if the client passes an array larger than the expected
            // length we can detect it
            ByteBuffer rxBuffer = ByteBuffer.allocate(
                    stageBLen + Integer.BYTES + PacketVerifier.HEADER_LENGTH + getAlignedBytes(stageBLen + Integer.BYTES) + 1);

            // receive the packet
            rxPacket = new DatagramPacket(rxBuffer.array(), rxBuffer.array().length);
            partBSocket.receive(rxPacket);

            // verify header, including length
            if (!PacketVerifier.verifyPacket(rxBuffer, rxPacket.getLength(), stageBLen + Integer.BYTES, secretA)) {
                System.err.println("stage b failed header verification");
                partBSocket.close();
                return false;
            }

            // verify payload
            if (rxBuffer.getInt(PacketVerifier.HEADER_LENGTH) != i) {
                System.err.println("stage b payload verification failed");
                partBSocket.close();
                return false;
            }

            // should be all 0s after packet id
            byte[] receivedPayload = Arrays.copyOfRange(
                rxBuffer.array(),
                PacketVerifier.HEADER_LENGTH + Integer.BYTES,
                PacketVerifier.HEADER_LENGTH + Integer.BYTES + stageBLen);
            for (byte b : receivedPayload) {
                if (b != 0) {
                    System.err.println("stage b zero byte payload verification failed");
                    partBSocket.close();
                    return false;
                }
            }

            // Decide whether to ack the current packet
            if (generator.nextInt() % 2 == 0) {
                ByteBuffer txBuffer = ByteBuffer.allocate(Integer.BYTES);
                txBuffer.putInt(i);

                ByteBuffer payloadWithHeader = PayloadCreator.constructPayloadWithHeader(txBuffer.array(), secretA, (short) 2);
                DatagramPacket txPacket = new DatagramPacket(payloadWithHeader.array(),
                        payloadWithHeader.array().length, rxPacket.getAddress(), rxPacket.getPort());
                partBSocket.send(txPacket);
            } else {
                i--;
            }
        }

        if (rxPacket == null) {
            partBSocket.close();
            System.err.println("never connected to a client properly (which is impossible)");
            return false;
        }

        // Create the response packet with tcpPort and secretB
        secretB = generator.nextInt(100);
        partCServerSocket = new ServerSocket(0);
        partCServerSocket.setSoTimeout(3000);

        ByteBuffer txBuffer = ByteBuffer.allocate(2 * Integer.BYTES);
        txBuffer.putInt(partCServerSocket.getLocalPort());
        txBuffer.putInt(secretB);

        ByteBuffer payloadWithHeader = PayloadCreator.constructPayloadWithHeader(txBuffer.array(), secretB, (short) 2);
        DatagramPacket txPacket = new DatagramPacket(
            payloadWithHeader.array(),
            payloadWithHeader.array().length,
            rxPacket.getAddress(), rxPacket.getPort());

        partBSocket.send(txPacket);

        System.out.println("tcpPort " + partCServerSocket.getLocalPort() + ", secretB " + secretB);

        partBSocket.close();

        return true;
    }

    public void stageC() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(3 * Integer.BYTES + 1);
        stageCNum = generator.nextInt(100);
        stageCLen = generator.nextInt(100);
        secretC = generator.nextInt(100);

        stageCChar = (byte) generator.nextInt();

        byteBuffer.putInt(stageCNum);
        byteBuffer.putInt(stageCLen);
        byteBuffer.putInt(secretC);
        byteBuffer.put(stageCChar);

        ByteBuffer responseWithHeader = PayloadCreator.constructPayloadWithHeader(byteBuffer.array(), secretB, (short) 2);

        partCSocket = partCServerSocket.accept();

        partCSocket.getOutputStream().write(responseWithHeader.array());

        System.out.println("num2 " + stageCNum + ", len2 " + stageCLen + ", secretC " + secretC + ", c " + stageCChar);
    }

    public void stageD() throws IOException {
        for (int i = 0; i < stageCNum; i++) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(PacketVerifier.HEADER_LENGTH + stageCLen + getAlignedBytes(stageCLen));
            partCSocket.getInputStream().read(byteBuffer.array());
            if (!PacketVerifier.verifyPacket(byteBuffer, byteBuffer.array().length, stageCLen, secretC)) {
                partCSocket.close();
                partCServerSocket.close();
                return;
            }

            for (int j = 0; j < stageCLen; j++) {
                if (byteBuffer.array()[PacketVerifier.HEADER_LENGTH + j] != stageCChar) {
                    partCSocket.close();
                    partCServerSocket.close();
                    return;
                }
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        secretD = generator.nextInt(100);
        byteBuffer.putInt(secretD);
        ByteBuffer responseWithHeader = PayloadCreator.constructPayloadWithHeader(byteBuffer.array(), secretC, (short) 2);
        partCSocket.getOutputStream().write(responseWithHeader.array());

        partCSocket.close();
        partCServerSocket.close();

        System.out.println("secretD " + secretD);
    }

    private static int getAlignedBytes(int payloadLen) {
        return (payloadLen % 4 == 0) ? 0 : 4 - (payloadLen % 4);
    }
}
