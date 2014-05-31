package com.braunster.mymodule.app.archive;

/**
 * Created by itzik on 11/18/13.
 */
public class Command {

    // Basic
    public static final char END = '}';
    public static final char START = '{';
    public static final char LOG = '^';
    public static final char RECEIVED = '.';
    public static final char CHECK = '?';
    public static final char CHECK_OK = 'K';

    // Car
    public static final char STICK_DRIVE = '@';
    public static final char ACCELEROMETER_DRIVE = '*';
    public static final char STOP = '!';

    // IR
    public static final char READ_IR_SIGNAL = '*';
    public static final char SEND_IR_SIGNAL = 'S';
}
