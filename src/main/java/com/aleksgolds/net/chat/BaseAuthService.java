package com.aleksgolds.net.chat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Простейшая реализвация сервиса аутентификации, которая работает на встроенном списке
 */
public class BaseAuthService implements AuthService {

    private class Entry {
        private final String nick;
        private final String login;
        private final String pass;

        public Entry(String nick, String login, String pass) {
            this.nick = nick;
            this.login = login;
            this.pass = pass;
        }

        public String getNick() {
            return nick;
        }
    }

    public class ConnectionToDB {
        static final String DATABASEURL = "jdbc:sqlite:serverdb.db";
        static Connection connection;
        static Statement statement;

        static {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(DATABASEURL);
                statement = connection.createStatement();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Entry> entries;

    public BaseAuthService() throws SQLException {
//        ConnectionToDB con = new ConnectionToDB();
        String sql = "select * from Users";
        ResultSet resultSet = ConnectionToDB.statement.executeQuery(sql);
        entries = new ArrayList<>();
        while (resultSet.next()) {
            entries.add(new Entry(resultSet.getString("nick"),
                    resultSet.getString("login"),
                    resultSet.getString("pass")));
        }
    }


    @Override
    public void start() {
        System.out.println(this.getClass().getName() + "Server started");

    }

    @Override
    public void stop() {
        System.out.println(this.getClass().getName() + "Server stopped");
    }

    @Override
    public String getNickByLoginAdPass(String login, String pass) {
        return entries.stream().filter(entry -> entry.login.equals(login) && entry.pass.equals(pass))
                .map(entry -> entry.nick)
                .findFirst().orElse(null);
//        for (Entry entry : entries) {
//            if (entry.login.equals(login) && entry.pass.equals(pass)) {
//                return entry.nick;
//            }
//        }
//        return null;
    }
}
