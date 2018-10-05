package deep.ryd.rydplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBManager {
    private DatabasHelper databasHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c){
        context=c;
    }

    public DBManager open() throws SQLException{
        databasHelper = new DatabasHelper(context);
        database = databasHelper.getWritableDatabase();
        return  this;
    }

    public void close(){
        databasHelper.close();
    }

    public void insert(String title, String url, String artist, String artist_url,String thumburl,String artist_thumb, String played_times){
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabasHelper.TITLE, title);
        contentValues.put(DatabasHelper.URL, url);
        contentValues.put(DatabasHelper.ARTIST,artist);
        contentValues.put(DatabasHelper.ARTIST_URL,artist_url);
        contentValues.put(DatabasHelper.THUMBNAIL_URL,thumburl);
        contentValues.put(DatabasHelper.ARTIST_THUMBNAIL_URL,artist_thumb);
        contentValues.put(DatabasHelper.PLAYED_TIMES,played_times);
        database.insert(DatabasHelper.TABLE_NAME, null, contentValues);
    }

    public Cursor fetch(){
        String[] colums=new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME,colums,null,null,null,null,null);
        if (cursor!= null){
            cursor.moveToFirst();
        }
        return cursor;
    }
    public Cursor fetch_sorted(){
        String[] colums=new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME,colums,null,null,null,null,DatabasHelper.PLAYED_TIMES+" DESC");
        if (cursor!= null){
            cursor.moveToFirst();
        }
        return cursor;
    }
    public Cursor fetch_top_artist(){
        String[] colums=new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME,colums,null,null,DatabasHelper.ARTIST,null,"Count("+DatabasHelper.ARTIST_THUMBNAIL_URL+") "+" DESC");
        if (cursor!= null){
            cursor.moveToFirst();
        }
        return cursor;
    }


    public void addSong(String title, String url, String artist,String thumburl,String artist_thumb, String artist_url){
        String played_times = "1";

        String[] colums=new String[]{
                DatabasHelper._ID,
                DatabasHelper.TITLE,
                DatabasHelper.URL,
                DatabasHelper.ARTIST_URL,
                DatabasHelper.ARTIST,
                DatabasHelper.THUMBNAIL_URL,
                DatabasHelper.ARTIST_THUMBNAIL_URL,
                DatabasHelper.PLAYED_TIMES
        };
        Cursor cursor = database.query(DatabasHelper.TABLE_NAME,colums,DatabasHelper.URL+"=?",new String[]{url},null,null,null);
        if (cursor!= null && cursor.getCount()>0){
            Log.i("ryd","SONG FOUND INCREASING NUMBER ");
            cursor.moveToFirst();
            String id = cursor.getString(cursor.getColumnIndex(DatabasHelper._ID));
            played_times = String.valueOf((new Integer(cursor.getString(cursor.getColumnIndex(DatabasHelper.PLAYED_TIMES)))+1));
            Log.i("ryd","INCREASED NUMBER TO "+played_times);
            update(Long.parseLong(id),title,url,artist,artist_url,thumburl,artist_thumb,played_times);
        }
        else {
            Log.i("ryd","SONG NOT FOUND ADDING ");
            insert(title,url,artist,artist_url,thumburl,artist_thumb,played_times);
        }
    }


    public int update(long _id, String title, String url, String artist, String artist_url,String thumb,String artist_thumb, String played_times) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabasHelper.TITLE, title);
        contentValues.put(DatabasHelper.URL, url);
        contentValues.put(DatabasHelper.ARTIST,artist);
        contentValues.put(DatabasHelper.ARTIST_URL,artist_url);
        contentValues.put(DatabasHelper.PLAYED_TIMES,played_times);
        contentValues.put(DatabasHelper.THUMBNAIL_URL,thumb);
        contentValues.put(DatabasHelper.ARTIST_THUMBNAIL_URL,artist_thumb);
        int i = database.update(DatabasHelper.TABLE_NAME, contentValues, DatabasHelper._ID + " = " + _id, null);
        return i;
    }

    public void delete(long _id) {
        database.delete(DatabasHelper.TABLE_NAME, DatabasHelper._ID + "=" + _id, null);
    }

}
