import java.io.IOException;
import java.net.SocketException;

public class Part2 {
    public static void main(String[] args) throws IOException, SocketException {
        MainClient client = new MainClient();

        // Run stage A, which kicks off stage B-D when a client correctly connects
        client.stageA();
    }
}
