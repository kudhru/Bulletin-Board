import java.util.List;

public class QuorumSyncThread extends Thread{

    private List<TCPConnection> openTCPConnections;
    private List<ServerInfo> servers;
    private int primaryServerNum;
    volatile boolean done = false;

    QuorumSyncThread(List<ServerInfo> servers, int primaryServerNum) {
        this.primaryServerNum = primaryServerNum;
        this.servers = servers;

    }

    void close() { done = true; }

    public void run() {
        while(!done) {
            try {
                sleep(CONSTANTS.QUORUM_SYNC_TIME);
                openTCPConnections = Utils.openTCPConnections(servers);
                Utils.sync(servers, openTCPConnections, primaryServerNum);
                Utils.closeTCPConnections(openTCPConnections);
            } catch (InterruptedException e) { }
        }
    }
}