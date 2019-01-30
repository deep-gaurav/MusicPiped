package me.devsilver.musicpiped;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.migration.Migration;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MusicDBManager {
    public static AppDatabase database;
    public static MusicDao musicDao;
    public static PlaylistDao playlistDao;
    public static Gson gson=new Gson();

    public static Migration Migration2_3 = new Migration(2,3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE 'PlaylistEntity' ( 'id' INTEGER NOT NULL, 'name' TEXT, PRIMARY KEY('id'))"
            );
        }
    };
    public static Migration Migration3_4 = new Migration(3,4){

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

        }
    };
    public static Migration Migration4_5 = new Migration(4,5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Cursor c = database.query("SELECT * FROM MusicEntity");
            if(c!=null){
                c.moveToFirst();
                for(int i=0;i<c.getCount();i++){
                    for (String x:c.getColumnNames())
                        System.out.println(x);
                    String title = c.getString(0);

                    byte[] arrtitle = title.getBytes(ISO_8859_1);
                    String newtitle = new String(arrtitle,UTF_8);
                    ContentValues cv  = new ContentValues();
                    cv.put("title",newtitle);
                    database.update("MusicEntity",SQLiteDatabase.CONFLICT_REPLACE,cv,"title= '"+title+"'",null);
                    c.moveToNext();
                }
            }
        }
    };
    public static Migration Migration5_6 = new Migration(5,6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

        }
    };
    MusicDBManager(Context context){
        if(database==null){
            database= Room.databaseBuilder(context,AppDatabase.class,"MusicDB")
                    .addMigrations(Migration2_3,Migration3_4,Migration4_5,Migration5_6)
                    .build();
            musicDao=database.musicDao();
            playlistDao=database.playlistDao();
        }
    }
    public void AddMusic(final AbstractMap music){
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                MusicEntity musicEntity = musicDao.findByTitle(music.get("title") .toString());
                if(musicEntity!=null){
                    System.out.print("FOUND MUSIC "+musicEntity.title);
                    musicEntity.timesPlayed+=1;
                    musicEntity.lastplayed=System.currentTimeMillis();
                    musicDao.Update(musicEntity);
                }
                else{

                    musicEntity=new MusicEntity();
                    musicEntity.title=(music.get("title") .toString());
                    musicEntity.timesPlayed=1;
                    musicEntity.detailJSON=gson.toJson(music);
                    musicEntity.lastplayed=System.currentTimeMillis();
                    musicEntity.addedon=System.currentTimeMillis();
                    musicEntity.artist=music.get("authorId").toString();
                    musicDao.insertAll(musicEntity);
                }
                return null;
            }
        }.execute();

    }
    public void updateplaylist(final int p, final String title){
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                MusicEntity musicEntity = musicDao.findByTitle(title);
                if(musicEntity!=null){
                    musicEntity.playlists=p;
                    musicDao.Update(musicEntity);
                }
                return null;
            }
        }.execute();
    }
    public void UpdateURLS(final String title, final String detailJSON){
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                MusicEntity musicEntity = musicDao.findByTitle(title);
                if(musicEntity!=null){
                    musicEntity.detailJSON=detailJSON;
                    musicDao.Update(musicEntity);
                }
                return null;
            }
        }.execute();

    }
    public HashMap getMusic(final String title) throws ExecutionException, InterruptedException {
        HashMap hashMap =
        new AsyncTask<Void,Void,HashMap>(){

            @Override
            protected HashMap doInBackground(Void... voids) {
                MusicEntity musicEntity = musicDao.findByTitle(title);
                return gson.fromJson(musicEntity.detailJSON,HashMap.class);
            }
        }.execute().get();
        return hashMap;
    }

    public List<MusicEntity> getTopTracks() throws ExecutionException, InterruptedException {
        final List<MusicEntity> tt ;
        tt=new AsyncTask<Void,Void,List<MusicEntity>>(){

            @Override
            protected List<MusicEntity> doInBackground(Void... voids) {
                List<MusicEntity> musicEntities = musicDao.getAllByPopularity();

                return musicEntities;
            }
        }.execute().get();
        return tt;
    }
    public List<MusicEntity> getArtistTracks(final String artistID) throws ExecutionException, InterruptedException {
        final List<MusicEntity> tt ;
        tt=new AsyncTask<Void,Void,List<MusicEntity>>(){

            @Override
            protected List<MusicEntity> doInBackground(Void... voids) {
                List<MusicEntity> musicEntities = musicDao.getArtistTrack(artistID);

                return musicEntities;
            }
        }.execute().get();
        return tt;
    }
    public List<MusicEntity> getRecents() throws ExecutionException, InterruptedException {
            final List<MusicEntity> tt ;
            tt=new AsyncTask<Void,Void,List<MusicEntity>>(){

                @Override
                protected List<MusicEntity> doInBackground(Void... voids) {
                    List<MusicEntity> musicEntities = musicDao.getRecent();

                    return musicEntities;
                }
            }.execute().get();
            return tt;
        }
    public List<MusicEntity> getLastAdded() throws ExecutionException, InterruptedException {
                final List<MusicEntity> tt ;
                tt=new AsyncTask<Void,Void,List<MusicEntity>>(){

                    @Override
                    protected List<MusicEntity> doInBackground(Void... voids) {
                        List<MusicEntity> musicEntities = musicDao.getLastAdded();

                        return musicEntities;
                    }
                }.execute().get();
                return tt;
            }
    public List<MusicEntity> getArtists() throws ExecutionException, InterruptedException {
            final List<MusicEntity> tt ;
            tt=new AsyncTask<Void,Void,List<MusicEntity>>(){

                @Override
                protected List<MusicEntity> doInBackground(Void... voids) {
                    List<MusicEntity> musicEntities = musicDao.getArtists();

                    return musicEntities;
                }
            }.execute().get();
            return tt;
        }
    
    public List<MusicEntity> deleteTrack(final String title) throws ExecutionException, InterruptedException {
            final List<MusicEntity> tt ;
            tt=new AsyncTask<Void,Void,List<MusicEntity>>(){

                @Override
                protected List<MusicEntity> doInBackground(Void... voids) {
                    MusicEntity musicEntity = musicDao.findByTitle(title);
                    musicDao.Delete(musicEntity);

                    return null;
                }
            }.execute().get();
            return tt;
        }

    public void close(){
    }

    public List<PlaylistEntity> getPlaylists() throws ExecutionException, InterruptedException {
        final List<PlaylistEntity> tt ;
        tt=new AsyncTask<Void,Void,List<PlaylistEntity>>(){

            @Override
            protected List<PlaylistEntity> doInBackground(Void... voids) {
                List<PlaylistEntity> playlists = playlistDao.getAllPlaylists();

                return playlists;
            }
        }.execute().get();
        return tt;
    }
    public void addnewPlaylist(final PlaylistEntity playlistEntity) throws ExecutionException, InterruptedException {
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    playlistDao.insertAll(playlistEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;

            }
        }.execute().get();
        return;
    }
    public void deletePlaylist(final int id) throws ExecutionException, InterruptedException {
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                PlaylistEntity playlistEntity = playlistDao.getPlaylistId(id);
                playlistDao.Delete(playlistEntity);
                return null;
            }
        }.execute().get();
        return;
    }
    public void addTracktoPlaylist(final String title, final int id) throws ExecutionException, InterruptedException {
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    MusicEntity musicEntity = musicDao.findByTitle(title);
                    musicEntity.playlists |= 1 << id;
                    musicDao.Update(musicEntity);
                } catch (Exception e){
                    e.printStackTrace();
                }
                return null;
            }
        }.execute().get();
    }
    public void removeTrackfromPlaylist(final String title, final int id) throws ExecutionException, InterruptedException {
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    MusicEntity musicEntity = musicDao.findByTitle(title);
                    musicEntity.playlists &= ~(1 << id);
                    musicDao.Update(musicEntity);
                } catch (Exception e){
                    e.printStackTrace();
                }
                return null;
            }
        }.execute().get();
    }
    public List<MusicEntity> getTracksinPlaylist(final int id) throws ExecutionException, InterruptedException {

        List<MusicEntity> tracks = new AsyncTask<Void,Void,List<MusicEntity>>(){


            @Override
            protected List<MusicEntity> doInBackground(Void... voids) {
                List<MusicEntity> tracks = new ArrayList<>();
                
                List<MusicEntity> alltrack= musicDao.getAllByPopularity();
                for (MusicEntity musicEntity:alltrack){
                    if(((musicEntity.playlists >> id) & 1)==1){
                        tracks.add(musicEntity);
                    }
                }
                
                return tracks;
            }
        }.execute().get();
        return tracks;
    }
    public void importPlaylist(final String playlistname, final List<AbstractMap> playlist) throws ExecutionException, InterruptedException {
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                PlaylistEntity playlistEntity = new PlaylistEntity();
                playlistEntity.name=playlistname;

                playlistDao.insertAll(playlistEntity);
                List<PlaylistEntity> playlistEntities = playlistDao.getAllPlaylists();
                for(PlaylistEntity x: playlistEntities){
                    if(x.name.equals(playlistEntity.name)){
                        playlistEntity=x;
                    }
                }
                List<MusicEntity> musicEntities =  new ArrayList<>();
                List<MusicEntity> allmusics = musicDao.getAllMusicUnordered();

                for(AbstractMap track :playlist){
                    String title = track.get("title").toString();
                    boolean exist=false;
                    for(MusicEntity musicEntity: allmusics){
                        if(musicEntity.title.equals(title)){

                            musicEntity.playlists |= 1 << playlistEntity.id;
                            musicDao.Update(musicEntity);
                            exist=true;
                            break;
                        }
                    }
                    if(exist){
                        continue;
                    }
                    MusicEntity musicEntity = new MusicEntity();
                    musicEntity.playlists |= 1 << playlistEntity.id;
                    musicEntity.title=track.get("title").toString();
                    musicEntity.addedon=System.currentTimeMillis();
                    musicEntity.artist=track.get("author").toString();
                    musicEntity.detailJSON=gson.toJson(track);
                    musicEntity.timesPlayed=0;
                    musicEntities.add(musicEntity);
                }
                musicDao.insertAll(musicEntities.toArray(new MusicEntity[musicEntities.size()]));
                return null;
            }
        }.execute().get();
    }

}
