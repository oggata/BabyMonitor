package braunster.babymonitor.database;


import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class DB   {
	
	final static String TAG = DB.class.getSimpleName();

	// SQL Text
	private static final String TYPE_T = " TEXT";
	private static final String TYPE_I = " INTEGER";
	private static final String COMMA_SEP = ", ";
	
	// Creating Tables
	public static void onCreate(SQLiteDatabase db) {
        createCommandsTable(db);
	}

    private static void createCommandsTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE "
                + Table.T_CALLS + " ( " + Column.ID + " INTEGER PRIMARY KEY" + COMMA_SEP

                + Column.SESSION_ID + TYPE_T + COMMA_SEP

                + Column.NAME + TYPE_T + COMMA_SEP

                + Column.NUMBER + TYPE_I + COMMA_SEP

                + Column.DATE + TYPE_T + COMMA_SEP

                + Column.TEXT + TYPE_T

                +" );");
    }
	
	// Upgrading database
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
//		Log.d(TAG, " ON UPGRADE");

        db.execSQL("DROP TABLE IF EXISTS " + Table.T_CALLS);

		onCreate(db);
	}

    public static final class Table {
        // Tables Names
        public static final String T_CALLS = "table_calls";
    }
	// Columns names
    public static final class Column implements BaseColumns {
        //General
        public static final String ID = "_id";
        public static final String SESSION_ID = "_session_id";
        public static final String NAME = "_name";
        public static final String TEXT = "_text";
        public static final String NUMBER = "_number";
        public static final String DATE = "_date";
    }
}
