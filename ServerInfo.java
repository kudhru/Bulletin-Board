/**
 *      Class to hold information about a server
 */
public class ServerInfo {
    private String IP;
    private int port;

    ServerInfo(String IP , int port) {
        this.IP = IP;
        this.port = port;
    }

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof ServerInfo)) {
            return false;
        }

        ServerInfo client = (ServerInfo) o;

        return client.IP.equals(IP) &&
                client.port == port;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + IP.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "ServerInfo :: IP : " + this.IP + " -- PORT : " + this.port;
    }

    public static String toString(String IP , int port) {
        return "ServerInfo :: IP : " + IP + " -- PORT : " + port;
    }
}
