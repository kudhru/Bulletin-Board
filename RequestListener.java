import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Used by server to listen to incoming requests and call server functions
 */
public class RequestListener extends Thread {
    private Bulletin_Board board;
    private ServerSocket serverSocket = null;
    private int listenerPort;

    public RequestListener(Bulletin_Board bulletinBoard, int listenerPort) {
        this.board = bulletinBoard;
        this.listenerPort = listenerPort;
    }

    @Override
    public void run() {
        // Server port listening to request
        try {
            serverSocket =  new ServerSocket(listenerPort, 100);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            // Start a new thread to handle this request
            while (true) {
                Socket socket = serverSocket.accept();
                
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                
                RequestHandler requestHandler =  new RequestHandler(board , socket , inputStream , outputStream);
                requestHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}