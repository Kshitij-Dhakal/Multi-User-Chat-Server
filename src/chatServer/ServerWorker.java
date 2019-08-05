package chatServer;

import dependencies.lib.UserBean;
import dependencies.lib.UserDao;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Iterator;

public class ServerWorker implements Runnable {
    public static final String LOGIN_SUCCESS = "login success";
    private static final String LOGIN_FAILED_LOGGED_IN = "login failed loggedIn";
    private static final String LOGIN_FAILED_NOT_LOGGED_IN = "login failed notLoggedIn";
    private static final String LOGIN_FAILED_ALREADY_LOGGED_IN = "login failed alreadyLoggedIn";
    private static final String LOGIN_FAILED_NOT_ENOUGH_TOKENS = "login failed notEnoughTokens";

    private boolean isLoggedIn = false;
    private Socket clientSocket;
    private OutputStream outputStream;
    private String userHandle;

    ServerWorker(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    static boolean send(ServerWorker sendTo, String message) throws IOException {
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

    private static SendReturn send(Iterator<ServerWorker> serverWorkerIterator, ServerWorker sendTo, String message) throws IOException {
        SendReturn sendReturn = new SendReturn(false, serverWorkerIterator);
        try {
            sendTo.getOutputStream().write((message + "\n").getBytes());
            sendReturn.sent = true;
        } catch (IOException e) {
            serverWorkerIterator.remove();
            sendTo.getClientSocket().close();
            Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
            serverWorkerIterator = sendAll(serverWorkerIterator, sendTo, iterator);
            sendReturn.iterator = serverWorkerIterator;
        }
        return sendReturn;
    }

    private static Iterator<ServerWorker> sendAll(Iterator<ServerWorker> serverWorkerIterator, ServerWorker sendTo, Iterator<ServerWorker> iterator) throws IOException {
        while (iterator.hasNext()) {
            ServerWorker next = iterator.next();
            SendReturn send = send(iterator, next, "offline " + sendTo.getUserHandle());
            System.out.println("ServerWorker : offline" + sendTo.getUserHandle());
            serverWorkerIterator = send.iterator;
        }
        return serverWorkerIterator;
    }

    public String getUserHandle() {
        return userHandle;
    }

    private void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    Socket getClientSocket() {
        return clientSocket;
    }

    private OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void run() {
        try {
            handleClientSocket();
        } catch (IOException e) {
            System.err.println("ServerWorker : Connection Interrupted for " + this.getUserHandle());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                handleLogOff();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClientSocket() throws IOException, ClassNotFoundException, SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        InputStream inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String lines;
        while ((lines = bufferedReader.readLine()) != null) {
            String[] tokens = lines.split(" ");
            if (tokens[0].equalsIgnoreCase("send")) {
                handleMessage(lines);
            } else if (tokens[0].equalsIgnoreCase("login")) {
                handleLogin(tokens);
            } else if (tokens[0].equalsIgnoreCase("key")) {
                handleKey(lines);
            } else if (tokens[0].equalsIgnoreCase("video")) {
                handleVideo(tokens);
            } else if (tokens[0].equalsIgnoreCase("exit")) {
                break;
            }
        }
    }

    private void handleVideo(String[] tokens) throws IOException {
        //TODO handlevideo
        /**
         * if command == video start receiver_username
         *      send back video init receiver_url
         *      (sender starts video server destined at receiver_url and port 42070)
         *      send video start to receiver
         *      (receiver listens to port 42070 for incoming calls)
         *if receiver accepts call
         *      receiver sends video accept sender_username
         *      send video accept sender_url so that receiver can send his video to receiver
         *      send video accept receiver to sender so sender can listen for incoming call at port 42071
         *else if receiver rejects call
         *      send video reject sender_username receiver_url
         *when call ends
         *      send video stop username -1
         */
        if (tokens.length == 3) {
            String command = tokens[1];
            if (command.equalsIgnoreCase("start")) {
                String username = tokens[2];
                Iterator<ServerWorker> serverWorkerIterator = Server.getWorkerArrayList().iterator();
                while (serverWorkerIterator.hasNext()) {
                    ServerWorker serverWorker = serverWorkerIterator.next();
                    if (serverWorker.getUserHandle().equals(username)) {
                        String sendBackMessage = "video init " + serverWorker.clientSocket.getInetAddress().getHostAddress();
                        send(this, sendBackMessage);
//                        System.out.println("ServerWorker 151 : Sending Back " + sendBackMessage);
                        String sendMessage = "video start " + this.getUserHandle();
//                        System.out.println("ServerWorker 153 : Sending " + sendMessage);
                        send(serverWorkerIterator, serverWorker, sendMessage);
//                        sent = send.sent;
                    }
                }
            } else if (command.equalsIgnoreCase("accept")) {
                String username = tokens[2];
                Iterator<ServerWorker> serverWorkerIterator = Server.getWorkerArrayList().iterator();
                while (serverWorkerIterator.hasNext()) {
                    ServerWorker serverWorker = serverWorkerIterator.next();
                    if (serverWorker.getUserHandle().equals(username)) {
                        String sendBackMessage = "video accept " + serverWorker.clientSocket.getInetAddress().getHostAddress();
                        send(this, sendBackMessage);
//                        System.out.println("ServerWorker 151 : Sending Back " + sendBackMessage);
                        String sendMessage = "video accepted " + this.getUserHandle();
//                        System.out.println("ServerWorker 153 : Sending " + sendMessage);
                        send(serverWorkerIterator, serverWorker, sendMessage);
//                        sent = send.sent;
                    }
                }
            } else if (command.equalsIgnoreCase("end")) {
                String username = tokens[2];
                Iterator<ServerWorker> serverWorkerIterator = Server.getWorkerArrayList().iterator();
                while (serverWorkerIterator.hasNext()) {
                    ServerWorker serverWorker = serverWorkerIterator.next();
                    if (serverWorker.getUserHandle().equals(username)) {
                        String sendMessage = "video end " + this.getUserHandle();
//                        System.out.println("ServerWorker 153 : Sending " + sendMessage);
                        send(serverWorkerIterator, serverWorker, sendMessage);
//                        sent = send.sent;
                    }
                }
            }
        }
    }

    private void handleKey(String lines) throws IOException {
        //key @username init p g Xa
        //key @username reply Xb
        String command = "key ";
        sendUserCommand(lines, command, "key sent");
    }

    private void sendUserCommand(String lines, String command, String replyMessage) throws IOException {
        boolean sent = false;
        String[] tokens = lines.split(" ", 3);
        if (tokens.length == 3) {
            String userHandle = tokens[1];
            String messageContent = tokens[2];
            Iterator<ServerWorker> serverWorkerIterator = Server.getWorkerArrayList().iterator();
            while (serverWorkerIterator.hasNext()) {
                ServerWorker serverWorker = serverWorkerIterator.next();
                if (serverWorker.getUserHandle().equals(userHandle)) {
                    SendReturn send = send(serverWorkerIterator, serverWorker, command + this.userHandle + " " + messageContent);
                    sent = send.sent;
                    serverWorkerIterator = send.iterator;
                }
            }
        }
        if (sent) {
            send(this, replyMessage + " success");
        } else {
            send(this, replyMessage + " failed");
        }
    }

    private void handleLogin(String[] tokens) throws IOException, ClassNotFoundException, SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        String userHandle = tokens[1];
        String password = tokens[2];
        UserBean login = UserDao.login(userHandle, password, DbConfig.DB_USERNAME, DbConfig.DB_PASSWORD);
        if (login.isValid()) {
            if (isLoggedIn) {
                send(this, LOGIN_FAILED_LOGGED_IN);
            } else if (tokens.length == 3) {
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
                    System.out.println("ServerWorker : Login Success");
                    send(this, LOGIN_SUCCESS + ":" + userHandle);
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
    }

    private void handleMessage(String line) throws IOException {
        String command = "message ";
        sendUserCommand(line, command, "message sent");
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
        boolean sent;
        Iterator<ServerWorker> iterator;

        SendReturn(boolean sent, Iterator<ServerWorker> iterator) {
            this.sent = sent;
            this.iterator = iterator;
        }
    }
}
