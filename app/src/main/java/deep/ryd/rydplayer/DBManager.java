package deep.ryd.rydplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.strictmode.SqliteObjectLeakedViolation;
import android.util.Log;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private DatabasHelper databasHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        databasHelper = new DatabasHelper(context);
        database = databasHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        databasHelper.close();
    }

    public void insert(String title, String url, String artist, String artist_url, String thumburl, String artist_thumb, String played_times, String audio_stream_ulr_1) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabasHelper.TITLE, title);
        contentValues.put(DatabasHelper.URL, url);
        contentValues.put(DatabasHelper.ARTIST, artist);
        contentValues.put(DatabasHelper.ARTIST_URL, artist_url);
        contentValues.put(DatabasHelper.THUMBNAIL_URL, thumburl);
        contentValues.put(DatabasHelper.ARTIST_THUMBNAIL_URL, artist_thumb);
        contentValues.put(DatabasHelper.PLAYED_TIMES, played_times);
        contentValues.put(DatabasHelper.STREAM_URL_1, audio_stream_ulr_1);
        database.insert(DatabasHelper.TABLE_NAME, null, contentValues);
    }

    public Cursor fetch() {
        String[] colums = new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES,
                DatabasHelper.STREAM_URL_1
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME, colums, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetch_sorted() {
        String[] colums = new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES,
                DatabasHelper.STREAM_URL_1
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME, colums, null, null, null, null, DatabasHelper.PLAYED_TIMES + " DESC");
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetch_top_artist() {
        String[] colums = new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES,
                DatabasHelper.STREAM_URL_1
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME, colums, null, null, DatabasHelper.ARTIST, null, "Count(" + DatabasHelper.ARTIST_THUMBNAIL_URL + ") " + " DESC");
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public StreamInfo fetchSong(String url){
        String[] colums = new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES,
                DatabasHelper.STREAM_URL_1
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME, colums, DatabasHelper.URL + "=?", new String[]{url}, null, null, null, null);
        if (cursor != null && cursor.getCount()!=0) {
            cursor.moveToFirst();

                int sid=NewPipe.getIdOfService("YouTube");
                StreamInfo streamInfo =new StreamInfo(
                        sid,
                        cursor.getString(cursor.getColumnIndex(DatabasHelper.URL)),
                        cursor.getString(cursor.getColumnIndex(DatabasHelper.URL)),
                        StreamType.AUDIO_STREAM,
                        "",
                        cursor.getString(cursor.getColumnIndex(DatabasHelper.TITLE)),
                        0
                );
                streamInfo.setThumbnailUrl(cursor.getString(cursor.getColumnIndex(DatabasHelper.THUMBNAIL_URL)));
                streamInfo.setUploaderName(cursor.getString(cursor.getColumnIndex(DatabasHelper.ARTIST)));
                streamInfo.setUploaderUrl(cursor.getString(cursor.getColumnIndex(DatabasHelper.ARTIST_URL)));
                streamInfo.setUploaderAvatarUrl(cursor.getString(cursor.getColumnIndex(DatabasHelper.ARTIST_THUMBNAIL_URL)));
                List<AudioStream> audioStreams = new ArrayList<>();
                audioStreams.add(core.StringtoAudioStream(cursor.getString(cursor.getColumnIndex(DatabasHelper.STREAM_URL_1))));
                Log.i("ryd","AUDIO STREAM LOADED "+audioStreams.get(0).getUrl());
                streamInfo.setAudioStreams(audioStreams);

                return streamInfo;
        }

        else {
            return null;
        }

    }


    public void addSong(String title, String url, String artist, String thumburl, String artist_thumb, String artist_url, String stream_url_1) {
        String played_times = "1";

        String[] colums = new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES,
                DatabasHelper.STREAM_URL_1
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME, colums, DatabasHelper.URL + "=?", new String[]{url}, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            Log.i("ryd", "SONG FOUND INCREASING NUMBER ");
            cursor.moveToFirst();
            String id = cursor.getString(cursor.getColumnIndex(DatabasHelper._ID));
            played_times = String.valueOf((new Integer(cursor.getString(cursor.getColumnIndex(DatabasHelper.PLAYED_TIMES))) + 1));
            Log.i("ryd", "INCREASED NUMBER TO " + played_times);
            update(Long.parseLong(id), title, url, artist, artist_url, thumburl, artist_thumb, played_times, stream_url_1);
        } else {
            Log.i("ryd", "SONG NOT FOUND ADDING ");
            insert(title, url, artist, artist_url, thumburl, artist_thumb, played_times, stream_url_1);
        }
    }


    public int update(long _id, String title, String url, String artist, String artist_url, String thumb, String artist_thumb, String played_times, String stream_url_1) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabasHelper.TITLE, title);
        contentValues.put(DatabasHelper.URL, url);
        contentValues.put(DatabasHelper.ARTIST, artist);
        contentValues.put(DatabasHelper.ARTIST_URL, artist_url);
        contentValues.put(DatabasHelper.PLAYED_TIMES, played_times);
        contentValues.put(DatabasHelper.THUMBNAIL_URL, thumb);
        contentValues.put(DatabasHelper.ARTIST_THUMBNAIL_URL, artist_thumb);
        contentValues.put(DatabasHelper.STREAM_URL_1, stream_url_1);
        int i = database.update(DatabasHelper.TABLE_NAME, contentValues, DatabasHelper._ID + " = " + _id, null);
        return i;
    }

    public void delete(long _id) {
        database.delete(DatabasHelper.TABLE_NAME, DatabasHelper._ID + "=" + _id, null);
    }

}