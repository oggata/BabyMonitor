package braunster.babymonitor.fragements;

import android.view.View;

/**
 * Created by itzik on 5/9/2014.
 */
public interface BaseFragmentInterface {
    public void createInfoPopup(boolean connected);
    public void createAudioSettingPopup(boolean connected, View dropDownView);
    public void createSettingsPopup(boolean connected, View dropDownView);
    public void createCallLogPopup(boolean connected, String sessionId);
    public void createIncomingCallDataPopup(boolean connected, String contactName, String contactNumber, String text, boolean outSideTouchable);

    public void showContent();
    public void hideContent();
}
