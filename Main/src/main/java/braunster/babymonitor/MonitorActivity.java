package braunster.babymonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import TCP.ConnectionStateChangeListener;
import TCP.TCPConnection;
import TCP.onConnectionLostListener;

public class MonitorActivity extends Activity {

    private final String TAG = MonitorActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.monitor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements View.OnClickListener {

        private final String TAG = PlaceholderFragment.class.getSimpleName();

        private static final String PREFS_SERVER_IP = "prefs.server.ip";

        private WifiManager wifiManager;
        private ConnectivityManager connManager;
        private NetworkInfo wifi;

        private TCPConnection connection;

        private View rootView;
        private LinearLayout liServerClientBtn, liServerDataEt;
        private Button btnServer, btnClient, btnDisconnect, btnPlayStop;
        private TextView txtIp;
        private EditText etIp, etServerPort;


        // Animation
        private Animation animFadeIn, animFadeOut;
        private TransitionDrawable animTrans;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            rootView = inflater.inflate(R.layout.fragment_monitor, container, false);

            animFadeIn = AnimationUtils.loadAnimation(getActivity(), R.animator.fade_in);
            animFadeOut = AnimationUtils.loadAnimation(getActivity(), R.animator.fade_out);
            animTrans = (TransitionDrawable) rootView.getBackground();

            viewsInit();

            connManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            wifiManager = (WifiManager) getActivity().getSystemService(Activity.WIFI_SERVICE);

            // Check for ip address from the preferences
            etIp.setText(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(PREFS_SERVER_IP, ""));

            if (txtIp != null)
                if (wifi.isConnected())
                    txtIp.setText(getIP());
                else
                    txtIp.setText("Not Connected To Wifi" );

            btnServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (wifi.isConnected()) {

                        if (!wifi.isConnected())
                        {
                            Toast.makeText(getActivity(), "Not connected to WIFI", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (connection == null)
                        {
                            initConnection();
                        }

                        connection.start(2000);
                    }
                }
            });

            btnClient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (wifi.isConnected()) {

                        if (!wifi.isConnected())
                        {
                            Toast.makeText(getActivity(), "Not connected to WIFI", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (!etIp.getText().toString().isEmpty())
                        {
                            if (connection == null)
                            {
                                initConnection();
                            }

                            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(PREFS_SERVER_IP,etIp.getText().toString()).commit();

                            connection.start(etIp.getText().toString(), 2000);
                        }
                        else
                            Toast.makeText(getActivity(), "Please enter the ip adress of the server", Toast.LENGTH_LONG).show();
                    }
                }
            });

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            btnPlayStop.setOnClickListener(this);
            btnDisconnect.setOnClickListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
