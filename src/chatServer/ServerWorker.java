package chatServer;

import java.io.*;
import java.net.Socket;
import java.util.Iterator;

public class ServerWorker implements Runnable {
    public static final String LOGIN_SUCCESS = "login success";
    public static final String LOGIN_FAILED_LOGGED_IN = "login failed loggedIn";
    public static final String LOGIN_FAILED_NOT_LOGGED_IN = "login failed notLoggedIn";
    public static final String LOGIN_FAILED_ALREADY_LOGGED_IN = "login failed alreadyLoggedIn";
    public static final String LOGIN_FAILED_NOT_ENOUGH_TOKENS = "login failed notEnoughTokens";

    private boolean isLoggedIn = false;
    private Server server;
    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String userHandle;

    public ServerWorker(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    public static boolean send(ServerWorker sendTo, String message) throws IOException {
        try {
            sendTo.getOutputStream().write((message + "\n").getBytes());
            return true;
        } catch (IOException e) {
            Server.getWorkerArrayList().remove(sendTo);
            sendTo.getClientSocket().close();
            Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
            sendAll(iterator, sendTo, iterator);
        }
        return false;
    }

    public static SendReturn send(Iterator<ServerWorker> serverWorkerIterator, ServerWorker sendTo, String message) throws IOException {
        SendReturn sendReturn = new SendReturn(false, serverWorkerIterator);
        try {
            sendTo.getOutputStream().write((message + "\n").getBytes());
            sendReturn.sent = true;
        } catch (IOException e) {
            serverWorkerIterator.remove();
            sendTo.getClientSocket().close();
            Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
            serverWorkerIterator = sendAll((Iterator<ServerWorker>) serverWorkerIterator, sendTo, (Iterator<ServerWorker>) iterator);
            sendReturn.iterator = serverWorkerIterator;
        }
        return sendReturn;
    }

    private static Iterator<ServerWorker> sendAll(Iterator<ServerWorker> serverWorkerIterator, ServerWorker sendTo, Iterator<ServerWorker> iterator) throws IOException {
        while (iterator.hasNext()) {
            ServerWorker next = iterator.next();
            SendReturn send = send(iterator, next, "offline " + sendTo.getUserHandle());
            serverWorkerIterator = send.iterator;
        }
        return serverWorkerIterator;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            Server.getWorkerArrayList().remove(this);
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
        if (isLoggedIn) {
            send(this, LOGIN_FAILED_LOGGED_IN);
        } else if (tokens.length == 2) {
            String userHandle = tokens[1];
            String message = "online " + userHandle;
            this.setUserHandle(userHandle);
            boolean alreadyLoggedIn = false;
            for (ServerWorker serverWorker : Server.getWorkerArrayList()) {
                if (serverWorker.getUserHandle().equals(userHandle)) {
                    alreadyLoggedIn = true;
                }
            }
            if (alreadyLoggedIn) {
                send(this, LOGIN_FAILED_ALREADY_LOGGED_IN);
            } else {
                send(this, LOGIN_SUCCESS);
                Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
                while (iterator.hasNext()) {
                    ServerWorker serverWorker = iterator.next();
                    send(iterator, this, "online " + serverWorker.getUserHandle());
                    send(serverWorker, message);
                }
                Server.getWorkerArrayList().add(this);
                isLoggedIn = true;
            }
        } else {
            send(this, LOGIN_FAILED_NOT_ENOUGH_TOKENS);
        }
    }

    private void handleMessage(String line) throws IOException {
        boolean sent = false;
        String[] tokens = line.split(" ", 3);
        if (tokens.length == 3) {
            String userHandle = tokens[1];
            String messageContent = tokens[2];
            Iterator<ServerWorker> serverWorkerIterator = Server.getWorkerArrayList().iterator();
            while (serverWorkerIterator.hasNext()) {
                ServerWorker serverWorker = serverWorkerIterator.next();
                if (serverWorker.getUserHandle().equals(userHandle)) {
                    SendReturn send = send(serverWorkerIterator, serverWorker, "message " + this.userHandle + " " + messageContent);
                    sent = send.sent;
                    serverWorkerIterator = send.iterator;
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
        if (isLoggedIn) {
            Server.getWorkerArrayList().remove(this);
            this.getClientSocket().close();
            isLoggedIn = false;
            String msg = "offline " + this.userHandle;
            Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
            while (iterator.hasNext()) {
                ServerWorker serverWorker = iterator.next();
                send(iterator, serverWorker, msg);
            }
        } else {
            send(this, LOGIN_FAILED_NOT_LOGGED_IN);
        }
    }

    private static class SendReturn {
        public boolean sent;
        public Iterator<ServerWorker> iterator;

        public SendReturn(boolean sent, Iterator<ServerWorker> iterator) {
            this.sent = sent;
            this.iterator = iterator;
        }
    }
}
