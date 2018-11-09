import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * 	Driver code to switch communication and control flow
 *
 *
 * 	Implements system communication protocol
 *
 */
public class RequestHandler extends Thread {
	private Bulletin_Board board;
	private final ObjectInputStream inputStream;
	private final ObjectOutputStream outputStream;
	final Socket socket;
	private Logger logger = Logger.getLogger(RequestHandler.class.getName());


	public RequestHandler(Bulletin_Board board, Socket socket, ObjectInputStream inputStream,
			ObjectOutputStream outputStream) {
		this.board = board;
		this.socket = socket;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	@Override
	public void run() {
		String received = "";
		int numRetry = 0;
		while (true) {
			try {
				// read from input stream
				received = inputStream.readUTF();
				System.out.println(String.format("Request Received ## %s", received));
				numRetry = 0;

				if (received.equalsIgnoreCase("Client:Exit")
						|| received.equalsIgnoreCase("Server:Exit")) {
					break;
				}

				String[] action = received.split(":");

				// Handle all server communication
				if (action[0].equalsIgnoreCase("Server")) {
					String temp = action[1];
					switch (temp.toUpperCase()) {
					// Takes care of broadcast and local updates
					case "UPDATE":
						board.handleServerUpdate(action);
						Utils.writeStringWithRandomDelay(outputStream,
								"SERVER:ACK:" + action[2] + ":" + action[3]);
						break;

					case "GET_NEXT_ID":
						if (board.isPrimary()) {
							int id = board.getNextID();
							Utils.writeStringWithRandomDelay(outputStream, "SERVER:NEXT_ID:" + id);
						}
						break;
						
					case "SEND_MAX_ID":
						Utils.writeStringWithRandomDelay(outputStream,
								"SERVER:MAX_ID:" + board.getMaxArticleID());
						break;

					case "PRINT_DB":
							System.out.println(
									"SERVER SIDE DB DISPLAY " + board.getServerNum() + "\n" + board.displayBoard());
							break;

					case "SEND_DATABASE":
						board.sendDatabase(outputStream);
						break;

					case "SEND_COMMIT_LOG":
						board.sendCommitLog(outputStream);
						break;

					case "LOCAL_UPDATE":
						int article_id = board.updateDatabaseLocally(action);
						board.setMaxArticleID(article_id);
						Utils.writeStringWithRandomDelay(outputStream,
								"SERVER:ACK:" + action[2] + ":" + action[3]);
						break;
						
					case "GET_ARTICLE":
						String content = board.getArticle(Integer.parseInt(action[2]));
						Utils.writeStringWithRandomDelay(outputStream, content);
						break;

					default:
						Utils.writeString(outputStream, "Invalid input");
						break;
					}
				}
				// Handle all client communication
				else if (action[0].equalsIgnoreCase("Client")) {
					String temp = action[1];
					int new_id = CONSTANTS.NEG_ID;

					switch (temp.toUpperCase()) {
						case "POST":
						case "REPLY":
							// "CLIENT:POST:CONTENT"
							new_id = board.handleClientUpdate(action);
							board.setMaxArticleID(new_id);
							Utils.writeStringWithRandomDelay(outputStream, temp.toUpperCase() + ":" + new_id);
							break;

						case "READ":
							// "CLIENT:READ"
							String board_contents = board.read();
							Utils.writeStringWithRandomDelay(outputStream, board_contents);
							break;

						case "CHOOSE":
							// "CLIENT:CHOOSE:ARTICLE_ID"
							String contents = board.choose(Integer.parseInt(action[2]));
							Utils.writeStringWithRandomDelay(outputStream,
									"CHOOSE:" + action[2] + ":" + contents);
							break;

						default:
							Utils.writeString(outputStream, "Invalid input");
							break;
					}
				} else {
					Utils.writeString(outputStream, "Invalid input");
				}
			} catch (IOException e) {
				// retry connections
				numRetry++;
				if (numRetry == CONSTANTS.MAX_RETRY){
					break;
				}
				try {
					sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		// close connections
		try {
			this.inputStream.close();
			this.outputStream.close();
			this.socket.close();
		} catch (IOException e) { }
	}
}