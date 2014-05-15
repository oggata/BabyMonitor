package braunster.babymonitor.fragements;

import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;

import braunster.babymonitor.BabyMonitorAppObj;

/**
 * Created by itzik on 5/9/2014.
 */
public class BaseFragment extends Fragment implements BaseFragmentInterface {

    public static final String SCREEN_WIDTH = "screen_width";
    public static final String SCREEN_HEIGHT = "screen_height";

    Point screenSize = new Point();
    View mainView;
    BabyMonitorAppObj app;

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null)
        {
            if (args.getInt(SCREEN_WIDTH, -1) != -1)
                screenSize.x =  args.getInt(SCREEN_WIDTH);

            if (args.getInt(SCREEN_HEIGHT, -1) != -1)
                screenSize.y =  args.getInt(SCREEN_HEIGHT);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() != null)
            app = (BabyMonitorAppObj) getActivity().getApplication();
        else app = BabyMonitorAppObj.getInstance();
    }

    @Override
    public void onInfoPressed() {

    }

}
