import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;


/**
 *  Driver to randomly post and reply
 */
public class TestWrite extends Thread{
    private final int numWrites;
    private final List<ServerInfo> serverInfoList;
    private int maxArticleId;
    private Logger logger = Logger.getLogger(TestWrite.class.getName());

    TestWrite(int numWrites, List<ServerInfo> serverInfoList) {
        this.numWrites = numWrites;
        this.serverInfoList = serverInfoList;
    }

    public int getMaxArticleId() { return this.maxArticleId; }

    public void run(){
        int serverId;
        int writeId;
        String response, message;
        Random rand = new Random();

        for(int i = 0 ; i < this.numWrites ; i++) {
            try {
                serverId = rand.nextInt(this.serverInfoList.size());

                TCPConnection tcpConnection = null;
                    tcpConnection = new TCPConnection(this.serverInfoList.get(serverId));

                message = String.format("CLIENT:POST:Write Message with number = %d", i);
                this.logger.info(String.format("Client Posting to server %s %d: %s",
                        tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort(), message));
                Utils.writeString(tcpConnection.getObjectOutputStream(), message);
                response = tcpConnection.getObjectInputStream().readUTF();
                System.out.println("Client received ## " + response);

                writeId = Integer.parseInt(response.split(":")[1]);
                message = String.format("CLIENT:REPLY:%d:Reply to message", writeId);
                this.logger.info(String.format("Client Posting to server %s %d: %s",
                        tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort(), message));
                Utils.writeString(tcpConnection.getObjectOutputStream(), message);
                response = tcpConnection.getObjectInputStream().readUTF();
                System.out.println("Client received ## " + response);

                this.maxArticleId = Integer.parseInt(response.split(":")[1]);

                tcpConnection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
