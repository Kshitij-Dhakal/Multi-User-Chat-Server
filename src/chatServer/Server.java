package chatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    ArrayList<ServerWorker> workerArrayList = new ArrayList<>();
    private static int port;

    public Server(int port) throws IOException {
        this.port = port;
        ServerSocket serverSocket = new ServerSocket(this.port);
        Socket clientListener;
        while (true) {
            System.out.println("About to accept new connection.");
            clientListener = serverSocket.accept();
            System.out.println("Connection established at "+clientListener);
            ServerWorker serverWorker = new ServerWorker(this, clientListener);
            Thread thread = new Thread(serverWorker);
            thread.start();
        }
    }

    public ArrayList<ServerWorker> getWorkerArrayList() {
        return workerArrayList;
    }

    public void setWorkerArrayList(ArrayList<ServerWorker> workerArrayList) {
        this.workerArrayList = workerArrayList;
    }
}
