import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all consistency sub classes
 */
public abstract class ConsistencyProtocol {

    protected Bulletin_Board board;

    public ConsistencyProtocol(Bulletin_Board board) {
        this.board = board;
    }

    // handles inter-server update operations
    public abstract void handleServerUpdate (String[] action);

    // handle updates from the client
    public abstract int handleClientUpdate(String[] action);

    // choose operation
    public abstract String choose (int article_id);

    // read operation
    public abstract String read ();

    // broadcast operation action to all servers
    protected void broadcastUpdate (String[] action) {
        System.out.println("broadcastUpdate :: " +
                action[0] + ":" + action[1] + ":" + action[2] + ":" + action[3] + ":" + action[4]);

        List<Thread> idList = new ArrayList<>();
        for (ServerInfo server : this.board.getServers()) {
            try {
                if ( (  server.getIP().equalsIgnoreCase("localhost") ||
                        InetAddress.getByName(server.getIP()).getHostAddress().equalsIgnoreCase(
                                InetAddress.getLocalHost().getHostAddress()) ) &&
                        server.getPort() == this.board.getListenerPort()) {
                    continue;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            // start thread in parallel for all server updates
            UpdateHandler handler = new UpdateHandler(server, action);
            handler.start();
            idList.add(handler);
        }

        // Waiting for all threads to end
        for (Thread thread : idList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}