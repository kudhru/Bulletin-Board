import java.util.*;

/**
 * Driver code to start server on shell/machine
 */
public class Server {

    private ConsistencyTypes consistency = ConsistencyTypes.SEQUENTIAL;
    private List <ServerInfo> serverInfoList = new ArrayList<>();
    private Bulletin_Board board;

    public Server(int currentServer, List<ServerInfo> serverInfoList, ConsistencyTypes consistency) {
        int numServers = serverInfoList.size();
        this.consistency = consistency;

        this.serverInfoList = serverInfoList;

        // start server on machine
        board = new Bulletin_Board(consistency , this.serverInfoList,
                0 , currentServer, numServers, 1);
    }

    // Stop server
    private void stop() {
        if (board != null) {
            board.close();
        }
    }

    public static void main(String[] args) {
        // Read from file for servers  #########################################
        List<ServerInfo> serverInfoList =
                Utils.getServerListFromProperties("IP.properties");
        // #####################################################################

        Scanner scanner = new Scanner(System.in);
        ConsistencyTypes consistency = ConsistencyTypes.SEQUENTIAL;
        System.out.println("Enter Consistency Policy \n SEQUENTIAL [S] , QUORUM [Q] , READ_YOUR_WRITE [R] ");
        String policy = scanner.nextLine();
        policy = policy.trim();
        switch (policy.toUpperCase()) {
            case "S" :
                consistency = ConsistencyTypes.SEQUENTIAL;
                break;
            case "Q" :
                consistency = ConsistencyTypes.QUORUM;
                break;
            case "R" :
                consistency = ConsistencyTypes.READ_YOUR_WRITE;
                break;
            default:
                consistency = ConsistencyTypes.SEQUENTIAL;
                break;
        }
        System.out.println("Consistency Policy = " + consistency.name());

        // Initialing and starting server
        Server server = new Server(Integer.parseInt(args[0]), serverInfoList, consistency);

        // check if needs to be stopped
        while(true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("Stop")) {
                break;
            }
        }

        server.stop();

        System.exit(0);
        return;
    }
}