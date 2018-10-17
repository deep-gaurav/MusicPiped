package deep.ryd.rydplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Playlist {
    public String playlistname;
    public int playlistid;
    public int playlistnumber;

    public static int totalplaylists=0;
    public static int playlistlastid=0;

    public Playlist(String from){
        totalplaylists++;
        String parts[]=from.split(" ");
        playlistnumber=Integer.parseInt(parts[0]);
        playlistid=Integer.parseInt(parts[1]);
        playlistname=parts[2];
        playlistlastid=playlistid;
    }

    public static List<Playlist> loadfromSharedPreference(Main2Activity main2Activity){
        totalplaylists=0;
        SharedPreferences sharedPreferences = main2Activity.getPreferences( Context.MODE_PRIVATE);
        Set<String> playlistset= sharedPreferences.getStringSet("playlists", new ArraySet<String>());
        Log.i("ryd","PLAYLISTS FOUND "+playlistset.size());
        main2Activity.playlists.clear();
        for(String x: playlistset){
            main2Activity.playlists.add(new Playlist(x));
        }
        return main2Activity.playlists;
    }

    public static void newPlaylist(String name,Main2Activity main2Activity){
        int playlistnewid = (int) Math.pow(2,totalplaylists);
        SharedPreferences sharedPreferences = main2Activity.getPreferences( Context.MODE_PRIVATE);
        Set<String> oldplaylists= sharedPreferences.getStringSet("playlists",new ArraySet<String>());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        oldplaylists.add(playlistnewid+" "+ ++totalplaylists + " " + name);
        editor.clear();
        editor.putStringSet("playlists",oldplaylists);
        editor.commit();

        Log.i("ryd","NEW PLAYLIST SAVED "+oldplaylists);
        main2Activity.mSectionsPagerAdapter.refresh();
    }

    @NonNull
    @Override
    public String toString() {
        return playlistnumber+" "+playlistid+" "+playlistname;
    }
}
