package TCP;

/**
 * Created by itzik on 2/21/14.
 */
public interface WifiStatesListener {

    public void onEnabled();

    public void onDisabled();

    public void onConnected(String networkName);

    public void onDisconnected();

}
