package braunster.babymonitor.objects;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import braunster.babymonitor.R;

/**
 * Created by itzik on 10/5/13.
 */


public class SimpleListAdapter extends BaseAdapter {

    private static final String TAG = SimpleListAdapter.class.getSimpleName();

    private Activity mActivity;

    private List<Call> calls = new ArrayList<Call>();

    private int textColor = Color.WHITE;

    boolean useTags = false;

    //View
    private View row;

    private TextView textView;

    public SimpleListAdapter(Activity activity){
        mActivity = activity;
    }

    public SimpleListAdapter(Activity activity, List<Call> listData){
        mActivity = activity;
        this.calls = listData;
    }

    @Override
    public int getCount() {
        return calls.size();
    }

    @Override
    public Object getItem(int i) {
        return calls.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        row = view;

        if ( row == null)
        {
            row =  ( (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) ).inflate(R.layout.row_text_view, null);

        }

        textView = (TextView) row.findViewById(R.id.txt_simple_row);

        if (textColor != -1)
            textView.setTextColor(textColor);

        textView.setText(calls.get(position).getName() + " - "
                + calls.get(position).getNumber() + " - "
                + millisToStringData(calls.get(position).getDate())
                + (calls.get(position).getText() != null ? " - SMS" : "") );

        return row;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setListData(List<Call> listData) {
        this.calls = listData;
        notifyDataSetChanged();
    }

    private String millisToStringData(String m){

        long millis = Long.parseLong(m);

        DateFormat formatter = new SimpleDateFormat("HH:mm");

        System.out.println(millis);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);


        return formatter.format(calendar.getTime());
    }
}
