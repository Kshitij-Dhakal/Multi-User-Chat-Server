package chatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class Server {
    static ArrayList<ServerWorker> workerArrayList = new ArrayList<>();
    private static int port;

    public Server(int port) throws IOException {
        Server.port = port;
        ServerSocket serverSocket = new ServerSocket(Server.port);
        Socket clientListener;
        //Admin
        Thread adminThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String adminCommand;
                Scanner scanner = new Scanner(System.in);
                while ((adminCommand = scanner.nextLine()) != null) {
                    String[] adminTokens = adminCommand.split(" ");
                    if (adminTokens[0].equalsIgnoreCase("kick")) {
                        Iterator<ServerWorker> serverWorkerIterator = getWorkerArrayList().iterator();
                        while (serverWorkerIterator.hasNext()) {
                            ServerWorker serverWorker = serverWorkerIterator.next();
                            try {
                                if (serverWorker.getUserHandle().equals(adminTokens[1])) {
                                    serverWorker.getClientSocket().close();
                                    serverWorkerIterator.remove();
                                } else {
                                    ServerWorker.send(serverWorker, "offline " + adminTokens[1]);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                System.err.println("Admin thread terminated!");
            }
        });
        adminThread.start();
        //Admin
        while (true) {
            System.out.println("About to accept new connection.");
            clientListener = serverSocket.accept();
            System.out.println("Connection established at " + clientListener);
            Thread serverWorkerThread = new Thread(new ServerWorker(this, clientListener));
            serverWorkerThread.start();
        }
    }

    public static ArrayList<ServerWorker> getWorkerArrayList() {
        return workerArrayList;
    }

    public void setWorkerArrayList(ArrayList<ServerWorker> workerArrayList) {
        Server.workerArrayList = workerArrayList;
    }
}
