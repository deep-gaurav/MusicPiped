package deep.ryd.rydplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import android.util.Log;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeChannelExtractor;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubePlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.IOException;
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
        playlistname="";
        for(int i=2;i<parts.length;i++){
            playlistname+=" "+parts[i];
        }
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

    public static int newPlaylist(String name,Main2Activity main2Activity){
        SharedPreferences sharedPreferences = main2Activity.getPreferences( Context.MODE_PRIVATE);
        Set<String> oldplaylists= sharedPreferences.getStringSet("playlists",new ArraySet<String>());
        totalplaylists = oldplaylists.size();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int playlistnewid = totalplaylists;
        oldplaylists.add(playlistnewid+" "+ totalplaylists + " " + name);
        editor.clear();
        editor.putStringSet("playlists",oldplaylists);
        editor.commit();

        Log.i("ryd","NEW PLAYLIST SAVED "+oldplaylists);
        main2Activity.mSectionsPagerAdapter.refresh();
        return playlistnewid;
    }
    public static void importPlaylist(final String url, final Main2Activity main2Activity){
        class ImportDownloader extends AsyncTask<Void,Void,Void>{

            @Override
            protected Void doInBackground(Void... voids) {
                int sid = NewPipe.getIdOfService("YouTube");
                Downloader.init(null);
                NewPipe.init(Downloader.getInstance());
                YoutubeService ys= null;
                try {
                    ys = (YoutubeService) NewPipe.getService(sid);
                } catch (ExtractionException e) {
                    e.printStackTrace();
                }

                try {
                    main2Activity.setProgressVisible();
                    YoutubePlaylistExtractor ype = (YoutubePlaylistExtractor)ys.getPlaylistExtractor(url);
                    ype.fetchPage();
                    ListExtractor.InfoItemsPage<StreamInfoItem> page=ype.getInitialPage();
                    List<StreamInfoItem> pl= page.getItems();
                    int pid=newPlaylist(ype.getName(),main2Activity);

                    DBManager dbManager = main2Activity.playerService.dbManager;
                    dbManager.open();
                    int i=0;
                    for(StreamInfoItem s:pl){

                        //CHECK ARTIST EXIST
                        List<StreamInfo> artistlist=dbManager.artistlists(s.getUploaderUrl());
                        if(artistlist.size()>0){
                            dbManager.addSong(s.getName(),s.getUrl(),s.getUploaderName(),s.getThumbnailUrl(),artistlist.get(0).getUploaderAvatarUrl(),s.getUploaderUrl(),"NOTING REUPDATE PLZ");
                        }
                        else {
                            StreamInfo streamInfo = StreamInfo.getInfo(ys, s.getUrl());
                            dbManager.addSong(streamInfo.getName(), streamInfo.getUrl(), streamInfo.getUploaderName(), streamInfo.getThumbnailUrl(), streamInfo.getUploaderAvatarUrl(), streamInfo.getUploaderUrl(), streamInfo.getAudioStreams().get(0).getUrl());
                        }
                        dbManager.addtoPlaylist(s.getUrl(),pid);
                        main2Activity.setProgressIndicator(i*100/pl.size());
                        i++;
                    }
                    dbManager.close();
                } catch (ExtractionException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    main2Activity.mSectionsPagerAdapter.refresh();
                    main2Activity.setProgressInvisible();
                }

                return null;
            }
        }
        new ImportDownloader().execute();
    }


    @NonNull
    @Override
    public String toString() {
        return playlistnumber+" "+playlistid+" "+playlistname;
    }
}
