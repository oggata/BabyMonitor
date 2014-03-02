package TCP;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by itzik on 11/8/13.
 */
public class TCPServerConnectionThread extends Thread{

    final String TAG = TCPServerConnectionThread.class.getSimpleName();

    private static final int SERVER_SO_TIMEOUT = 10 * 1000;
    private ServerSocket serverSocket;
    private Socket socket = null;

    private Handler handler;
    private Message msg;

    private boolean accept = false, connectToServer = false, close = false;
    private int serverPort;
    private String serverIp;

    public TCPServerConnectionThread(String ip, int serverPort){
        Log.i(TAG, "Starting Connection Thread As Client");
        serverIp = ip;
        this.serverPort = serverPort;

        connectToServer = true;
    }


    public TCPServerConnectionThread(int serverPort){
        Log.i(TAG, "Starting Connection Thread As Server");
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
    }

    public synchronized void startAccept(boolean accept){
        this.accept = accept;
    }

    public void close(){
        close = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Closing the server");
                    if (serverSocket != null)
                        serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean isClosed(){
        return close;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public boolean isAccepting() {
        return accept;
    }

    // Host Connection
    private void accept(){

        if (serverSocket != null)
        {
            try {

                Log.i(TAG, "Waiting for client...");

                serverSocket.setSoTimeout(SERVER_SO_TIMEOUT);

                socket = serverSocket.accept();

                if (socket.isConnected())
                {
                    Log.i(TAG, "Connected.");

                    // TODO check with and without
                    socket.setTcpNoDelay(false);

                    if (!close)
                    {
                        successMsg();

                        serverSocketMsg();
                    }
                    else
                    {
                        Log.i(TAG, "Server was closed while accepting");

                        try {
                            socket.close();
                        }
                        catch (IOException e){}
                    }

                }

            }
            catch (SocketException e){}
            catch (IOException e) {

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
        msg = new Message();
        msg.what = TCPConnection.SUCCESS_CONNECTION;
        msg.obj = socket;
        handler.sendMessage(msg);

        Thread.currentThread().interrupt();
    }

    private void serverSocketMsg(){
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
