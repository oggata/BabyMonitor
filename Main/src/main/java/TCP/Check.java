package TCP;

/**
 * Created by itzik on 11/18/13.
 */
public class Check {

    private final String TAG = Check.class.getSimpleName();

    public static final int SUCCESS = 2002;
    public static final int FAILED = 2003;
    public static final int TIMEOUT = 2004;
    public static final int NOT_STARTED = 2005;

    public static final int DONE = 1001;
    public static final int ON_PROGRESS = 1002;
//    public static final int STARTED = 1000;

    private int timeOut = 1 *  ( 30 * 1000 );
    private long startedTime = 0 ;
    private int status = NOT_STARTED, result;

    public Check(){
    }

    public int getStatus() {
        return status;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getResult() {
        return result;
    }

    public void done(){ status = DONE; }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setStartedTime(long startedTime) {
        this.startedTime = startedTime;
        status = ON_PROGRESS;
    }

    public long getStartedTime() {
        return startedTime;
    }

    public boolean checkForTimeout(long currentTime) {
//        Log.d(TAG, " current Time - startedTime = " + (currentTime - startedTime) );

        // Check if there wasn't a timeout since the check has started.
        if ( status == ON_PROGRESS && currentTime - startedTime > timeOut)
        {
            status = DONE;
            result = TIMEOUT;
//            Log.d(TAG, "Timeout");
            return true;
        }

        return false;
    }

    public static Msg getCheckMessage(){

        Msg msg = new Msg(false, true, 0);

        msg.setCommandType(String.valueOf(Command.CHECK));

        return msg;
    }
}
