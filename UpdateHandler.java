import java.io.IOException;

/**
 *  Send update to a server and wait for completion
 */
public class UpdateHandler extends Thread{
    private ServerInfo server;
    private String[] action;

    public UpdateHandler(ServerInfo server, String[] action) {
        this.server = server;
        this.action = action;
    }

    @Override
    public void run() {
        try {
            TCPConnection tcpConnection =  new TCPConnection(server);

            Utils.writeStringWithRandomDelay(tcpConnection.getObjectOutputStream(),
                    action[0]+":"+action[1]+":"+action[2]+":"+action[3]+":"+action[4]);

            String reply = tcpConnection.getObjectInputStream().readUTF();
            String[] list = reply.split(":");
            if (    list[0].equalsIgnoreCase("SERVER") &&
                    list[1].equalsIgnoreCase("ACK") &&
                    list[2].equalsIgnoreCase(action[2]) &&
                    list[3].equalsIgnoreCase(action[3])
                    ) {
            } else {
                System.out.println("Update ACK not received correctly from [ " + server.toString() + " ]");
            }

            tcpConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}