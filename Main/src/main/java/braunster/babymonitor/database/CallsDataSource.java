package braunster.babymonitor.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

import braunster.babymonitor.objects.Call;

/**
 * Created by itzik on 10/12/13.
 */
public class CallsDataSource {

    final String TAG = this.getClass().getSimpleName();

    private SQLiteDatabase db;
    private DBHelper dbHelper;

    private Context context;

    // Columns
    private final static String[] allColumns = {
            DB.Column.ID, DB.Column.SESSION_ID,
            DB.Column.NAME, DB.Column.NUMBER, DB.Column.DATE, DB.Column.TEXT
    };

    public CallsDataSource(Context context){
        this.context = context;
        dbHelper = new DBHelper(context);
    }

    /** Open DB */
    private void open() throws SQLException {
        // Log.i(TAG, ACTIVITY + " db.open");
        db = dbHelper.getWritableDatabase();
    }

    /** Close DB */
    private void close() {
        // Log.i(TAG, ACTIVITY + " db.close");
        dbHelper.close();
    }

    public long addCall(String sessionId, Call call){

        open();

        long id;

        Log.i(TAG, "Adding CAll, Caller Name: " + call.getName() + ", Session Id: " + sessionId);

        // set value that will be inserted the row
        ContentValues values = new ContentValues();

        // Name and Type
        values.put(allColumns[1], sessionId);
        values.put(allColumns[2], call.getName());
        values.put(allColumns[3], call.getNumber());
        values.put(allColumns[4], call.getDate());
        values.put(allColumns[5], call.getText());

        //insert to table
        id = db.insert(DB.Table.T_CALLS, null, values);

        close();

        return id;
    }

    public ArrayList<Call> getAllCalls(String sessionId){

        open();

//        Log.i(TAG, " Getting call, ID: " + id);

        String selectQuery = "SELECT * FROM " + DB.Table.T_CALLS + " WHERE " + allColumns[1] + "=?" ;
        Cursor cursor = db.rawQuery(selectQuery, new String[]{sessionId});

//        Cursor cursor = db.query(DB.Table.T_CALLS, new String[]{ allColumns[0]}, allColumns[1] +"=?", new String[] {sessionId}, null, null, null, null);

        ArrayList<Call> calls = new ArrayList<Call>();

        if(cursor.moveToFirst())
        {
            do
            {
                calls.add(getCallFromCursor(cursor));
            }
            while (cursor.moveToNext());
        }

        close();

        return calls;
    }

    public ArrayList<Call> getAllCalls() {

        open();

//        Log.i(TAG, " getAllControllers ");

        ArrayList<Call> calls = new ArrayList<Call>();

        String selectQuery = "SELECT * FROM " + DB.Table.T_CALLS + " ORDER BY " + allColumns[0] ;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst())
        {
            do
            {
                calls.add(getCallFromCursor(cursor));
            }
            while (cursor.moveToNext());
        }

        close();

        return calls;
    }

    private Call getCallFromCursor(Cursor cursor){

        Call call = new Call(
                cursor.getLong(cursor.getColumnIndex(allColumns[0])),
                cursor.getString(cursor.getColumnIndex(allColumns[2])),
                cursor.getString(cursor.getColumnIndex(allColumns[3])),
                cursor.getString(cursor.getColumnIndex(allColumns[4])),
                cursor.getString(cursor.getColumnIndex(allColumns[5]))
        );

        return call;
    }

    /** Delete event by given id.*/
    public boolean deleteCallById(long id){

//        Log.d(TAG, "deleteCommandById, Id: " + id);

        open();

        boolean isDeleted = db.delete(DB.Table.T_CALLS, allColumns[0] + " = " + id, null) > 0;

        close();

        return isDeleted;

    }
}
