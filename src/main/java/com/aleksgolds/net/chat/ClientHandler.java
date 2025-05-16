package com.aleksgolds.net.chat;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Класс обслуживания клиента (отвечает за связь между клиентом и сервером)
 */
public class ClientHandler implements Serializable {

    private MyServer server;
    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    private String name;

    public String getName() {
        return this.name;
    }

    public ClientHandler(MyServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.name = "";
            ExecutorService executorService = Executors.newCachedThreadPool();
            executorService.execute(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                    executorService.shutdown();
                }
            });
//            new Thread(() -> {
//                try {
//                    authentication();
//                    readMessages();
//                } catch (IOException | SQLException e) {
//                    e.printStackTrace();
//                } finally {
//                    closeConnection();
//                }
//            }).start();
        } catch (IOException ex) {
            System.out.println("Проблема при создании клиента");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String message = inputStream.readUTF();
            if (message.startsWith(ChatConst.AUTH_COMMAND)) {
                String[] parts = message.split("\\s+");
                String nick = server.getAuthService().getNickByLoginAdPass(parts[1], parts[2]);
                if (nick != null) {
                    //Проверим, что такого нет!
                    if (!server.isNickBusy(nick)) {
                        sendMsg(ChatConst.AUTH_OK + " " + nick);
                        name = nick;
                        server.subscribe(this);
                        server.broadcastMessage(name + " вошёл в чат");
                        server.readFromChatHistoryHundredMessages().forEach(this::sendMsg); //считка и посылка в чат последних 100 строк чата
///auth login1 pass1
                        return;
                    } else {
                        sendMsg("Ник уже используется");
                    }

                } else {
                    sendMsg("Неверный логин/пароль");
                }

            }
        }
    }

    public void sendMsg(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessages() throws IOException, SQLException {
        while (true) {
            String messageFromClient = inputStream.readUTF();

            server.writeChatHistory("[" + name + "]: " + messageFromClient); //запись истории всего чата
            server.writePersonChatHistory(new File("history_" + name + ".txt"), "от " + name + ": " + messageFromClient); //запись истории чата юзера

            System.out.println("от " + name + ": " + messageFromClient);
            if (messageFromClient.equals(ChatConst.STOP_WORD)) {
                return;
            }
            if (messageFromClient.startsWith(ChatConst.PRIVATE_MESSAGE)) {
                String[] splStr = messageFromClient.split("\\s+");
                List<String> nicknames = new ArrayList<>();
                for (int i = 1; i < splStr.length - 1; i++) {
                    nicknames.add(splStr[i]);
                }
                server.broadcastMessageToClients("[" + name + "]: " + messageFromClient, nicknames);
            }
            if (messageFromClient.equals(ChatConst.CLIENTS_LIST)) {
                server.broadcastClients();
            }
            if (messageFromClient.startsWith(ChatConst.CHANGE_NICK)) {
                String[] splStr = messageFromClient.split("\\s+");
                if (name.equals(splStr[1]) && !server.isNickBusy(splStr[2])) {
                    server.changeNick(splStr[1], splStr[2]);
                    server.broadcastMessage("[" + name + "]: сменил свой ник на " + splStr[2]);
                    name = splStr[2];
                }
                if (splStr[2] == null) {
                    server.broadcastMessage("[" + name + "]: попытался сменить свой ник на пустой");
                }
                server.broadcastMessage("[" + name + "]: попытался сменить чужой ник");
            }
            server.broadcastMessage("[" + name + "]: " + messageFromClient);
        }

//                    if (messageFromClient.startsWith(ChatConst.PRIVATE_MESSAGE)) {
//                        String to = messageFromClient.split(" ")[1];
//                        String message = messageFromClient.split(" ")[2];
//                        server.privateMessage(this, to, message);
//                    } else {
//                        server.broadcastMessage("[" + name + "]: " + messageFromClient);
//                    }
    }

    public void closeConnection() {
        server.unSubscribe(this);
        server.broadcastMessage(name + " вышел с чата");
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}







