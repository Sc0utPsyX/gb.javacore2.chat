package ru.gb.may_chat.server;

import ru.gb.may_chat.server.model.User;
import ru.gb.may_chat.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static ru.gb.may_chat.constants.MessageConstants.REGEX;
import static ru.gb.may_chat.enums.Command.*;

public class Server {
    public static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static final int PORT = 8189;
    private List<Handler> handlers;

    private UserService userService;

    public Server(UserService userService) {
        this.userService = userService;
        this.handlers = new ArrayList<>();
    }

    public void start() {
        LOGGER.setLevel(Level.ALL);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server start!");
            userService.start();
            while (true) {
                LOGGER.info("Waiting for connection......");
                Socket socket = serverSocket.accept();
                LOGGER.info("Client connected");
                Handler handler = new Handler(socket, this);
                handler.handle();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    public void broadcast(String from, String message) {
        String msg = BROADCAST_MESSAGE.getCommand() + REGEX + String.format("[%s]: %s", from, message);
        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }

    public void broadcast(String from, String message, String whom) {
        String msg = PRIVATE_MESSAGE.getCommand() + REGEX + String.format("[%s] to [%s]: %s", from, whom ,message);
        for (Handler handler : handlers) {
            if (handler.getUser().equals(whom) || handler.getUser().equals(from)){
                handler.send(msg);
            }
        }
    }

    public UserService getUserService() {
        return userService;
    }
    
    public synchronized boolean isUserAlreadyOnline(String nick) {
        for (Handler handler : handlers) {
            if (handler.getUser().equals(nick)) {
                return true;
            }
        }
        return false;
    }
    
    public synchronized void addHandler(Handler handler) {
        this.handlers.add(handler);
        sendContacts();
    }

    public synchronized void removeHandler(Handler handler) {
        this.handlers.remove(handler);
        sendContacts();
    }

    public synchronized void updateHandlers(Handler handler){
        sendContacts();
    }

    private void shutdown() {
        userService.stop();
    }

    private void sendContacts() {
       String contacts = handlers.stream()
                .map(Handler::getUser)
                .collect(Collectors.joining(REGEX));
       String msg = LIST_USERS.getCommand() + REGEX + contacts;

        for (Handler handler : handlers) {
            handler.send(msg);
        }
    }
}
