package com.aleksgolds.net.chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class EchoClient extends JFrame {

    private JTextArea chatArea;
    private JTextField inputField;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Socket socket;

    public static void main(String[] args) {
        new EchoClient();
    }

    public EchoClient(){
        try {
            openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initGUI();
    }
    private void openConnection() throws IOException {
        socket = new Socket(EchoConstans.HOST, EchoConstans.PORT);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                while (true){
                String strFromServer = inputStream.readUTF();
                if (strFromServer.equals(EchoConstans.STOP_WORD)){
                    break;
                }
                chatArea.append(strFromServer);
                chatArea.append("\n");
            }
            } catch (IOException ex){
                ex.printStackTrace();
            }
        }).start();
    }
    private  void closeConnection(){
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void initGUI(){
        setBounds(600,300,500,500);
        setTitle("Client");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // Message Area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        //Down Panel
        JPanel panel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        panel.add(inputField, BorderLayout.CENTER);
        JButton sendButton = new JButton("Send");
        add(panel, BorderLayout.SOUTH);
        panel.add(sendButton,BorderLayout.EAST);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    outputStream.writeUTF(EchoConstans.STOP_WORD);
                    closeConnection();

                } catch (IOException ex){
                    ex.printStackTrace();
                }
            }
        });


        setVisible(true);
    }

    public void sendMessage(){
        if (!inputField.getText().trim().isEmpty()){
            try {
                outputStream.writeUTF(inputField.getText());
                inputField.setText("");
                inputField.grabFocus();
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,"Send error occurred");
            }
        }

    }

}
