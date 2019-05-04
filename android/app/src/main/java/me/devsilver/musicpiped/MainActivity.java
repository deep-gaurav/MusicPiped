package me.devsilver.musicpiped;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArraySet;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.*;
import java.util.concurrent.ExecutionException;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.plugin.common.*;

public class MainActivity extends FlutterActivity {

  public static final String CHANNEL = "me.devsilver.musicpiped/PlayerChannel";
  public static final String CHANNELJSON = "me.devsilver.musicpiped/PlayerChannelJSON";
  public static final String ACTIVITY_ACTION_FILTER="me.devsilver.musicpiped/MAINACTIVITYBROADCASTFILTER";

  public static final int ACTION_STATUS_UPDATE=1;
  public static final int ACTION_CLOSE=2;
  public Gson gson;

  public PlayerService service;


  private Thread.UncaughtExceptionHandler handleAppCrash =
          new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
              StringWriter sw = new StringWriter();
              PrintWriter pw = new PrintWriter(sw);
              ex.printStackTrace(pw);
              String sStackTrace = sw.toString();
              //send email here
              Intent intent = new Intent(Intent.ACTION_SEND);
              intent.setType("text/plain");
              intent.putExtra(Intent.EXTRA_EMAIL, "deepgauravraj@gmail.com");
              intent.putExtra(Intent.EXTRA_SUBJECT, "MusicPipe App Crash Report");
              intent.putExtra(Intent.EXTRA_TEXT, "///////////\n"+sStackTrace);


