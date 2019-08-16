package chatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

class Server {
    private static ArrayList<ServerWorker> workerArrayList = new ArrayList<>();
    private static int port;

    Server(int port) throws IOException {
        Server.port = port;
        ServerSocket serverSocket = new ServerSocket(Server.port);
        Socket clientListener;
        //Admin
        Thread adminThread = new Thread(() -> {
            String adminCommand;
            Scanner scanner = new Scanner(System.in);
            while ((adminCommand = scanner.nextLine()) != null) {
                String[] adminTokens = adminCommand.split(" ");
                if (adminTokens[0].equalsIgnoreCase("kick")) {
                    handleKick(adminTokens[1]);
                } else if (adminTokens[0].equalsIgnoreCase("ls")) {
                    System.out.println("Online : ");
                    for (ServerWorker serverWorker : getWorkerArrayList()) {
                        System.out.println(serverWorker.getBean().getUserHandle());
                    }

                } else if (adminTokens[0].equalsIgnoreCase("status")) {
                    System.out.println(serverSocket);
                }
            }
            System.err.println("Admin thread terminated!");
        });
        adminThread.start();
        //Admin
        while (true) {
            System.out.println("About to accept new connection.");
            clientListener = serverSocket.accept();
            System.out.println("Connection established at " + clientListener);
            Thread serverWorkerThread = new Thread(new ServerWorker(clientListener));
            serverWorkerThread.start();
        }
    }

    static ArrayList<ServerWorker> getWorkerArrayList() {
        return workerArrayList;
    }

    private void handleKick(String adminToken) {
        Iterator<ServerWorker> serverWorkerIterator = getWorkerArrayList().iterator();
        while (serverWorkerIterator.hasNext()) {
            ServerWorker serverWorker = serverWorkerIterator.next();
            try {
                if (serverWorker.getBean().getUserHandle().equals(adminToken)) {
                    serverWorker.getClientSocket().close();
                    serverWorkerIterator.remove();
                } else {
                    ServerWorker.send(serverWorker, "offline " + adminToken);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
