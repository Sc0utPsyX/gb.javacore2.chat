package ru.gb.may_chat.server;

import ru.gb.may_chat.constants.MessageConstants;
import ru.gb.may_chat.enums.Command;
import ru.gb.may_chat.server.error.WrongCredentialsException;
import ru.gb.may_chat.server.model.DatabaseHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.*;

public class Handler {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private String user;

    public Handler(Socket socket, Server server) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            Server.LOGGER.info("Handler created");
        } catch (IOException e) {
            Server.LOGGER.warning("Connection problems with user: " + user);
        }
    }

    public void handle() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.execute(() -> {
            authorize();
            Server.LOGGER.info("Auth done");
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                try {
                    String message = in.readUTF();
                    parseMessage(message);
                } catch (IOException e) {
                    server.removeHandler(Handler.this);
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCommand(split[0]);

        switch (command) {
            case BROADCAST_MESSAGE -> {
                server.broadcast(user, split[1]);
                Server.LOGGER.info("Some Message sent."); // в задании было условие клиент прислал сообщение
            }
            case PRIVATE_MESSAGE -> {
                server.broadcast(user, split[2], split[1]);
                Server.LOGGER.info("Some Private Message sent."); // в задании было условие клиент прислал сообщение
            }
            case CHANGE_NICK -> {
                server.broadcast(user, new String("changed nickname to " + split[1]));
                Server.LOGGER.info(new String(user + " changed nickname to " + split[1]));
                user = server.getUserService().changeNick(user, split[1]);
                server.updateHandlers(this);
                break;
            }
            default -> Server.LOGGER.info("Unknown message " + message);

        }
    }

    private void authorize() {
        Server.LOGGER.info("Authorizing");
        Thread socketTimeout = new Thread(() -> {
            try {
                Thread.sleep(20000);
                socket.close();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        socketTimeout.start();
        try {
            while (!socket.isClosed()) {
                String msg = in.readUTF();
                if (msg.startsWith(AUTH_MESSAGE.getCommand())) {
                    String[] parsed = msg.split(REGEX);
                    String response = "";
                    String nickname = null;
                    try {
                        nickname = server.getUserService().authenticate(parsed[1], parsed[2]);
                    } catch (WrongCredentialsException e) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + e.getMessage();
                        Server.LOGGER.warning("Wrong credentials: " + parsed[1]);
                    }
                    
                    if (server.isUserAlreadyOnline(nickname)) {
                        response = ERROR_MESSAGE.getCommand() + REGEX + "This client already connected";
                        Server.LOGGER.warning("Already connected");
                    }
                    
                    if (!response.equals("")) {
                        send(response);
                    } else {
                        Server.LOGGER.info("Auth ok");
                        this.user = nickname;
                        send(AUTH_OK.getCommand() + REGEX + nickname);
                        server.addHandler(this);
                        socketTimeout.interrupt();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUser() {
        return user;
    }
}
