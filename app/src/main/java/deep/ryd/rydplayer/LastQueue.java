package deep.ryd.rydplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LastQueue extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "LAST_QUEUE";

    // Table columns
    public static final String _ID = "_id";
    public static final String URL = "url";
    public static final String ARTIST = "artist";
    public static final String TITLE = "title";
    public static final String ARTIST_URL = "artist_url";
    public static final String PLAYED_TIMES = "times_played";
    public static final String THUMBNAIL_URL = "thumbnail_url";
    public static final String ARTIST_THUMBNAIL_URL = "artist_thumb_url";
    public static final String STREAM_URL_1 = "stream_url_1";


    // Database Information
    static final String DB_NAME = "TempHistory.DB";

    // database version
    static final int DB_VERSION = 1;

    // Creating table query
    private static final String CREATE_TABLE = "create table " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + URL + " TEXT NOT NULL, "
            + TITLE + " TEXT, "
            + ARTIST_URL + " TEXT, "
            + ARTIST + " TEXT, "
            + THUMBNAIL_URL + " TEXT, "
            + ARTIST_THUMBNAIL_URL + " TEXT, "
            + STREAM_URL_1 + " TEXT );";


    public LastQueue(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