//            if (connection != null)
//                connection.Terminate();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // TODO save a data obj to the bundle.
        }

        private void viewsInit(){

            // Linear Layout
            liServerClientBtn = (LinearLayout) rootView.findViewById(R.id.linear_client_server_select_buttons);
            liServerDataEt = (LinearLayout) rootView.findViewById(R.id.linear_server_data);

            // Buttons - Server & Client Connection - Disconnect - Control
            btnServer = (Button) rootView.findViewById(R.id.btn_start_server);
            btnClient = (Button) rootView.findViewById(R.id.btn_start_client);
            btnDisconnect = (Button) rootView.findViewById(R.id.btn_disconnect);
            btnPlayStop = (Button) rootView.findViewById(R.id.btn_stop_play);

            // EditText - Server Data
            etIp = (EditText) rootView.findViewById(R.id.et_server_ip);
            etServerPort = (EditText) rootView.findViewById(R.id.et_server_port);

            // TextView - Phone Ip
            txtIp = (TextView) rootView.findViewById(R.id.txt_phone_ip);
        }

        private void initConnection(){
            connection = new TCPConnection();

            connection.setConnectionStateChangeListener(new ConnectionStateChangeListener() {
                @Override
                public void onConnected(int connectionType, Object obj) {
                    Log.d(TAG, "Connected");

                    animateBackground();

                    hideServerData();
                    hideTxtIp();
                    hideServerClientButtons();

                    rootView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showControlButtons();
                            showDisconnectButton();
                        }
                    }, 1000);
                }

                @Override
                public void onConnectionChangeState(int connectionType, String state) {

                }

                @Override
                public void onConnectionFailed(String issue) {
                    Log.d(TAG, "Connection Failed");
                }
            });

            connection.setOnConnectionLost(new onConnectionLostListener() {
                @Override
                public void onConnectionLost(int connectionType, String issue) {

                    Log.d(TAG, "onConnection Lost");
//                    hideControlButtons();
//                    hideDisconnectButton();
//                    showTxtIp();
//                    showServerClientButtons();
//                    showServerData();
                }
            });
        }

        private String getIP(){
            int ip =wifiManager.getConnectionInfo().getIpAddress();

            String ipString = String.format(
                    "%d.%d.%d.%d",
                    (ip & 0xff),
                    (ip >> 8 & 0xff),
                    (ip >> 16 & 0xff),
                    (ip >> 24 & 0xff));

            return ipString;
        }

        @Override
        public void onClick(View v) {

            switch (v.getId())
            {
                case R.id.btn_stop_play:

                    if (connection != null && connection.isConnected())
                    {
                        if (!v.isSelected())
                        {
                            if(connection.getConnectionType() == TCPConnection.SERVER)
                                connection.getAudioController().play();
                            else
                                connection.getRecordController().record();
                        }
                        else
                        {
                            if(connection.getConnectionType() == TCPConnection.SERVER)
                                connection.getAudioController().stop();
                            else
                                connection.getRecordController().stop();
                        }

                        v.setSelected(!v.isSelected());
                    }

                    break;

                case R.id.btn_disconnect:
                    if (connection != null)
                    {
                        Log.d(TAG, "Disconnect");

                        connection.close();

                        animateBackground();

                        hideControlButtons();
                        hideDisconnectButton();

                        showServerClientButtons();
                        showServerData();
                        showTxtIp();

                        btnPlayStop.setSelected(false);
                    }
                    break;
            }
        }

        // Animate
//        private void colorChange(){
////            //animate from your current color to red
////            final ValueAnimator anim = ValueAnimator.ofInt(Color.parseColor("#FFFFFF"), Color.parseColor("#000000"));
////            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
////                @Override
////                public void onAnimationUpdate(ValueAnimator animation) {
////                    rootView.setBackgroundColor( () anim.getAnimatedValue());
////                }
////            });
////
////            anim.start();
//
//
//            ColorDrawable layers[] = new ColorDrawable[2];
//            layers[0] = new ColorDrawable(0xff0000ff);
//            layers[1] = new ColorDrawable(0xffff0000);
//            ColorTransaction colorTransaction = new ColorTransaction(layers);
//            rootView.setBackgroundDrawable(colorTransaction);
//
//            colorTransaction.changeColor(0xff00ff00);
//        }

        private void  fadeViewIn(final View v){

            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);

            v.animate().alpha(1f).setDuration(1000).setListener(null);
        }

        private void fadeViewOut(final View v){

            v.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            v.setVisibility(View.VISIBLE);
                        }
                    });
        }

        private void animateBackground(){
            if (connection != null && connection.isConnected())
                animTrans.startTransition(1000);
            else animTrans.reverseTransition(1000);
        }

        private void showControlButtons(){
            fadeViewIn(btnPlayStop);
        }

        private void hideControlButtons(){
            fadeViewOut(btnPlayStop);
        }

        private void showServerData(){
            fadeViewIn(liServerDataEt);
        }

        private void hideServerData(){
            fadeViewOut(liServerDataEt);
        }

        private void showDisconnectButton(){
           fadeViewIn(btnDisconnect);
        }

        private void hideDisconnectButton(){
            fadeViewOut(btnDisconnect);
        }

        private void showServerClientButtons(){
            fadeViewIn(liServerClientBtn);
        }

        private void hideServerClientButtons(){
            fadeViewOut(liServerClientBtn);
        }

        private void showTxtIp(){
            fadeViewIn(txtIp);
        }

        private void hideTxtIp(){
            fadeViewOut(txtIp);
        }
    }



}
