package braunster.babymonitor.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import braunster.babymonitor.R;
import braunster.babymonitor.fragements.BaseFragment;
import braunster.babymonitor.fragements.ConnectedFragment;
import braunster.babymonitor.objects.BabyMonitorAppObj;

/**
 * Created by itzik on 5/19/2014.
 */
public class BaseActivity extends Activity{

    BaseFragment fragment;
    BabyMonitorAppObj app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (BabyMonitorAppObj) getApplication();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.monitor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_info:
                fragment.createInfoPopup(app.getDataConnection().isConnected());
                return true;
            case R.id.action_settings:
                fragment.createSettingsPopup(fragment instanceof ConnectedFragment ? true : false, findViewById(R.id.action_settings));
                return true;
            case R.id.action_audio_settings:
                fragment.createAudioSettingPopup(app.getDataConnection().isConnected(), findViewById(R.id.action_audio_settings));
                return true;
            case R.id.action_call_log:
                fragment.createCallLogPopup(app.getDataConnection().isConnected(), app.getDataSessionId());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
