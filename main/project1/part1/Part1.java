import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Part1 {
    private static final String ADDRESS_NAME = "attu5.cs.washington.edu";

    private UdpClient udpClient = null;
    private TcpClient tcpClient = null;

    // all the secrets
    private int secretA = 0;
    private int secretB = 0;
    private int secretC = 0;
    private int secretD = 0;

    // stuff received from stage a
    private int num = 0;
    private int len = 0;
    private int udpPort = 0;

    // stuff received from stage b
    private int tcpPort = 0;

    // stuff received from stage c
    private int num2 = 0;
    private int len2 = 0;
    private byte c = 0;

    public Part1() throws SocketException, UnknownHostException {
        // Initially set up the UDP client to be used for stage a
        udpClient = new UdpClient(ADDRESS_NAME);
    }

    /**
     * Send "hello world" packet, wait for response, parse the response, and
     * store data in PartA object.
     */
    public void stageA() throws IOException {
        String hi = "hello world\0";

        // transmit across UDP until write succeeds
        while (true) {
            this.udpClient.write(hi.getBytes(), 12235, 0);

            try {
                // If we don't receive anything within some time retry writing
                ByteBuffer readBuf = this.udpClient.read(4 * Integer.BYTES);

                num = readBuf.getInt();
                len = readBuf.getInt();
                udpPort = readBuf.getInt();
                secretA = readBuf.getInt();

                // exit while (true) loop
                break;
            } catch (SocketTimeoutException e) {
                System.out.println(e + ", retrying stage A");
            }
        }

        System.out.println("num " + num + ", len " + len + ", udpPort " + udpPort + ", secretA " + secretA);
    }

    /**
     * Send num UDP packets, each time waiting for a response. Once all num packets
     * sent, receive the final packet and parse the result.
     */
    public void stageB() throws IOException {
        for (int i = 0; i < num; i++) {
            ByteBuffer payload = ByteBuffer.allocate(len + 4);
            payload.putInt(i);

            while (true) {
                this.udpClient.write(payload.array(), udpPort, secretA);

                try {
                    ByteBuffer rxBuff = udpClient.read(Integer.BYTES);
                    int ackId = rxBuff.getInt();
                    if (ackId == i) {
                        // only move onto next packet id when ack packet id matches
                        break;
                    }
                    System.out.println("invalid packet id received, retrying id = " + i);
                } catch (SocketTimeoutException e) {
                    System.out.println(e + ", retrying id = " + i);
                }
            }
        }

        // keep trying to read result of stage B (SocketTimeoutException shouldn't happen though)
        while (true) {
            try {
                ByteBuffer stageBRes = udpClient.read(2 * Integer.BYTES);
                tcpPort = stageBRes.getInt();
                secretB = stageBRes.getInt();
                break;
            } catch (SocketTimeoutException e) {
                System.out.println(e + ", trying to read stage B result again");
            }
        }

        System.out.println("tcpPort " + tcpPort + ", secretB " + secretB);

        // we are done with the udp client, so close it
        udpClient.close();
        udpClient = null;
    }

    /**
     * Reads TCP port given in stage B, parses the data to be used for stage D.
     */
    public void stageC() throws IOException {
        tcpClient = new TcpClient(ADDRESS_NAME, tcpPort);
        ByteBuffer rxArr = tcpClient.read(4 * Integer.BYTES);

        num2 = rxArr.getInt();
        len2 = rxArr.getInt();
        secretC = rxArr.getInt();
        c = rxArr.get();

        System.out.println("num2 " + num2 + ", len2 " + len2 + ", secretC " + secretC + ", c " + c);
    }

    /**
     * Writes num2 packets that contain the character c to TCP port, reads from the TCP
     * port afterwards to get the final secret.
     */
    public void stageD() throws IOException {
        byte[] txArr = new byte[len2];
        Arrays.fill(txArr, c);

        for (int i = 0; i < num2; i++) {
            tcpClient.write(txArr, secretC);
        }

        ByteBuffer rxArr = tcpClient.read(Integer.BYTES);
        secretD = rxArr.getInt();
        System.out.println("secretD " + secretD);

        tcpClient.close();
        tcpClient = null;
    }

    public void closeAll() throws IOException {
        if (udpClient != null) {
            udpClient.close();
            udpClient = null;
        }

        if (tcpClient != null) {
            tcpClient.close();
            tcpClient = null;
        }
    }

    public static void main(String[] args) throws SocketException, UnknownHostException, IOException {
        Part1 p1 = new Part1();

        p1.stageA();
        p1.stageB();
        p1.stageC();
        p1.stageD();
        p1.closeAll();  // should already be closed by close all just in case
    }
}
