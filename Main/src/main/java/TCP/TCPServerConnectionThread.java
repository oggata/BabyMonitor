package TCP;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by itzik on 11/8/13.
 */
public class TCPServerConnectionThread extends Thread{

    final String TAG = TCPServerConnectionThread.class.getSimpleName();

    private static final int SERVER_SO_TIMEOUT = 100;
    private ServerSocket serverSocket;
    private Socket socket = null;

    private Handler handler;
    private Message msg;

    private boolean accept = false, connectToServer = false, close = false;
    private int serverPort;
    private String serverIp;

    public TCPServerConnectionThread(String ip, int serverPort){
        serverIp = ip;
        this.serverPort = serverPort;

        connectToServer = true;
    }


    public TCPServerConnectionThread(int serverPort){
        Log.i(TAG, "Starting server port");
        this.serverPort = serverPort;

        try {
            serverSocket = new ServerSocket(serverPort);
            accept = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        while (!close)
        {
            if (accept)
                accept();


            if (connectToServer)
                connectToServer();
        }

//        try {
//            if (serverSocket != null)
//            {
//                serverSocket.close();
//
//                Log.i(TAG, " ServerSocket is now closed");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


    }

    public synchronized void startAccept(boolean accept){
        this.accept = accept;
    }

    public void close(){
        close = true;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    // Host Connection
    private void accept(){
        if (serverSocket != null)
        {
            try {

                Log.i(TAG, "Waiting for client...");

                socket = serverSocket.accept();

                if (socket.isConnected())
                {
                    Log.i(TAG, "Connected.");

                    socket.setTcpNoDelay(true);

                    socket.setSoTimeout(SERVER_SO_TIMEOUT);

                    successMsg();

                    serverSocketMsg();
                }

            } catch (IOException e) {

                failMsg(TCPConnection.ERROR_ACCEPTATION_TIMEOUT);

                e.printStackTrace();
            }
        }


        accept = false;
    }

    // Client connection
    private void connectToServer(){
        try {
            // Creating the connection
            InetAddress serverAddr = InetAddress.getByName(serverIp);

            socket = new Socket(serverAddr, serverPort);

            if (socket.isConnected())
                successMsg();

        } catch (UnknownHostException e1) {
            failMsg(TCPConnection.ERROR_CONNECTING_AS_CLIENT);
            e1.printStackTrace();
        }
        catch (ConnectException e){
            failMsg(TCPConnection.ERROR_CONNECTING_AS_CLIENT);
            e.printStackTrace();
        } catch(IOException e1) {
            failMsg(TCPConnection.ERROR_CONNECTING_AS_CLIENT);
            e1.printStackTrace();
        }

        connectToServer = false;
    }

    private void successMsg(){
        Log.i(TAG, "Connected.");
        msg = new Message();
        msg.what = TCPConnection.SUCCESS_CONNECTION;
        msg.obj = socket;
        handler.sendMessage(msg);

        Thread.currentThread().interrupt();
    }

    private void serverSocketMsg(){
        Log.i(TAG, "Connected.");
        msg = new Message();
        msg.what = TCPConnection.SERVER_SOCKET;
        msg.obj = serverSocket;
        handler.sendMessage(msg);

        Thread.currentThread().interrupt();
    }

    private void failMsg(int code){
        msg = new Message();
        msg.what = code;
        handler.sendMessage(msg);

        Thread.currentThread().interrupt();
    }
}