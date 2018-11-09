import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QuorumConsistency subclass implementing consistency functions
 */
public class QuorumConsistency extends ConsistencyProtocol {

	public QuorumConsistency(Bulletin_Board board) {
		super(board);
	}

	// Randomly get a read/write quorum
	List<ServerInfo> getQuorum(List<ServerInfo> serverList , int num) {
		Set<ServerInfo> quorum = new HashSet<>();
		int n = serverList.size();
		Random random = new Random();
		
		while(quorum.size() != num) {
			int rand = random.nextInt(n);
			quorum.add(serverList.get(rand));
		}
		ArrayList<ServerInfo> list = new ArrayList<>();
		list.addAll(quorum);
		return list;
	}

	// Handle server actions -- make a quorum and run operations
	@Override
	public void handleServerUpdate(String[] action) {
		// get a write quorum
		List<ServerInfo> writeQuorum = getQuorum(board.getServers(), board.getNumWriteQuorum());

		List<TCPConnection> openTCPConnections = Utils.openTCPConnections(writeQuorum);

		// make all replicas consistent in this quorum
		Utils.sync(writeQuorum, openTCPConnections, board.getPrimaryServerNum());

		for (int i = 0 ; i < writeQuorum.size() ; i++) {
			ObjectOutputStream outputStream = openTCPConnections.get(i).getObjectOutputStream();
			ObjectInputStream inputStream = openTCPConnections.get(i).getObjectInputStream();
			// Update all servers in quorum
			try {
				Utils.writeStringWithRandomDelay(outputStream,
						"SERVER:LOCAL_UPDATE:"+ action[2] +":" + action[3] + ":" + action[4]);
                String reply = inputStream.readUTF();
                String[] list = reply.split(":");
                if (    list[0].equalsIgnoreCase("SERVER") &&
                        list[1].equalsIgnoreCase("ACK") &&
                        list[2].equalsIgnoreCase(action[2]) &&
                        list[3].equalsIgnoreCase(action[3])
                        ) {
                } else {
                    System.out.println("Update ACK not received correctly");
                }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Utils.closeTCPConnections(openTCPConnections);
	}

	// get update request fro client and send to server function
	public int handleClientUpdate(String[] action) {
		ServerInfo primaryServer = this.board.getPrimaryServerInfo();
		try {
			// get new id for this request
			TCPConnection connection = new TCPConnection(primaryServer);
			int new_id = this.board.getNewId(connection);
			connection.close();

			// Holding quorum for post and reply in same machine
			String str = Utils.constructUpdateActionForServers(action, new_id);
			this.handleServerUpdate(str.split(":"));

			return new_id;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return CONSTANTS.NEG_ID;
	}

	// Choose one article from whole database
	@Override
	public String choose(int article_id) {
		// get a read quorum
		List<ServerInfo> readQuorum = getQuorum(board.getServers(), board.getNumReadQuorum());

		List<TCPConnection> openTCPConnections = Utils.openTCPConnections(readQuorum);

		// Ask for if any of the servers have the article and return it
		for (int i = 0 ; i < readQuorum.size() ; i++) {
			ObjectOutputStream outputStream = openTCPConnections.get(i).getObjectOutputStream();
			ObjectInputStream inputStream = openTCPConnections.get(i).getObjectInputStream();
			try {
				Utils.writeStringWithRandomDelay(outputStream, "SERVER:GET_ARTICLE:"+ article_id);
				String contents = inputStream.readUTF();
				if (!contents.equalsIgnoreCase(CONSTANTS.ARTICLE_DOES_NOT_EXIST)) {
					return contents;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Utils.closeTCPConnections(openTCPConnections);
		return CONSTANTS.ARTICLE_DOES_NOT_EXIST;
	}

	// Read the whole database
	@Override
	public String read() {
		// get a read quorum
		List<ServerInfo> readQuorum = getQuorum(board.getServers(), board.getNumReadQuorum());

		List<TCPConnection> openTCPConnections = Utils.openTCPConnections(readQuorum);

		int max_id = Integer.MIN_VALUE;
		TCPConnection selectedServerTCPConnection = null;

		// Ask for the latest version number and maintain who has it
		for (int i = 0 ; i < readQuorum.size() ; i++) {
			ObjectOutputStream outputStream = openTCPConnections.get(i).getObjectOutputStream();
			ObjectInputStream inputStream = openTCPConnections.get(i).getObjectInputStream();

			try {
				// Get the max id from the servers
				Utils.writeStringWithRandomDelay(outputStream, "SERVER:SEND_MAX_ID");
				int serverID = Integer.parseInt(inputStream.readUTF().split(":")[2]);
				if (max_id < serverID) {
					max_id = serverID;
					selectedServerTCPConnection = openTCPConnections.get(i);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Send back the latest version
		ConcurrentHashMap<Integer, Article> database = board.getDataService().getDatabase();
		if(selectedServerTCPConnection != null) {
			try {
				Utils.writeStringWithRandomDelay(selectedServerTCPConnection.getObjectOutputStream(),
						"SERVER:SEND_DATABASE");
				database = (ConcurrentHashMap<Integer, Article>)
						selectedServerTCPConnection.getObjectInputStream().readObject();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		Utils.closeTCPConnections(openTCPConnections);

		String reply = Utils.convertDatabaseMapToString(database);
		return reply;
	}
}
