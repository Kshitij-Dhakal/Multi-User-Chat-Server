package chatServer;

import dependencies.Listeners.InterruptListener;
import dependencies.lib.UserBean;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerWorker implements Runnable {
    public static final String LOGIN_SUCCESS = "login success";
    private static final String LOGIN_FAILED_LOGGED_IN = "login failed loggedIn";
    private static final String LOGIN_FAILED_NOT_LOGGED_IN = "login failed notLoggedIn";
    private static final String LOGIN_FAILED_ALREADY_LOGGED_IN = "login failed alreadyLoggedIn";
    private static final String LOGIN_FAILED_NOT_ENOUGH_TOKENS = "login failed notEnoughTokens";

    private static ArrayList<InterruptListener> listeners = new ArrayList<>();
    private boolean isLoggedIn = false;
    private Socket clientSocket;
    private OutputStream outputStream;
    private UserBean bean;

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
            SendReturn send = send(iterator, next, "offline " + sendTo.getBean().getUserHandle());
            System.out.println("ServerWorker : offline" + sendTo.getBean().getUserHandle());
            serverWorkerIterator = send.iterator;
        }
        return serverWorkerIterator;
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
            try {
                System.err.println("ServerWorker : Connection Interrupted for " + this.getBean().getUserHandle());
            } catch (NullPointerException e1) {
                System.err.println("ServerWorker : Connection Interrupted for " + this.getClientSocket().getRemoteSocketAddress());
            }
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
            } else if (tokens[0].equalsIgnoreCase("register")) {
                handleRegister(tokens);
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
                    if (serverWorker.getBean().getUserHandle().equals(username)) {
                        String[] remote = serverWorker.getClientSocket().getRemoteSocketAddress().toString().split(":");
                        String remoteIP = remote[0].substring(1);
                        if (remote[0].substring(1).equalsIgnoreCase("127.0.0.1")) {
                            remoteIP = this.getClientSocket().getLocalAddress().getHostAddress();
                        }
                        String sendBackMessage = "video init " + remoteIP;
                        System.out.println("Receiver's Username : " + serverWorker.getBean().getUserHandle() + " Receiver's IP : " + remoteIP);
                        send(this, sendBackMessage);
//                        System.out.println("ServerWorker 151 : Sending Back " + sendBackMessage);
                        String sendMessage = "video start " + this.getBean().getUserHandle();
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
                    if (serverWorker.getBean().getUserHandle().equals(username)) {
                        String[] remote = serverWorker.getClientSocket().getRemoteSocketAddress().toString().split(":");
                        String remoteIP = remote[0].substring(1);
                        if (remote[0].substring(1).equalsIgnoreCase("127.0.0.1")) {
                            remoteIP = this.getClientSocket().getLocalAddress().getHostAddress();
                        }
                        String sendBackMessage = "video accept " + remoteIP;
                        send(this, sendBackMessage);
//                        System.out.println("ServerWorker 151 : Sending Back " + sendBackMessage);
                        String sendMessage = "video accepted " + this.getBean().getUserHandle();
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
                    if (serverWorker.getBean().getUserHandle().equals(username)) {
                        String sendMessage = "video end " + this.getBean().getUserHandle();
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
                if (serverWorker.getBean().getUserHandle().equals(userHandle)) {
                    SendReturn send = send(serverWorkerIterator, serverWorker, command + this.getBean().getUserHandle() + " " + messageContent);
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
        UserBean login = new UserBean() {{
            setUserHandle(tokens[1]);
            setPassword(tokens[2]);
        }};
        login = UserDao.login(login, ServerConfig.DB_USER, ServerConfig.DB_PASS);
        if (login.isValid()) {
            if (isLoggedIn) {
                send(this, LOGIN_FAILED_LOGGED_IN); //client application is already logged in
            } else if (tokens.length == 3) {
                String message = "online " + login;
                this.setBean(login);
                boolean alreadyLoggedIn = false;
                for (ServerWorker serverWorker : Server.getWorkerArrayList()) {
                    if (serverWorker.getBean().getUserHandle().equals(userHandle)) {
                        alreadyLoggedIn = true;
                    }
                }
                if (alreadyLoggedIn) {
                    send(this, LOGIN_FAILED_ALREADY_LOGGED_IN); //client credential is already logged in
                } else {
                    System.out.println("ServerWorker : Login Success");
                    send(this, LOGIN_SUCCESS + ":" + getBean());
                    Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
                    while (iterator.hasNext()) {
                        ServerWorker serverWorker = iterator.next();
                        send(iterator, this, "online " + serverWorker.getBean());
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

    private void handleRegister(String[] tokens) throws IOException, ClassNotFoundException, SQLException, InvalidKeySpecException, NoSuchAlgorithmException {
        String[] split = tokens[1].split("~");
        UserBean bean = new UserBean() {{
            setFirstName(split[0]);
            setLastName(split[1]);
            setUserHandle(split[2]);
            setPassword(split[3]);
        }};
        bean = UserDao.register(bean, ServerConfig.DB_USER, ServerConfig.DB_PASS);
        if (bean.isValid()) {
            if (isLoggedIn) {
                send(this, LOGIN_FAILED_LOGGED_IN); //client application is already logged in
            } else {
                String message = "online " + bean;
                this.setBean(bean);
                System.out.println("ServerWorker : Login Success");
                send(this, LOGIN_SUCCESS + ":" + getBean());
                Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
                while (iterator.hasNext()) {
                    ServerWorker serverWorker = iterator.next();
                    send(iterator, this, "online " + serverWorker.getBean());
                    send(serverWorker, message);
                }
                Server.getWorkerArrayList().add(this);
                isLoggedIn = true;
            }
        } else {
            System.out.println("Register Failed");
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
            String msg = "offline " + this.getBean().getUserHandle() + "~" + this.getBean().getFirstName()+
            "~" + this.bean.getLastName();
            Iterator<ServerWorker> iterator = Server.getWorkerArrayList().iterator();
            while (iterator.hasNext()) {
                ServerWorker serverWorker = iterator.next();
                send(iterator, serverWorker, msg);
            }
        } else {
            send(this, LOGIN_FAILED_NOT_LOGGED_IN);
        }
    }

    public UserBean getBean() {
        return bean;
    }

    public void setBean(UserBean bean) {
        this.bean = bean;
    }

    private static class SendReturn {
        boolean sent;
        Iterator<ServerWorker> iterator;

        SendReturn(boolean sent, Iterator<ServerWorker> iterator) {
            this.sent = sent;
            this.iterator = iterator;
        }
    }

    public void addInterruptListener(InterruptListener listener) {
        listeners.add(listener);
    }

    public void sendInterrupt(String username) {
        for (InterruptListener listener : listeners) {
            listener.connectionInterrupted(username);
        }
    }
}
