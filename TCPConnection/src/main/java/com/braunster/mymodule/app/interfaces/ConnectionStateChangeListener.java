package com.braunster.mymodule.app.interfaces;

/**
 * Created by itzik on 11/21/13.
 */
public interface ConnectionStateChangeListener {
    public static final int TYPE_STATE = 0;
    public static final int TYPE_CONNECTED = 1;
    public static final int TYPE_FAILED = 2;
    public void onConnected(int connectionType, Object obj, String sessionId);

    public void onConnectionChangeState(int connectionType, String state);

    public void onConnectionFailed(String issue);
}
