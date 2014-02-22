package TCP;

/**
 * Created by itzik on 11/21/13.
 */
public interface ConnectionStateChangeListener {

    public void onConnected(int connectionType, Object obj);

    public void onConnectionChangeState(int connectionType, String state);

    public void onConnectionFailed(String issue);
}
