package hw5.ockel.id1212.kth.se.hw5.net;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;


/**
 * This class handles all the communication between the client and the server. For example when the client
 * connects and disconnects from the server.
 */

public class ServerHandler {
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    private Callback callback;


    private boolean connected = false;
    private final int TIMEOUT = 30000;

    public ServerHandler(Callback cb){
        this.callback = cb;
    }

    /**
     * Connects the client with the server.
     * @param host which host the client should use
     * @param port which port the client should connect to on the host
     * @throws IOException
     */

    public void connect(String host, int port) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), TIMEOUT);
        this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.connected = true;
        callback.notifyConnected();
        new Thread(new ServerListener()).start();
        System.out.println("starting to listen");

        //todo Catch exception and disconnect
    }

    public ServerHandler getServerHandler(){
        return this;
    }

    public void startGame() {
        sendCommand("start");
    }

    public void quitGame() {
        sendCommand("exit");
    }

    public void restartGame() {
        sendCommand("restart");
    }

    public void makeGuess(String message) {
        if (message.length() == 0)
            return;
        if (message.length() == 1)
            sendCommand(message);
        else
            sendCommand("guess " + message);
    }

    private void sendCommand(String message) {
        class MessageSender {
            private String message;

            private MessageSender(String message) {
                this.message = message;
            }

            private void send() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            writer.write(message + "\n");
                            writer.flush();
                            callback.messageSent();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
        new MessageSender(message).send();
    }


    /**
     * Disconnects the client from the server.
     */

    public void disconnect() {
        try {
            writer.close();
            socket.close();
            connected = false;
        } catch (IOException e) {
            System.out.println("Issue while disconnecting.");
            e.printStackTrace();
        }finally {
            callback.notifyDisconnected();
        }
    }


    private class ServerListener implements Runnable {

        /**
         * Continuously reads data from the server and handles the results in different ways.
         */

        @Override
        public void run() {
            while (connected){
                try {
                    String received = reader.readLine();
                    System.out.println(received);
                    if (received == null){
                        disconnect();
                        return;
                    }
                    callback.messageReceived(received);
                } catch (IOException e) {
                    e.printStackTrace();
                    disconnect();
                }
            }
        }

    }

}
