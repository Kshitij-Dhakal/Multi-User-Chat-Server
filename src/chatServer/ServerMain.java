package chatServer;

import dependencies.lib.Config;

import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) throws IOException {
        new Server(Config.PORT);
    }
}
