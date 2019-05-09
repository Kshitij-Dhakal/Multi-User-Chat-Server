package chatServer;

import java.io.*;
import java.net.Socket;

public class ServerWorker implements Runnable {
    private boolean isLoggedIn;
    private Server server;
    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String userHandle;

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void setClientSocket(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientSocket() throws IOException {
        this.inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String lines;
        while ((lines = bufferedReader.readLine()) != null) {
            String tokens[] = lines.split(" ");
            if (tokens[0].equalsIgnoreCase("msg")) {
                handleMessage(lines);
            } else if (tokens[0].equalsIgnoreCase("login")) {
                handleLogin(tokens);
            } else if (tokens[0].equalsIgnoreCase("exit")) {
                handleLogOff();
                break;
            }
        }
    }

    public static void send(ServerWorker sendTo, String message) throws IOException {
        sendTo.getOutputStream().write((message + "\n").getBytes());
    }

    private void handleLogin(String[] tokens) throws IOException {
        System.out.println("Handling Logins");
        if (tokens.length == 2) {
            String userHandle = tokens[1];
            String message = userHandle + " is online";
            for (ServerWorker serverWorker : this.server.getWorkerArrayList()) {
                send(serverWorker, message);
            }
            this.setUserHandle(userHandle);
            this.server.getWorkerArrayList().add(this);
        } else {
            System.out.println("Failed to Login");
        }
    }

    private void handleMessage(String line) throws IOException {
        String[] tokens = line.split(" ", 3);
        if (tokens.length == 3) {
            String userHandle = tokens[1];
            String messageContent = tokens[2];
            for (ServerWorker serverWorker : this.server.getWorkerArrayList()) {
                if (serverWorker.getUserHandle().equals(userHandle)) {
                    send(serverWorker, this.userHandle + " sent : " + messageContent);

                }
            }
        } else {
            System.out.println("Failed to send message");
        }
    }

    private void handleLogOff() throws IOException {
        this.server.getWorkerArrayList().remove(this);
        String msg = this.userHandle + " went offline.";
        for (ServerWorker serverWorker : this.server.getWorkerArrayList()) {
            send(serverWorker, msg);
        }
    }
}
