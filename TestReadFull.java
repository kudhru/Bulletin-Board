import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 *  Driver to randomly read from server
 */
public class TestReadFull extends Thread{
    private final int numReads;
    private final List<ServerInfo> serverInfoList;
    private final int maxArticleId;
    private Logger logger = Logger.getLogger(TestRead.class.getName());

    TestReadFull(int numReads, List<ServerInfo> serverInfoList, int maxArticleId) {
        this.numReads = numReads;
        this.serverInfoList = serverInfoList;
        this.maxArticleId = maxArticleId;
    }

    public void run(){
        int serverId;
        String response, message;
        Random rand = new Random();

            try {
                serverId = rand.nextInt(this.serverInfoList.size());
                TCPConnection tcpConnection = new TCPConnection(this.serverInfoList.get(serverId));

                message = String.format("CLIENT:READ");
                this.logger.info(String.format("Client Posting to server %s %d: %s",
                        tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort(), message));
                Utils.writeString(tcpConnection.getObjectOutputStream(), message);
                response = tcpConnection.getObjectInputStream().readUTF();
                System.out.println("Client received ## \n" + response);

                tcpConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
   }
}
