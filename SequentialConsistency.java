import java.io.IOException;
import java.net.UnknownHostException;

/**
 * ReadYourWriteConsistency subclass implementing consistency functions
 */
public class SequentialConsistency extends ConsistencyProtocol{

    public SequentialConsistency(Bulletin_Board board) {
    	 super(board);
    }

    // primary updates locally and asks everyone else to update
    @Override
    public synchronized void handleServerUpdate(String[] action) {
    	this.board.updateDatabaseLocally(action);
        if (this.board.isPrimary()) {
            // Broadcast the update to all the servers from primary
            broadcastUpdate(action);
        }
    }

    @Override
    public synchronized String choose(int article_id) {
        return this.board.getArticle(article_id);
    }

    @Override
    public synchronized String read() {
        return this.board.displayBoard();
    }
    
    // send action/request to Primary server
    public int handleClientUpdate(String[] action) {
        ServerInfo primaryServer = this.board.getPrimaryServerInfo();
        try {
            TCPConnection connection = new TCPConnection(primaryServer);
            // get id for this request
            int new_id = this.board.getNewId(connection);
            // send to primary
            this.board.sendArticle(connection, action, new_id);
            connection.close();
            return new_id;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return CONSTANTS.NEG_ID;
    }
}