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

    public static void send(ServerWorker sendTo, String message) throws IOException {
        sendTo.getOutputStream().write((message + "\n").getBytes());
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
            System.err.println("Connection Interrupted for " + this.getUserHandle());
        }
    }

    private void handleClientSocket() throws IOException {
        this.inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String lines;
        while ((lines = bufferedReader.readLine()) != null) {
            String[] tokens = lines.split(" ");

            if (tokens[0].equalsIgnoreCase("msg")) {
                handleMessage(lines);
            } else if (tokens[0].equalsIgnoreCase("login")) {
                handleLogin(tokens);
            } else if (tokens[0].equalsIgnoreCase("exit")) {
                handleLogOff();
                break;
            }

        }
        clientSocket.close();
    }

    private void handleLogin(String[] tokens) throws IOException {
        if (tokens.length == 2) {
            String userHandle = tokens[1];
            String message = "online " + userHandle;
            for (ServerWorker serverWorker : Server.getWorkerArrayList()) {
                send(serverWorker, message);
            }
            this.setUserHandle(userHandle);
            Server.getWorkerArrayList().add(this);
        } else {
            System.out.println("Failed to Login");
        }
    }

    private void handleMessage(String line) throws IOException {
        boolean sent = false;
        String[] tokens = line.split(" ", 3);
        if (tokens.length == 3) {
            String userHandle = tokens[1];
            String messageContent = tokens[2];
            for (ServerWorker serverWorker : Server.getWorkerArrayList()) {
                if (serverWorker.getUserHandle().equals(userHandle)) {
                    send(serverWorker, "message " + this.userHandle + " " + messageContent);
                    sent = true;
                }
            }
        }
        if (sent) {
            send(this, "Send Success");
        } else {
            send(this, "Send Failed");
        }
    }

    private void handleLogOff() throws IOException {
        Server.getWorkerArrayList().remove(this);
        String msg = "offline " + this.userHandle;
        for (ServerWorker serverWorker : Server.getWorkerArrayList()) {
            send(serverWorker, msg);
        }
    }
}
