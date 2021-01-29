package chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleChat implements ISimpleChat {

    Socket socket;
    PrintWriter printerWRiter;
    BufferedReader reader;
    volatile Boolean isOn = true;

    public static void main(String[] args) throws IOException, ChatException {
        try {
            if (args.length == 0) {
                System.out.println("Введите параметр для запуска");
            } else if (args[0].equals("-client")) {
                SimpleChat chat = new SimpleChat();
                chat.client();
            } else if (args[0].equals("-server")) {
                new SimpleChat().server();
            }
        } catch (Exception exp) {
            throw new ChatException(exp);
        }
    }

    @Override
    public void client() throws ChatException, IOException {
//        String address = sc.nextLine();
        String address = "localhost";
        this.socket = new Socket(address, ISimpleChat.SERVER_PORT);

        this.printerWRiter = new PrintWriter(socket.getOutputStream());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        Runnable processReader = new Runnable() {
            @Override
            public void run() {
                String fromUser;
                while(isOn) {
                    try {
                        if ((fromUser = getMessage()) != null) {
                            System.out.println("Server: " + fromUser);
                        }
                    } catch (ChatException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(processReader).start();
        String toUser;
        while (isOn) {
            try {
                if ((toUser = stdIn.readLine()) != null) {
                    try {
                        sendMessage(toUser);

                        if (toUser.equals("Bye")) {
                            close();
                        }
                    } catch (ChatException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void server() throws ChatException, IOException {

        try (ServerSocket service = new ServerSocket(SERVER_PORT);) {
            this.socket = service.accept();
            System.out.println("Успешное подключение");
            this.printerWRiter = new PrintWriter(socket.getOutputStream());
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            Runnable processReader = new Runnable() {
                @Override
                public void run() {
                    String fromUser;
                    while (isOn) {
                        try {
                            if ((fromUser = getMessage()) != null) {
                                System.out.println("Client: " + fromUser);
                                if (fromUser.equals("Bye")) {
                                    close();
                                    break;
                                }
                            }
                        } catch (ChatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            Thread a = new Thread(processReader);
            a.start();
            String toUser;
            while (isOn) {
                try {
                    if ((toUser = stdIn.readLine()) != null) {
                        try {
                            sendMessage(toUser);

                            if (toUser.equals("Bye")) {
                                close();
                            }
                        } catch (ChatException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public String getMessage() throws ChatException {
        String fromUser;
        try {
            if(isOn && (fromUser = reader.readLine()) != null) {
                return fromUser;
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void sendMessage(String message) throws ChatException {
        printerWRiter.println(message);
        printerWRiter.flush();
    }

    @Override
    public void close() throws ChatException {
        this.isOn = false;
        try {
            socket.close();
        } catch (IOException e) {
            throw new ChatException(e);
        }
    }
}