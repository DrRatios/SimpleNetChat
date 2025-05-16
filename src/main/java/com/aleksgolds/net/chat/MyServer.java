package com.aleksgolds.net.chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Непосредственно сервер
 */

public class MyServer {
    private List<ClientHandler> clients;
    private AuthService authService;
    private static final Logger LOGGER = LogManager.getLogger(MyServer.class);


    public AuthService getAuthService() {
        return authService;
    }

    public MyServer() {
        try (ServerSocket serverSocket = new ServerSocket(ChatConst.PORT)) {
            authService = new DatabaseAuthService();
            authService.start();
            clients = new ArrayList<>();
            while (true) {
                LOGGER.info("Сервер запущен ожидает подключения клиента на порте: " + ChatConst.PORT);
//                System.out.println("Сервер ожидает подключения");
                Socket socket = serverSocket.accept();
                LOGGER.info("Клиент подключился");
//                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }

        } catch (IOException | SQLException ex) {
            LOGGER.info("Произошла ошибка: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public synchronized boolean isNickBusy(String nick) {
        return clients.stream().anyMatch(client -> client.getName().equals(nick));
//        for (ClientHandler client : clients) {
//            if (client.getName().equals(nick)){
//                return true;
//            }
//        }
//        return false;

    }

    public synchronized void changeNick(String oldNick, String newNick) throws SQLException {
        /** где проверка на null??? */

        PreparedStatement st = BaseAuthService.ConnectionToDB.
                connection.prepareStatement("UPDATE Users SET nick = ? WHERE nick = ?");
        st.setString(1, newNick);
        st.setString(2, oldNick);
        st.executeUpdate();
    }

//    public void writeAndReadChatHistory() throws IOException {
//        File chatHistory = new File("chatHistory.txt");
//
//        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(chatHistory, true))) {
//            String chatMessage = message.translateEscapes();
//            dataOutputStream.writeUTF(newMessage + "\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try(DataInputStream dataInputStream = new DataInputStream(new FileInputStream(chatHistory))) {
//            String s = dataInputStream.readUTF();
//            server.broadcastMessage(s);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClients();
    }

    public synchronized void unSubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClients();
    }

    /**
     * broadcastMessage Отправляет сообщение всем пользователям,
     * а также записывает в файл все сообщения (пишет историю чата)
     *
     * @param message
     */
    public synchronized void broadcastMessage(String message) {
        /** Соответственно посылка сообщения всем пользователям */
        for (ClientHandler client : clients) {
            client.sendMsg(message);
        }
    }

    //запись истории всего чата

    public synchronized void writeChatHistory(String message) {
        /** Запись в файл всех сообщений всех пользователей */

        File chatHistory = new File("chatHistory.txt");
        LOGGER.info("Клиент прислал сообщение/команду: " + message);
        try (FileOutputStream fileOutputStream = new FileOutputStream(chatHistory, true);
             Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void writePersonChatHistory(File file, String message) {
        /** Запись в файл  */
        try (FileOutputStream fileOutputStream = new FileOutputStream(file, true);
             Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //считка последних 100 строк истории всего чата
    public synchronized List<String> readFromChatHistoryHundredMessages() throws IOException {
        List<String> hundredMessagesFromChatHistory = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("chatHistory.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                hundredMessagesFromChatHistory.add(line);
                if (hundredMessagesFromChatHistory.size() > 100) {
                    hundredMessagesFromChatHistory.remove(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hundredMessagesFromChatHistory;

    }

    public synchronized void broadcastMessageToClients(String message, List<String> nicknames) {
        clients.stream().filter(c -> nicknames.contains(c.getName()))
                .forEach(c -> c.sendMsg(message));

//        for (ClientHandler client : clients) {
//            if (!nicknames.contains(client.getName())){
//                continue;
//            }
//        client.sendMsg(message)
//        }
    }

    public synchronized void broadcastClients() {
        String clientMessage = ChatConst.CLIENTS_LIST +
                " " +
                clients.stream()
                        .map(ClientHandler::getName)
                        .collect(Collectors.joining(" "));
//      clients.stream().map(c->c.getName()).forEach(n->sb.append(n).append(" "));
        clients.forEach(c -> c.sendMsg(clientMessage));
    }

}


//        String[] parts = message.split("\\s+");
//        String pwNick = getAuthService().getNickByLoginAdPass(parts[1], parts[2]);
//        if (message.startsWith(ChatConst.PRIVATE_MESSAGE){
//
//
//        }
//        for (ClientHandler client : clients) {
//            client.sendMsg(message);
//        }

//    public synchronized void privateMessage(String message) {
//        if
//        clients.forEach(client. -> client.sendMsg(message));
////        for (ClientHandler client : clients) {
////            client.sendMsg(message);
////        }
//    }


