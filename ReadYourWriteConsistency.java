import java.io.IOException;
import java.net.UnknownHostException;


/**
 * ReadYourWriteConsistency subclass implementing consistency functions
 */
public class ReadYourWriteConsistency extends ConsistencyProtocol{

	public ReadYourWriteConsistency(Bulletin_Board board) {
		super(board);
	}

	// if a server asks it to update, update locally
	@Override
	public void handleServerUpdate(String[] action) {
		this.board.updateDatabaseLocally(action);
	}

	// choose a single article
	@Override
	public synchronized String choose(int article_id) {
		String reply = CONSTANTS.ARTICLE_DOES_NOT_EXIST;
		// wait till update for id asked in done
		while(reply.equalsIgnoreCase(CONSTANTS.ARTICLE_DOES_NOT_EXIST)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			reply = this.board.getArticle(article_id);
		}
		return reply;
	}

	// Read the whole database
	@Override
	public String read() {
		int local_max_id = board.getMaxArticleID();
		int overall_max_id = -1;

		// ask for maximum if written till now
		ServerInfo primaryServer = this.board.getPrimaryServerInfo();
		try {
			TCPConnection connection = new TCPConnection(primaryServer);
			Utils.writeStringWithRandomDelay(connection.getObjectOutputStream(), "SERVER:SEND_MAX_ID");
			overall_max_id = Integer.parseInt(connection.getObjectInputStream().readUTF().split(":")[2]);
			connection.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// wait till updates are done
		while(local_max_id < overall_max_id) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			local_max_id = board.getMaxArticleID();
		}
		// return data
		return this.board.displayBoard();
	}

	// Handle request from clients
	public int handleClientUpdate(String[] action) {
		ServerInfo primaryServer = this.board.getPrimaryServerInfo();
		try {
			// get new if for this update
			TCPConnection connection = new TCPConnection(primaryServer);
			int new_id = this.board.getNewId(connection);
			connection.close();

			String str = Utils.constructLocalUpdateActionForServers(action, new_id);
			String[] newAction = str.split(":");

			// send this action to server part code to handle
			this.handleServerUpdate(newAction);

			// Starting a lazy propagation thread
			new Thread(){
				@Override
				public void run() {
					broadcastUpdate(newAction);
				}
			}.start();

			return new_id;

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return CONSTANTS.NEG_ID;
	}
}