              startActivity(Intent.createChooser(intent, "Email CrashLogs"));
            }
          };


  private ServiceConnection serviceConnection = new ServiceConnection(){

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

      PlayerService.isBound=true;
      service = ((PlayerService.LocalBinder)iBinder).getService();

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }
  };
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    GeneratedPluginRegistrant.registerWith(this);


    Thread.setDefaultUncaughtExceptionHandler(handleAppCrash);

    Intent intent = new Intent(this,PlayerService.class);


    //getApplicationContext().startService(intent);
    PlayerService.isBound=true;
    bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    startService(intent);
    //ADDFAVORITE
    try {
      List<PlaylistEntity> playlistEntities = new MusicDBManager(this).getPlaylists();
      if(playlistEntities.isEmpty()){
        PlaylistEntity favorite = new PlaylistEntity();
        favorite.id=1;
        favorite.name="Favorite";
        new MusicDBManager(this).addnewPlaylist(favorite);
      }
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    gson=new Gson();
    new MethodChannel(getFlutterView(),CHANNEL).setMethodCallHandler(
            new MethodChannel.MethodCallHandler() {
              @Override
              public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {

                if(methodCall.method.equals("playURL") && methodCall.hasArgument("url")){

                  int r=playMediaplayer(methodCall.argument("url").toString());
                  result.success(r);
                }
                if(methodCall.method.equals("updateQueue")){
                  if(methodCall.hasArgument("queue")){
                      List queue = methodCall.argument("queue");

                      Intent bIntent= new Intent();
                      bIntent.setAction(PlayerService.PLAYER_ACTION_FILTER);
                      bIntent.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_REPLACE_QUEUE);
                      bIntent.putExtra("queue",(Serializable)queue);
                      if(methodCall.hasArgument("index")){
                        bIntent.putExtra("index",(int)methodCall.argument("index"));
                      }
                      LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(bIntent);
                      System.out.println("BROADCASTING UPDATE QUEUE");

                  }
                  else{
                    System.out.println("Does not have queue parameter");
                  }
                }
                else if(methodCall.method.equals("addtoQueue")){
                  if(methodCall.hasArgument("queue")){
                    List queue = methodCall.argument("queue");

                    Intent bIntent= new Intent();
                    bIntent.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    bIntent.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_ADD_TO_QUEUE);
                    bIntent.putExtra("queue",(Serializable)queue);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(bIntent);
                    System.out.println("BROADCASTING UPDATE QUEUE");
                  }
                }
                else if(methodCall.method.equals("reorderqueue")){
                  Intent i = new Intent();
                  int oldpos=methodCall.argument("oldpos");
                  int newpos=methodCall.argument("newpos");
                  i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                  i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_REORDER_QUEUE);
                  i.putExtra("oldpos",oldpos);
                  i.putExtra("newpos",newpos);
                  LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                else if(methodCall.method.equals("play")){
                  System.out.println("Invoking play");
                  Intent i = new Intent();
                  i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                  i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_PLAY);
                  LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                else if(methodCall.method.equals("pause")){
                  System.out.println("Invoking Pause");
                  Intent i = new Intent();
                  i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                  i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_PAUSE);
                  LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                }
                else if(methodCall.method.equals("playIndex")){
                  if(methodCall.hasArgument("index")){  
                    int index= methodCall.argument("index");
                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_PLAY_INDEX);
                    i.putExtra("index",index);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                  }
                }
                else if(methodCall.method.equals("toggleRepeatMode")){
                  if(methodCall.hasArgument("mode")){
                    int mode= (int)methodCall.argument("mode");
                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_TOGGLE_REPEAT);
                    i.putExtra("mode",mode);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                  }
                }
                else if(methodCall.method.equals("toggleShuffle")){
                  if(methodCall.hasArgument("shuffle")){
                    boolean shuffle = (boolean)methodCall.argument("shuffle");
                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_TOGGLE_SHUFFLE);
                    i.putExtra("shuffle",shuffle);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                  }
                }
                else if(methodCall.method.equals("setSleepTimer")){
                  long sleeptime=-1;
                  if(methodCall.hasArgument("sleeptime")){
                    sleeptime = (long)methodCall.argument("sleeptime");
                  }
                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_EQ_USE_PRESET);
                    i.putExtra("sleeptime",sleeptime);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                  
                }
                else if(methodCall.method.equals("setEQPreset")){
                  if(methodCall.hasArgument("preset")){
                    Integer p = methodCall.argument("preset");
                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_EQ_USE_PRESET);
                    i.putExtra("preset",p.shortValue());
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);

                  }
                }
                else if(methodCall.method.equals("setEQ")){
                  if(methodCall.hasArgument("eqSetting")){
                    Map setting = methodCall.argument("eqSetting");

                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_SET_EQ);
                    i.putExtra("eqSetting", (Serializable) setting);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                  }
                }
                else if(methodCall.method.equals("setAutoPlay")){
                  if(methodCall.hasArgument("autoplay")){
                    boolean autoplay = methodCall.argument("autoplay");

                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_SET_AUTOPLAY);
                    i.putExtra("autoplay",autoplay);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                  }
                }
                else if(methodCall.method.equals("seekTo")){
                    if(methodCall.hasArgument("msec")){
                        long msec = ((Double) methodCall.argument("msec")).longValue();

                        Intent i = new Intent();
                        i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                        i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_SEEK);
                        i.putExtra("msec",msec);
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);

                    }
                }
                else if(methodCall.method.equals("removefromQueue")){
                  if(methodCall.hasArgument("index")){
                    int index=methodCall.argument("index");
                    Intent i = new Intent();
                    i.setAction(PlayerService.PLAYER_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_REMOVE_INDEX);
                    i.putExtra("index",index);
                    LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(i);
                  }
                }
                else if(methodCall.method.equals("getQueue")){
                  result.success(service.queue);
                }
                else if(methodCall.method.equals("requestTopTracks")){
                  List<MusicEntity> topTracks= null;
                  try {
                    topTracks = new MusicDBManager(MainActivity.this).getTopTracks();
                    System.out.print("GOT TOPTRACKS "+topTracks.size());
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                  int size=0;
                  if(methodCall.hasArgument("page")){
                      size=methodCall.argument("page");
                  }
                  List inQ=new ArrayList();
                  for(int i=size*10;i<(size+1)*10 && i<topTracks.size();i++){

                    inQ.add(gson.fromJson(topTracks.get(i).detailJSON,HashMap.class));
                  }
                  result.success(inQ);
                }
                else if(methodCall.method.equals("requestAllTracks")){
                  List<MusicEntity> topTracks= null;
                  try {
                    topTracks = new MusicDBManager(MainActivity.this).getLastAdded();
                    System.out.print("GOT TOPTRACKS "+topTracks.size());
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                    int size=0;
                    if(methodCall.hasArgument("page")){
                        size=methodCall.argument("page");
                    }
                    List inQ=new ArrayList();
                    for(int i=size*10;i<(size+1)*10 && i<topTracks.size();i++){

                        inQ.add(gson.fromJson(topTracks.get(i).detailJSON,HashMap.class));
                    }
                    result.success(inQ);
                }
                else if(methodCall.method.equals("requestHistory")){
                  List<MusicEntity> topTracks= null;
                  try {
                    topTracks = new MusicDBManager(MainActivity.this).getRecents();
                    System.out.print("GOT TOPTRACKS "+topTracks.size());
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                    int size=0;
                    if(methodCall.hasArgument("page")){
                        size=methodCall.argument("page");
                    }
                    List inQ=new ArrayList();
                    for(int i=size*10;i<(size+1)*10 && i<topTracks.size();i++){

                        inQ.add(gson.fromJson(topTracks.get(i).detailJSON,HashMap.class));
                    }
                    result.success(inQ);
                }
                else if(methodCall.method.equals("requestPopularTracks")){
                  List<MusicEntity> topTracks= null;
                  try {
                    topTracks = new MusicDBManager(MainActivity.this).getTopTracks();
                    System.out.print("GOT TOPTRACKS "+topTracks.size());
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                    int size=0;
                    if(methodCall.hasArgument("page")){
                        size=methodCall.argument("page");
                    }
                    List inQ=new ArrayList();
                    for(int i=size*10;i<(size+1)*10 && i<topTracks.size();i++){

                        inQ.add(gson.fromJson(topTracks.get(i).detailJSON,HashMap.class));
                    }
                    result.success(inQ);
                }
                else if(methodCall.method.equals("requestArtistTrack")){
                  if(methodCall.hasArgument("artistId")){
                    String artistId = methodCall.argument("artistId");

                    List<MusicEntity> topTracks= null;
                    try {
                      topTracks = new MusicDBManager(MainActivity.this).getArtistTracks(artistId);
                      System.out.print("GOT TOPTRACKS "+topTracks.size());
                    } catch (ExecutionException e) {
                      e.printStackTrace();
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                      
                      List inQ=new ArrayList();
                      for(int i=0; i<topTracks.size();i++){

                          inQ.add(gson.fromJson(topTracks.get(i).detailJSON,HashMap.class));
                      }
                      result.success(inQ);
                  }
                }
                else if(methodCall.method.equals("requestShuffled")){
                  List<MusicEntity> topTracks= null;
                  try {
                    topTracks = new MusicDBManager(MainActivity.this).getTopTracks();
                    Collections.shuffle(topTracks);
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                    int size=0;
                    if(methodCall.hasArgument("page")){
                        size=methodCall.argument("page");
                    }
                    List inQ=new ArrayList();
                    for(int i=size*10;i<(size+1)*10 && i<topTracks.size();i++){

                        inQ.add(gson.fromJson(topTracks.get(i).detailJSON,HashMap.class));
                    }
                    result.success(inQ);
                }
                else if(methodCall.method.equals("requestArtists")){
                  List<MusicEntity> topTracks= null;
                  try {
                    topTracks = new MusicDBManager(MainActivity.this).getArtists();
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                    int size=0;
                    if(methodCall.hasArgument("page")){
                        size=methodCall.argument("page");
                    }
                    List inQ=new ArrayList();
                    for(int i=size*10;i<(size+1)*10 && i<topTracks.size();i++){

                        inQ.add(gson.fromJson(topTracks.get(i).detailJSON,HashMap.class));
                    }
                    result.success(inQ);
                }
                else if(methodCall.method.equals("deletetrack")){
                  try{
                      if(methodCall.hasArgument("title")){
                      String title=(String)methodCall.argument("title");
                      new MusicDBManager(MainActivity.this).deleteTrack(title); 
                    }
                  } catch(Exception e){
                    e.printStackTrace();
                  }
                  result.success(null);
                }
                else if(methodCall.method.equals("getPlaylists")){
                  List<HashMap> outputplaylist=new ArrayList<>();
                  int size = methodCall.argument("page");
                  try{
                    List<PlaylistEntity> playlistEntities = new MusicDBManager(MainActivity.this).getPlaylists();
                    for(PlaylistEntity playlistEntity:playlistEntities){
                      HashMap playlist = new HashMap();
                      playlist.put("id",playlistEntity.id);
                      playlist.put("name",playlistEntity.name);
                      outputplaylist.add(playlist);
                    }
                  } catch(Exception e){
                    e.printStackTrace();
                  }
                  List inQ=new ArrayList();
                  for(int i=size*10;i<(size+1)*10 && i<outputplaylist.size();i++){

                      inQ.add(outputplaylist.get(i));
                  }
                  result.success(inQ);
                }
                else if(methodCall.method.equals("addTracktoPlaylist")){
                  if(methodCall.hasArgument("track") && methodCall.hasArgument("playlistId")){
                    String track =(String) methodCall.argument("track");
                    int id = (int) methodCall.argument("playlistId");

                    try{
                      new MusicDBManager(MainActivity.this).addTracktoPlaylist(track,id);
                    } catch (Exception e){
                      e.printStackTrace();
                    }
                  }
                }
                else if(methodCall.method.equals("removeTrackfromPlaylist")){
                  if(methodCall.hasArgument("track") && methodCall.hasArgument("playlistId")){
                    String track =(String) methodCall.argument("track");
                    int id = (int) methodCall.argument("playlistId");

                    try{
                      new MusicDBManager(MainActivity.this).removeTrackfromPlaylist(track,id);
                    } catch (Exception e){
                      e.printStackTrace();
                    }
                  }
                }else if(methodCall.method.equals("getTracksinPlaylist")){
                  if(methodCall.hasArgument("playlistId")){
                    int id = (int) methodCall.argument("playlistId");
                    List<MusicEntity> tracks = new ArrayList<>();

                    try{
                      List<MusicEntity> musicEntities = new MusicDBManager(MainActivity.this).getTracksinPlaylist(id);
                      tracks.addAll(musicEntities);
                    } catch (Exception e){
                      e.printStackTrace();
                    }
                    List tracksInPlaylists=new ArrayList<>();
                    for(MusicEntity musicEntity: tracks){
                      tracksInPlaylists.add(gson.fromJson(musicEntity.detailJSON,HashMap.class));
                    }
                    result.success(tracksInPlaylists);
                  }
                } else if(methodCall.method.equals("addPlaylist")){
                  if(methodCall.hasArgument("name")){
                    String playlistname = (String)methodCall.argument("name");
                    PlaylistEntity playlistEntity = new PlaylistEntity();
                    playlistEntity.name=playlistname;
                    try{
                      new MusicDBManager(MainActivity.this).addnewPlaylist(playlistEntity);
                    } catch (Exception e){
                      e.printStackTrace();
                    }
                  }
                } else if(methodCall.method.equals("removePlaylist")){
                  if(methodCall.hasArgument("playlistId")){
                    int id = (int) methodCall.argument("playlistId");
                    try {
                      new MusicDBManager(MainActivity.this).deletePlaylist(id);
                    } catch (Exception e){
                      e.printStackTrace();
                    }
                  }
                } else if(methodCall.method.equals("importPlaylist")){
                  if(methodCall.hasArgument("playlistname")&& methodCall.hasArgument("playlist")){
                    String playlistname = methodCall.argument("playlistname");
                    List<AbstractMap> playlists = methodCall.argument("playlist");
                    try {
                      new MusicDBManager(MainActivity.this).importPlaylist(playlistname,playlists);
                    } catch (Exception e){
                      e.printStackTrace();
                    }
                  }
                }
                else{
                  result.notImplemented();
                }
              }

            }
    );

    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ACTIVITY_ACTION_FILTER);
    LocalBroadcastManager.getInstance(this).registerReceiver(new ActivityBroadcastReceiver(),intentFilter);
    importOLD();
  }

  private int playMediaplayer(String url){
    MediaPlayer mediaPlayer = new MediaPlayer();
    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        mp.start();
      }
    });

    try {
      mediaPlayer.setDataSource(url);
      mediaPlayer.prepareAsync();
    }
    catch (IOException e){
      return -1;
    }

    return 1;
  }


  public class ActivityBroadcastReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
      if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_STATUS_UPDATE){

        HashMap<String,Object> jsonObject = new HashMap<>();
        if(intent.getBooleanExtra("queueupdate",false)){
          List queuejson = (List) intent.getSerializableExtra("queue");

          jsonObject.put("queue",queuejson);
          jsonObject.put("queueUpdate", true);
        } else{
          jsonObject.put("queueUpdate", false);
        }
        Boolean isplaying = intent.getBooleanExtra("isPlaying",false);
        int CurrentPlayingtime = intent.getIntExtra("currentPlayingTime",0);
        int TotalTimme = intent.getIntExtra("totalTimme",0);
        int currentIndex = intent.getIntExtra("currentIndex",0);
        int repeatStatus = intent.getIntExtra("repeatMode",0);
        boolean shuffle = intent.getBooleanExtra("shuffle",false);
        long sleeptime = intent.getLongExtra("sleeptime",-1);
        short preset = intent.getShortExtra("preset",(short)0);
        boolean autoplay = intent.getBooleanExtra("autoplay",false);

        Map<String,Integer> eqSetting = (Map<String, Integer>) intent.getSerializableExtra("eqSetting");

        try {
          jsonObject.put("isplaying",isplaying);
          jsonObject.put("currentplayingtime",CurrentPlayingtime);
          jsonObject.put("totaltime",TotalTimme);
          jsonObject.put("currentIndex",currentIndex);
          jsonObject.put("repeatMode",repeatStatus);
          jsonObject.put("shuffle",shuffle);
          jsonObject.put("sleeptime", sleeptime);
          jsonObject.put("preset",preset);
          jsonObject.put("eqSetting",eqSetting);
          jsonObject.put("autoplay",autoplay);
        } catch (Exception e) {
          e.printStackTrace();
        }
        new MethodChannel(getFlutterView(),CHANNEL).invokeMethod(
                "statusUpdate",jsonObject
                
        );
      }
      else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_CLOSE){
        finish();
      }
    }
  }

  @Override
  protected void onDestroy() {
    PlayerService.isBound=false;
    super.onDestroy();
  }

  public void importOLD(){
    if(getSharedPreferences("PlayerServicePrefsImport",MODE_PRIVATE).getBoolean("imported",false)){
      return;
    }
    else {
      new AsyncTask<Void,Void,Void>(){

        @Override
        protected Void doInBackground(Void... voids) {
          new MethodChannel(getFlutterView(),CHANNEL).invokeMethod(
                  "loading", true

          );

          String path ="/data/data/deep.ryd.rydplayer/databases/History.DB";
          String TABLE_NAME = "ALL_SONGS";
          File f=new File(path);
          if(f.exists()){
            SQLiteDatabase db = SQLiteDatabase.openDatabase(path,null,SQLiteDatabase.OPEN_READONLY);
            final Cursor cursor = db.query(TABLE_NAME,null,null,null,null,null,null);
            if(cursor!=null){
              cursor.moveToFirst();
              for ( int i =0;i<cursor.getCount();i++){
                String url = cursor.getString(cursor.getColumnIndex("url"));
                String id = url.split("\\?v=",2)[1];
                final int playlists = Integer.parseInt(cursor.getString(cursor.getColumnIndex("playlists"))) << 2;

                final String queryURL = "https://invidio.us/api/v1/videos/"+id;
                final int curi=i;

                StringRequest request = new StringRequest(
                        Request.Method.GET,
                        queryURL,
                        new Response.Listener<String>() {
                          @Override
                          public void onResponse(String response) {
                            AbstractMap json = gson.fromJson(response,HashMap.class);
                            new MusicDBManager(MainActivity.this).AddMusic(json);
                            new MusicDBManager(MainActivity.this).updateplaylist(playlists,json.get("title").toString());

                            if(curi==cursor.getCount()-1){

                                new MethodChannel(getFlutterView(),CHANNEL).invokeMethod(
                                        "loading",false
                                );
                            } else {

                                new MethodChannel(getFlutterView(),CHANNEL).invokeMethod(
                                        "loading", true

                                );
                            }
                          }
                        },
                        new Response.ErrorListener() {
                          @Override
                          public void onErrorResponse(VolleyError error) {

                          }
                        }
                );
                VolleySingleton.getInstance(MainActivity.this).addtoRequestQueue(request);
                cursor.moveToNext();
              }
              cursor.close();
            }
            //ADD PLAYLISTS
            SharedPreferences sharedPreferences = getSharedPreferences("Main2Activity",MODE_PRIVATE);
            if(sharedPreferences.contains("playlists")){
              Set<String> stringSet = sharedPreferences.getStringSet("playlists",new ArraySet<String>());
              for(String s: stringSet){
                PlaylistEntity playlistEntity = new PlaylistEntity();
                playlistEntity.id=Integer.parseInt(s.split(" ",3)[0])+2;
                playlistEntity.name=s.split(" ",3)[2];
                try {
                  new MusicDBManager(MainActivity.this).addnewPlaylist(playlistEntity);
                } catch (ExecutionException e) {
                  e.printStackTrace();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
            }
          }

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,null);

      getSharedPreferences("PlayerServicePrefsImport",MODE_PRIVATE).edit().clear().putBoolean("imported",true).commit();
    }
  }
}
