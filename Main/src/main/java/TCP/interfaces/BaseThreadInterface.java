package TCP.interfaces;

import android.os.Handler;

/**
 * Created by itzik on 5/11/2014.
 */
public interface BaseThreadInterface {
    public void setHandler(Handler handler);
    public void close();
}
