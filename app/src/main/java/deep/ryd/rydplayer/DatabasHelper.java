package deep.ryd.rydplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabasHelper extends SQLiteOpenHelper {
    // Table Name
    public static final String TABLE_NAME = "ALL_SONGS";

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
    public static final String PLAYLISTS = "playlists";

    // Database Information
    static final String DB_NAME = "History.DB";

    // database version
    static final int DB_VERSION = 4;

    // Creating table query
    private static final String CREATE_TABLE = "create table " + TABLE_NAME + "(" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + URL + " TEXT NOT NULL, "
            + TITLE + " TEXT, "
            + ARTIST_URL + " TEXT, "
            + ARTIST + " TEXT, "
            + THUMBNAIL_URL + " TEXT, "
            + ARTIST_THUMBNAIL_URL + " TEXT, "
            + PLAYED_TIMES + " INTEGER, "
            + STREAM_URL_1 + " TEXT, "
            + PLAYLISTS + " INTEGER DEFAULT 0);";


    public DatabasHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_TABLE);
        createLastTempTable(db);
    }

    public void createLastTempTable(SQLiteDatabase db){
        String TABLE_NAME = "LAST_QUEUE";

        String _ID = "_id";
        String URL = "url";
        String ARTIST = "artist";
        String TITLE = "title";
        String ARTIST_URL = "artist_url";
        String PLAYED_TIMES = "times_played";
        String THUMBNAIL_URL = "thumbnail_url";
        String ARTIST_THUMBNAIL_URL = "artist_thumb_url";
        String STREAM_URL_1 = "stream_url_1";

        String CREATE_TABLE = "create table " + TABLE_NAME + "(" +
                _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + URL + " TEXT NOT NULL, "
                + TITLE + " TEXT, "
                + ARTIST_URL + " TEXT, "
                + ARTIST + " TEXT, "
                + THUMBNAIL_URL + " TEXT, "
                + ARTIST_THUMBNAIL_URL + " TEXT, "
                + STREAM_URL_1 + " TEXT );";

        db.execSQL(CREATE_TABLE);


    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (newVersion > oldVersion) {
            db.execSQL("ALTER TABLE "+TABLE_NAME+" ADD COLUMN "+PLAYLISTS+" INTEGER DEFAULT 0");
        }
    }
}

