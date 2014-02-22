package TCP;

import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * Created by itzik on 11/23/13.
 */
public class Msg {

    private boolean isSent, isReceived, notifyWhenSent, notifyWhenReceived;

    private String data, destinationDeviceName;

    private String commandType;

    private int id;

    private long timeStamp;

    private int connectionType;

    public Msg(boolean notifyWhenSent, boolean notifyWhenReceived, int connectionType){
        isSent = false;
        isReceived = false;

        this.notifyWhenSent = notifyWhenSent;
        this.notifyWhenReceived = notifyWhenReceived;

        timeStamp = new Date().getTime();

        this.connectionType =connectionType;

        // TODO create unick id for each message. random number 1000 - 9999
    }

    public byte[] getMsgForSending(){
        String message;

        // Adding the command brackets and extra data for the arduino

        message = String.valueOf( Command.START );

        if (commandType != null) {message += commandType;}
        if (data != null) {message += data;}

        message += Command.END;

        byte[] bytes = null;

        try {

             return bytes = message.getBytes("UTF-8") ;

        } catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }

        return null;

    }

    public static Msg getCheckOkMessage(int connectionType){

        Msg msg = new Msg(false, false, 0);
        msg.setData(String.valueOf(Command.CHECK_OK));
        msg.setConnectionType(connectionType);

        return msg;
    }

    public static Msg getReceivedMessage(int connectionType){

        Msg msg = new Msg(false, false, 0);
        msg.setData(String.valueOf(Command.RECEIVED));
        msg.setConnectionType(connectionType);

        return msg;
    }

    /* Getters and Setters */

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean isSent) {
        this.isSent = isSent;
    }

    public boolean isReceived() {
        return isReceived;
    }

    public void setReceived(boolean isReceived) {
        this.isReceived = isReceived;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public boolean notifyWhenSent() {
        return notifyWhenSent;
    }

    public boolean notifyWhenReceived() {
        return notifyWhenReceived;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(int connectionType) {
        this.connectionType = connectionType;
    }

    public void setDestinationDeviceName(String destinationDeviceName) {
        this.destinationDeviceName = destinationDeviceName;
    }

    public String getDestinationDeviceName() {
        return destinationDeviceName;
    }
}
