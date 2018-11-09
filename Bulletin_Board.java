import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class which serves as the entry point into server
 */
public class Bulletin_Board {

    private DataService dataService;        // Holds all data

    private ConsistencyTypes consistencyType;
    private ConsistencyProtocol consistencyProtocol;    // Base class for all consistencies
    private QuorumSyncThread quorumSyncThread = null;   // Sync Thread

    private List<ServerInfo> servers;                   // List of all the servers
    private ServerInfo primaryServerInfo;               // priamry server
    private RequestListener requestListener;            // TCP server which listens to incoming requests

    private boolean isPrimary = false;
    private int serverNum;                  // Current server number
    private int primaryServerNum;           // primary server number
    private List<Integer> sequentialLog;    // log maintained in sequential consistency
    int numWriteQuorum;     // Nw
    int numReadQuorum;      // Nr

    private int article_id;
    private int listenerPort;

    private Logger logger = Logger.getLogger(Bulletin_Board.class.getName());


	public Bulletin_Board(ConsistencyTypes consistencyType, List<ServerInfo> servers,
                          int primaryServerNum, int serverNum,
                          int numWriteQuorum, int numReadQuorum) {
        this.primaryServerNum = primaryServerNum;
        if (primaryServerNum == serverNum) {
            this.isPrimary = true;
        }
        this.primaryServerInfo = servers.get(primaryServerNum);
        this.servers = servers;
        this.serverNum = serverNum;
        this.listenerPort = servers.get(serverNum).getPort();

        this.article_id = 0;
        this.numWriteQuorum = numWriteQuorum;
        this.numReadQuorum = numReadQuorum;
        this.sequentialLog = new ArrayList<>();
        this.consistencyType = consistencyType;

        initTopicStream();
        initConsistencyProtocol();

        // Start the TCP server for accepting incoming connections
        this.requestListener = new RequestListener(this, this.listenerPort);
        requestListener.start();

        if (isPrimary && consistencyType == ConsistencyTypes.QUORUM) {
            quorumSyncThread = new QuorumSyncThread(getServers(), getPrimaryServerNum());
        }
    }

    //____________________________________________________________________________________________
    @Override
    public void finalize() {
        close();
    }

    public void close() {
	    requestListener.stop();
        if (isPrimary && consistencyType == ConsistencyTypes.QUORUM) {
            quorumSyncThread.close();
        }
    }

    // init board in this server
    private void initTopicStream() {
        dataService = new DataService();
        Article rootArticle = new Article(-1 , -1, "");
        dataService.putArticle(rootArticle);
    }

    // create consistency policy subclass
    private void initConsistencyProtocol() {
        switch (this.getConsistencyType()) {
            case QUORUM:
            	consistencyProtocol = new QuorumConsistency(this);
                break;

            case SEQUENTIAL:
                consistencyProtocol = new SequentialConsistency(this);
                break;

            case READ_YOUR_WRITE:
            	consistencyProtocol = new ReadYourWriteConsistency(this);
                break;

            default:
                System.out.println("Invalid Consistency Type");
                System.exit(1);
                break;
        }
    }

    //____________________________________________________________________________________________

    public synchronized void addCommitLog(int num) { sequentialLog.add(num); }

    public int getPrimaryServerNum() { return primaryServerNum; }

    public int getServerNum() { return serverNum; }

    public ServerInfo getPrimaryServerInfo() { return primaryServerInfo; }

    public int getListenerPort() { return listenerPort; }

    public DataService getDataService() { return dataService; }

    public List<ServerInfo> getServers() { return servers; }

    public boolean isPrimary() { return isPrimary; }

    public ConsistencyTypes getConsistencyType() { return consistencyType; }
    
    public int getNumWriteQuorum() { return numWriteQuorum; }

	public int getNumReadQuorum() { return numReadQuorum; }

    public List<Integer> getSequentialLog() { return sequentialLog; }

    // update the max id seen till now
    public synchronized void setMaxArticleID (int id) {
        this.article_id = Math.max(this.article_id, id);
    }

    // SEQUENCER :: generate next id to be sent to caller
    public synchronized int getNextID () {
        this.article_id++;
        return article_id;
    }

    // max id number of article seen in this server
    public synchronized int getMaxArticleID () { return article_id; }

    //____________________________________________________________________________________________

    // update local database only, called from coordinator from different server
    public int updateDatabaseLocally(String[] action) {
        int parent_id = Integer.parseInt(action[2]);
        int article_id = Integer.parseInt(action[3]);
        Article article = new Article(article_id, parent_id, action[4]);

        this.logger.info(String.format("Updating local Database Article: %s", article.toString()));

        Article parentArticle = this.getDataService().getArticle(parent_id);
        if(parentArticle != null) {
            this.getDataService().putArticle(article);
            parentArticle.addReply(article_id);
            addCommitLog(article_id);
            return article_id;
        } else {
            this.logger.info(String.format(
                    "!! Update withholded by system, parent missing -- Article: %s !!", article.toString()));
            return CONSTANTS.NEG_ID;
        }
    }

    // get a specific article from database
    public synchronized String getArticle(int article_id) {
        Article article = getDataService().getArticle(article_id);
        if(article != null) {
            return article.getContent();
        } else {
            return CONSTANTS.ARTICLE_DOES_NOT_EXIST;
        }
    }

    // Convert the board into a string to be sent to client
    public synchronized String displayBoard() {
        return Utils.convertDatabaseMapToString(dataService.getDatabase());
    }
    
	public String read() {
		return this.consistencyProtocol.read();
	}

    public String choose(int article_id) {
        return consistencyProtocol.choose(article_id);
    }

    // handles inter-server update operations
    public void handleServerUpdate(String[] action) {
        this.consistencyProtocol.handleServerUpdate(action);
    }

    // handle updates from the client
    public int handleClientUpdate(String[] action) {
        return this.consistencyProtocol.handleClientUpdate(action);
    }

    // Ask for the next id before posting / replying [server does it]
	public int getNewId(TCPConnection connection) throws IOException {
	    Utils.writeStringWithRandomDelay(connection.getObjectOutputStream(), "SERVER:GET_NEXT_ID");
        String reply = connection.getObjectInputStream().readUTF();
        return Integer.parseInt(reply.split(":")[2]);
    }

    // Write database object into output stream ina thread safe way
    public synchronized void sendDatabase(ObjectOutputStream outputStream) {
        Utils.delayRandomTime();
        try {
            Utils.writeObjectWithRandomDelay(outputStream, dataService.getDatabase());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // send list of all the commits done till now in this server
    public synchronized void sendCommitLog(ObjectOutputStream outputStream) {
        try {
            Utils.writeObject(outputStream, this.sequentialLog);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Using connection object send an an article/action to a server
    public void sendArticle(TCPConnection connection, String[] action, int article_id) throws IOException {
        String str = Utils.constructUpdateActionForServers(action, article_id);
        Utils.writeStringWithRandomDelay(connection.getObjectOutputStream(), str);
        String received = connection.getObjectInputStream().readUTF();
        if (!received.equalsIgnoreCase("SERVER:ACK:" + str.split(":")[2] + ":" + article_id)) {
            this.logger.info("Update did not completed : " + str);
        }
    }
}
