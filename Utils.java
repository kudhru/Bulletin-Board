import java.io.*;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

/**
 * Generic utility which implement all genric functions
 */
public class Utils {
	private static Logger logger = Logger.getLogger(Utils.class.getName());

	/**
	 * 	Given a list of servers sync all data by sending delta to all
	 */
	public static void sync(List<ServerInfo> serverList,
							List<TCPConnection> openTCPConnections, int primaryServer) {
		int max_id = CONSTANTS.NEG_ID;
		int numServers = serverList.size(); 
		
		// global database to sync 
		ConcurrentHashMap<Integer, Article> globalDatabase = new ConcurrentHashMap<>();

		// mapping to store updates not seen
		HashMap<ServerInfo, List<Integer>> backLog =  new HashMap<>();
		for (int i = 0 ; i < numServers ; i++) {
			backLog.put(serverList.get(i), new ArrayList<>());
		}

		// Getting max_id now from Primary Server  .......................................................
		try {
			ObjectOutputStream primaryOutputStream = openTCPConnections.get(primaryServer).getObjectOutputStream();
			ObjectInputStream primaryInputStream = openTCPConnections.get(primaryServer).getObjectInputStream();
			// Get the max id from the primary
			writeStringWithRandomDelay(primaryOutputStream, "SERVER:SEND_MAX_ID");
			max_id =  Math.max(max_id, Integer.parseInt(primaryInputStream.readUTF().split(":")[2]));
		} catch (IOException e) {
			e.printStackTrace();
		}
		// ................................................................................................

		for (int i = 0 ; i < numServers ; i++) {
			try {
				ObjectOutputStream outputStream = openTCPConnections.get(i).getObjectOutputStream();
				ObjectInputStream inputStream = openTCPConnections.get(i).getObjectInputStream();

				writeStringWithRandomDelay(outputStream, "SERVER:SEND_DATABASE");
		        
		        ConcurrentHashMap<Integer, Article> data =
						(ConcurrentHashMap<Integer, Article>) inputStream.readObject();
		        
		        // Sync with global hash map and add mapping
		        for (int article_id = 1 ; article_id <= max_id ; article_id++ ) {
					if (data.containsKey(article_id)) {
						if (!globalDatabase.containsKey(article_id))
							globalDatabase.put(article_id, data.get(article_id));
					} else {
						backLog.get(serverList.get(i)).add(article_id);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		// sending the delta to the servers
		for (int i = 0 ; i < numServers ; i++) {
			try {
				ObjectOutputStream outputStream = openTCPConnections.get(i).getObjectOutputStream();

				for (Integer article_id : backLog.get(serverList.get(i))) {
					Article article = globalDatabase.get(article_id);
					if (article != null) {
						writeStringWithRandomDelay(
								outputStream, "SERVER:LOCAL_UPDATE:" +
								article.getParentId() + ":" + article.getId() + ":" + article.getContent());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static List<TCPConnection> openTCPConnections(List<ServerInfo> serverList) {
		// OPEN CONNECTIONS
		List<TCPConnection> openTCPConnections = new ArrayList<>();
		for (ServerInfo server : serverList) {
			try {
				openTCPConnections.add(new TCPConnection(server));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return openTCPConnections;
	}

	public static void closeTCPConnections(List<TCPConnection> openTCPConnections) {
		// CLOSE CONNECTIONS
		for (int i = 0 ; i < openTCPConnections.size() ; i++) {
			try {
				openTCPConnections.get(i).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// make action from client request
	public static String constructUpdateActionForServers(String[] action, int article_id) {
		String content = action[2];
		int parent_id = -1;
		if (action[1].equalsIgnoreCase("REPLY")) {
			parent_id = Integer.parseInt(action[2]);
			content = action[3];
		}
		String str = "SERVER:UPDATE:" + parent_id + ":" + article_id + ":" + content;
		return str;
	}

	public static String constructLocalUpdateActionForServers(String[] action, int article_id) {
		String content = action[2];
		int parent_id = -1;
		if (action[1].equalsIgnoreCase("REPLY")) {
			parent_id = Integer.parseInt(action[2]);
			content = action[3];
		}
		String str = "SERVER:LOCAL_UPDATE:" + parent_id + ":" + article_id + ":" + content;
		return str;
	}

	// Function to decide if packet needs to be delayed
	// 10 percentage packets only will be delayed
	private static boolean decideDelay() {
		Random rand = new Random();
		int check = rand.nextInt(CONSTANTS.DELAY_CONDITION);
		if ((int)(0.1 * check) > 0) {
			return false;	// 90% no delay
		} else {
			return true;	// 10% delay
		}
	}

	public static void delayRandomTime() {
		if (!decideDelay()) {
			return;
		}
		try {
			Random rand = new Random();
			int sleep_time = rand.nextInt(CONSTANTS.RANDOM_TIME_UPPER_LIMIT);
			sleep_time = sleep_time * 100;
			sleep(sleep_time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void writeStringWithRandomDelay(ObjectOutputStream outputStream,
												  String content) throws IOException {
		delayRandomTime();
		writeString(outputStream, content);
	}

	public static void writeObjectWithRandomDelay(ObjectOutputStream outputStream,
												  Object content) throws IOException {
		delayRandomTime();
		writeObject(outputStream, content);
	}

	public static void writeString(ObjectOutputStream outputStream,
								   String content) throws IOException {
		outputStream.writeUTF(content);
		outputStream.flush();
	}

	public static void writeObject(ObjectOutputStream outputStream,
								   Object content) throws IOException {
		outputStream.writeObject(content);
		outputStream.flush();
	}

	public static String convertDatabaseMapToString(ConcurrentHashMap<Integer, Article> database) {
		StringBuffer reply = new StringBuffer("");
		convertDatabaseMapToStringUtil(database, 0 , -1 , reply);
		return reply.toString();
	}

	public static void convertDatabaseMapToStringUtil(ConcurrentHashMap<Integer, Article> database,
													  int level , int parent_id , StringBuffer reply) {
		Article article = database.get(parent_id);
		for (int id : article.getReplies()) {
			printSpaces(level, reply);
			Article reply_article = database.get(id);
			reply.append("" + id + " - " + reply_article.getContent() + "\n");
			convertDatabaseMapToStringUtil(database , level + 1 , id , reply);
		}
	}

	public static void printSpaces(int level , StringBuffer reply) {
		for (int i = 0 ; i < level ; i++) {
			reply.append("  ");
		}
	}

	public static void log(String log) {
		logger.info(log);
	}

	public static String post(List<ServerInfo> serverInfoList, String content) throws IOException {
		Random rand = new Random();
		int serverId = rand.nextInt(serverInfoList.size());

		TCPConnection tcpConnection = new TCPConnection(serverInfoList.get(serverId));

		String message = String.format("CLIENT:POST:%s", content);
		System.out.println(String.format("Client Posting to server %s %d: %s",
				tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort(), message));
		Utils.writeString(tcpConnection.getObjectOutputStream(), message);

		String response = tcpConnection.getObjectInputStream().readUTF();
		tcpConnection.close();
		return response;
	}

	public static String reply(List<ServerInfo> serverInfoList, int articleId, String content) throws IOException {
		Random rand = new Random();
		int serverId = rand.nextInt(serverInfoList.size());

		TCPConnection tcpConnection = new TCPConnection(serverInfoList.get(serverId));

		String message = String.format("CLIENT:REPLY:%d:%s", articleId, content);
		System.out.println(String.format("Client replying to server %s %d for articleId %d: %s",
				tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort(), articleId, message));
		Utils.writeString(tcpConnection.getObjectOutputStream(), message);

		String response = tcpConnection.getObjectInputStream().readUTF();
		tcpConnection.close();
		return response;
	}

	public static String choose(List<ServerInfo> serverInfoList, int articleId) throws IOException {
		Random rand = new Random();
		int serverId = rand.nextInt(serverInfoList.size());

		TCPConnection tcpConnection = new TCPConnection(serverInfoList.get(serverId));

		String message = String.format("CLIENT:CHOOSE:%d", articleId);
		System.out.println(String.format("Client sending a CHOOSE request to server %s %d for articleId %d",
				tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort(), articleId));
		Utils.writeString(tcpConnection.getObjectOutputStream(), message);

		String response = tcpConnection.getObjectInputStream().readUTF();
		tcpConnection.close();
		return response;
	}

	public static String read(List<ServerInfo> serverInfoList) throws IOException {
		Random rand = new Random();
		int serverId = rand.nextInt(serverInfoList.size());

		TCPConnection tcpConnection = new TCPConnection(serverInfoList.get(serverId));

		String message = String.format("CLIENT:READ");
		System.out.println(String.format("Client sending a READ request to server %s %d",
				tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort()));
		Utils.writeString(tcpConnection.getObjectOutputStream(), message);

		String response = tcpConnection.getObjectInputStream().readUTF();
		tcpConnection.close();
		return response;
	}

	public static List<Integer> getCommitLogs(List<ServerInfo> serverInfoList, int serverId) throws IOException, ClassNotFoundException {
		TCPConnection tcpConnection = new TCPConnection(serverInfoList.get(serverId));
		String message = "SERVER:SEND_COMMIT_LOG";
		System.out.println(String.format("Asking for commit logs from server %s %d ....",
				tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort()));
		Utils.writeString(tcpConnection.getObjectOutputStream(), message);
		List<Integer> commitLogs = (List<Integer>)
				tcpConnection.getObjectInputStream().readObject();
		tcpConnection.close();
		return commitLogs;
	}

	public static ConcurrentHashMap<Integer, Article> getDatabase(List<ServerInfo> serverInfoList, int serverId) throws IOException, ClassNotFoundException {
		TCPConnection tcpConnection = new TCPConnection(serverInfoList.get(serverId));
		String message = "SERVER:SEND_DATABASE";
		System.out.println(String.format("Asking for database from server %s %d ....",
				tcpConnection.getSocket().getInetAddress(), tcpConnection.getSocket().getPort()));
		Utils.writeString(tcpConnection.getObjectOutputStream(), message);
		ConcurrentHashMap<Integer, Article> database = (ConcurrentHashMap<Integer, Article>)
				tcpConnection.getObjectInputStream().readObject();
		tcpConnection.close();
		return database;
	}

	// read file and populate server info
	public static List<ServerInfo>  getServerListFromProperties(String name) {
		List<ServerInfo> serverInfoList = new ArrayList<>();
		try {
			File file = new File(name);
			FileInputStream fileInput = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(fileInput);
			fileInput.close();

			Enumeration enuKeys = properties.keys();
			while (enuKeys.hasMoreElements()) {
				String key = (String) enuKeys.nextElement();
				System.out.println(properties.getProperty(key));
				String[] values = properties.getProperty(key).split(":");
				ServerInfo serverInfo = new ServerInfo(values[0], Integer.parseInt(values[1]));
				serverInfoList.add(serverInfo);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serverInfoList;
	}

	public static boolean assertEquals(String a, String b, String successMsg) {
		if (!a.equals(b)) {
			System.out.println(String.format("*********** TEST FAILED: %s is not equal to %s ***********", a, b));
			return false;
		}
		System.out.println(String.format("*********** TEST SUCCESSFUL: %s", successMsg));
		return true;
	}

	public static boolean assertEquals(List<Integer> a, List<Integer> b, String successMsg) {
		if (!a.equals(b)) {
			System.out.println(String.format("*********** TEST FAILED: %s is not equal to %s ***********", a, b));
			return false;
		}
		System.out.println(String.format("*********** TEST SUCCESSFUL: %s", successMsg));
		return true;
	}

}