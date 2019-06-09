package deep.ryd;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;

import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.utils.Localization;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


enum PlayerState{
  Playing,Paused,Loading,Idle
}

public class MainActivity extends FlutterActivity implements AudioManager.OnAudioFocusChangeListener {

  private static final String CHANNEL = "me.devsilver.musicpiped/player";

  private MediaPlayer UMP;
  private MediaSession mediaSession;


  private String src;

  private String vidId;

  private String thumbURL;
  private Bitmap thumnb;

  private String title;

  private Timer timer;

  private String openURL;

  private String author;

  private Handler handler = new Handler();

  private MethodChannel methodChannel;

  private BecomingNoisyReceiver becomingNoisyReceiver;
  private PlayerBroadCastReceiver playerBroadCastReceiver;
  private ServiceConnection serviceConnection;

  private PlayerState state = PlayerState.Idle;

  public static PlayerService playerService;

  AudioManager audioManager;

  HttpProxyCacheServer proxyCacheServer;

  public static String CHANNEL_ID = "MusicPipedPlayer";
  public static String PLAYER_ACTION_FILTER = "musicpipedFilter";

  private File createMusicDir(){
    File musicDir = new File(getCacheDir(),"musicDir");
    if(!musicDir.exists()){
      musicDir.mkdir();
    }
    return musicDir;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    GeneratedPluginRegistrant.registerWith(this);

    becomingNoisyReceiver = new BecomingNoisyReceiver();

    NewpipeDownloader.init(null);
    NewPipe.init(NewpipeDownloader.getInstance(), new Localization("US", "EN"));

    UMP = new MediaPlayer();
    proxyCacheServer = URLProxyFactory.getProxy(this);

    createNotificationChannel();

    methodChannel = new MethodChannel(getFlutterView(), CHANNEL);

    serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder service) {
        playerService = ((PlayerService.PlayerServiceBinder) service).getService();
        setupMediaSession();

      }

      @Override
      public void onServiceDisconnected(ComponentName name) {

      }
    };

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      startForegroundService(new Intent(this, PlayerService.class));

    } else {
      startService(new Intent(this, PlayerService.class));
    }
    bindService(
            new Intent(
                    this,
                    PlayerService.class
            ),
            serviceConnection
            ,
            BIND_AUTO_CREATE
    );

    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    methodChannel.setMethodCallHandler(
            new MethodChannel.MethodCallHandler() {
              @Override
              public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
                if (methodCall.method.equals("changeSource")) {
                  try {
                    changeSource(
                            methodCall.argument("src"),
                            methodCall.argument("vidId"),
                            methodCall.argument("thumbURL"),
                            methodCall.argument("title"),
                            methodCall.argument("artist")
                    );
                    byte[] imagebytearray = methodCall.argument("thumb");
                    if (imagebytearray != null) {
                      thumnb = BitmapFactory.decodeByteArray(imagebytearray, 0, imagebytearray.length);
                    }
                  } catch (IOException e) {
                    e.printStackTrace();
                    result.error("Error", e.getMessage(), e.toString());
                  }
                } else if (methodCall.method.equals("updateMetadata")) {
                  title = methodCall.argument("title");
                  vidId = methodCall.argument("vidId");
                  author = methodCall.argument("artist");
                  byte[] imagebytearray = methodCall.argument("thumb");
                  if (imagebytearray != null) {
                    thumnb = BitmapFactory.decodeByteArray(imagebytearray, 0, imagebytearray.length);
                  }
                  setupMediaSession();
                  playerService.showNotificaion(
                          title,
                          author,
                          mediaSession.getSessionToken(),
                          false,
                          thumnb
                  );
                } else if (methodCall.method.equals("play")) {
                  play();
                } else if (methodCall.method.equals("pause")) {
                  pause();
                } else if (methodCall.method.equals("seek")) {
                  seek(methodCall.argument("position"));
                } else if (methodCall.method.equals("isCached")) {
                  String url = methodCall.argument("url");
                  Log.d("musicpiped", "Check url : " + url);
                  boolean cached = proxyCacheServer.isCached(url);
                  result.success(cached);
                  Log.d("musicpiped", "androidcached " + cached);
                } else if(methodCall.method.equals("readyOpenURL")){
                  result.success(openURL);
                }

                else {
                  result.notImplemented();
                }
              }
            }
    );

    timer = new Timer();
    timer.scheduleAtFixedRate(
            new TimerTask() {
              @Override
              public void run() {
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    updateCycle();
                  }
                });
              }
            }, 0,
            500
    );
    ;

    UMP.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        onPrepare();
      }
    });

    UMP.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {

        try {
          if ((UMP.getCurrentPosition() / UMP.getDuration()) < 0.9) {
            return;
          }
        } catch (Exception e) {
          return;
        }

        methodChannel.invokeMethod(
                "ended", ""
        );
        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0)
                .build();

        MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, author)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, author)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, thumnb)
                .build();

        mediaSession.setMetadata(metadata);
        mediaSession.setPlaybackState(state);
      }
    });

    UMP.setOnErrorListener(new MediaPlayer.OnErrorListener() {
      @Override
      public boolean onError(MediaPlayer mp, int what, int extra) {
        //methodChannel.invokeMethod("error","");
        Log.i("musicpiped", "MediPlayer Error : " + what + " " + extra);
        if (extra == MediaPlayer.MEDIA_ERROR_IO || extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
          Log.d("musicpiped", "Timeout");
          next();
        }
        return false;
      }
    });

    playerBroadCastReceiver = new PlayerBroadCastReceiver();
    registerReceiver(playerBroadCastReceiver, new IntentFilter(PLAYER_ACTION_FILTER));
    // ATTENTION: This was auto-generated to handle app links.
    Intent appLinkIntent = getIntent();
    handleURL(appLinkIntent);

  }

  void handleURL(Intent appLinkIntent){
    String appLinkAction = appLinkIntent.getAction();
    Uri appLinkData = appLinkIntent.getData();
    if(appLinkIntent.getAction().equals(Intent.ACTION_SEND)){
      Log.d("musicpiped","App started by share");
      String url = appLinkIntent.getStringExtra(Intent.EXTRA_TEXT);
      Uri uri = Uri.parse(url);
      String vidId;
      if(uri.getQueryParameter("v")!=null){
        vidId = uri.getQueryParameter("v");
      }else{
        vidId = uri.getLastPathSegment();
      }
      Log.d("musicpiped","openingVideo : "+vidId);
      openURL = vidId;
    }

    if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null){
      String vidId;
      if(appLinkData.getQueryParameter("v")!=null){
        vidId=appLinkData.getQueryParameter("v");
      }
      else {

        vidId = appLinkData.getLastPathSegment();
      }
      openURL = vidId;
    }
    else if(Intent.ACTION_SEND.equals(appLinkAction) && appLinkData !=null){
      String vidId;
      if(appLinkData.getQueryParameter("v")!=null){
        vidId=appLinkData.getQueryParameter("v");
      }
      else {

        vidId = appLinkData.getLastPathSegment();
      }
      openURL = vidId;
    }

  }

  void onPrepare(){
    UMP.start();
    setupMediaSession();
    state=PlayerState.Playing;

    play();
  }


  void updateCycle(){

    HashMap args = new HashMap();
    if(state==PlayerState.Playing){

      args.put("currentTime",UMP.getCurrentPosition()/1000);
      args.put("duration",UMP.getDuration()/1000);
      methodChannel.invokeMethod(
              "timeupdate",
              args
      );
    }
  }

  void changeSource(String src,String vidId,String thumbURL, String title, String author) throws IOException {
    this.src=src;
    this.vidId=vidId;
    this.thumbURL=thumbURL;
    this.title = title;
    this.author = author;

    UMP.stop();
    UMP.reset();
    String murl = src+"&videoId="+vidId;
    if(proxyCacheServer.isCached(murl))
    {
      playURL(murl);

    }
    else {
      AsyncTask urlVerifier = new URLVerifier(this,vidId).execute(murl);

    }
    state = PlayerState.Loading;
    methodChannel.invokeMethod("loadstart","");
  }

  public void playURL(String url) throws IOException {
    Log.d("musicpiped","non proxy URL : "+url);
    String proxiedURL = proxyCacheServer.getProxyUrl(url);
    Log.d("musicpiped","Proxy URL: "+proxiedURL);
    try {
      UMP.stop();
    }catch (Exception e){
      e.printStackTrace();
    }
    UMP.setDataSource(proxiedURL);
    UMP.prepareAsync();
  }

  void play(){
    if(state == PlayerState.Loading){
    }else{

      int result = audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
      if(result!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
        methodChannel.invokeMethod("pause","");
        return;
      }
      state = PlayerState.Playing;
      try {

        UMP.start();
      } catch (Exception e){
        e.printStackTrace();
        return;
      }
      PlaybackState state = new PlaybackState.Builder()
              .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE| PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_STOP )
              .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0)
              .build();

      MediaMetadata metadata = new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE,title)
              .putString(MediaMetadata.METADATA_KEY_TITLE,title)
              .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST,author)
              .putString(MediaMetadata.METADATA_KEY_ARTIST,author)
              .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,thumnb)
              .build();

      mediaSession.setMetadata(metadata);
      mediaSession.setPlaybackState(state);


      playerService.showNotificaion(
              title,
              author,
              mediaSession.getSessionToken(),
              true,
              thumnb
      );
      setVolumeControlStream(AudioManager.STREAM_MUSIC);

      methodChannel.invokeMethod("play","");

      registerReceiver(becomingNoisyReceiver,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

    }


  }

  void pause(){
    if(state==PlayerState.Playing) {
      try {
        if(UMP.isPlaying()){

        }else{
          return;
        }
      }catch (Exception e){
        return;
      }
      state = PlayerState.Paused;
      UMP.pause();
      methodChannel.invokeMethod("pause","");
      PlaybackState state = new PlaybackState.Builder()
              .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_STOP)
              .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0)
              .build();

      MediaMetadata metadata = new MediaMetadata.Builder()
              .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE,title)
              .putString(MediaMetadata.METADATA_KEY_TITLE,title)
              .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST,author)
              .putString(MediaMetadata.METADATA_KEY_ARTIST,author)
              .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,thumnb)
              .build();

      mediaSession.setMetadata(metadata);
      mediaSession.setPlaybackState(state);

      playerService.showNotificaion(
              title,
              author,
              mediaSession.getSessionToken(),
              false,
              thumnb
      );
      unregisterReceiver(becomingNoisyReceiver);

    }
  }

  void next(){
    UMP.stop();
    methodChannel.invokeMethod("next","");
  }

  void previous(){
    UMP.stop();
    methodChannel.invokeMethod("previous","");
  }

  void stop(){
    UMP.stop();
    state = PlayerState.Idle;
    methodChannel.invokeMethod("ended","");
    audioManager.abandonAudioFocus(this);
  }

  void seek(int position){
    UMP.seekTo(position*1000);
  }

  private void setupMediaSession() {
    //ComponentName mediaButtonReceiverComponentName = new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class);

    //Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
    //mediaButtonIntent.setComponent(mediaButtonReceiverComponentName);


    //PendingIntent mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
    if(mediaSession!=null){
      return;
    }

    mediaSession = new MediaSession(this,"musicpiped");
    mediaSession.setCallback(new MediaSession.Callback() {
      @Override
      public void onPlay() {
        Log.d("musicpiped","playbutton");
        play();
      }

      @Override
      public void onPause() {

        Log.d("musicpiped","pausebutton");
        pause();
      }

      @Override
      public void onSkipToNext() {

        Log.d("musicpiped","skipbutton");
        next();
      }

      @Override
      public void onSkipToPrevious() {

        Log.d("musicpiped","previousbutton");
        previous();
      }

      @Override
      public void onStop() {
        stop();
        Log.d("musicpiped","stopbutton");

      }

      @Override
      public void onSeekTo(long pos) {

        Log.d("musicpiped","seekbutton");
        seek((int) pos);
      }

      @Override
      public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        Log.d("musicpiped","MediaPress received");
        return super.onMediaButtonEvent(mediaButtonEvent);
      }
    });
    PlaybackState state = new PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_STOP)
            .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0)
            .build();

    mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);

    MediaMetadata metadata = new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE,title)
            .putString(MediaMetadata.METADATA_KEY_TITLE,title)
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST,author)
            .putString(MediaMetadata.METADATA_KEY_ARTIST,author)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,thumnb)
            .build();

    mediaSession.setMetadata(metadata);
    mediaSession.setPlaybackState(state);
    mediaSession.setActive(true);


    playerService.showNotificaion(title,author,mediaSession.getSessionToken(),false,thumnb);
    //mediaSession.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);
  }

  @Override
  public void onAudioFocusChange(int focusChange) {
    if(focusChange==AudioManager.AUDIOFOCUS_LOSS || focusChange== AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
      pause();
    }else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){
      play();
    }
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = "Player";
      String description = "Notification channel of Player";
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
      channel.setDescription(description);
      channel.setSound(null,null);
      NotificationManager notificationManager = getSystemService(NotificationManager.class);
      notificationManager.createNotificationChannel(channel);
    }
  }

  @Override
  protected void onDestroy() {
    try {
      timer.cancel();
      UMP.stop();
      UMP.release();
    }
    catch (Exception e){
      e.printStackTrace();
    }
    try{

      mediaSession.setActive(false);
      mediaSession.release();

    }
    catch (Exception e){
      e.printStackTrace();
    }
    try{
      unregisterReceiver(playerBroadCastReceiver);
      unregisterReceiver(becomingNoisyReceiver);
      unbindService(serviceConnection);
    }catch (Exception e){
      e.printStackTrace();
    }

    stopService(new Intent(this,PlayerService.class));
    super.onDestroy();
  }

  private class BecomingNoisyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
        pause();
      }
    }
  }


  public class PlayerBroadCastReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
      Log.d("musicpiped","broadcastreceived v: "+String.valueOf(intent.getLongExtra(Intent.ACTION_MAIN,0)));
      if(intent.getLongExtra(Intent.ACTION_MAIN,0)==PlaybackState.ACTION_PLAY){
        Log.d("musicpiped","Play");
        play();
      }
      else if(intent.getLongExtra(Intent.ACTION_MAIN,0)==PlaybackState.ACTION_PAUSE){
        Log.d("musicpiped","Pause");
        pause();
      } else if(intent.getLongExtra(Intent.ACTION_MAIN,0)==PlaybackState.ACTION_SKIP_TO_NEXT){
        Log.d("musicpiped","Next");
        next();
      } else if(intent.getLongExtra(Intent.ACTION_MAIN,0)==PlaybackState.ACTION_SKIP_TO_PREVIOUS){
        Log.d("musicpiped","Previous");
        previous();
      }
    }
  }

}

 class URLVerifier extends AsyncTask<String,String,String>{

  String videoId;
  MainActivity activity;

  URLVerifier(MainActivity context,String videoId){
    this.activity=context;
    this.videoId=videoId;
  }

  String getNewpipeURL() throws ExtractionException, IOException {

    Log.d("musicpiped","Get newpipe URL");

    String youtubeURL="https://www.youtube.com/watch?v=";

    String url = youtubeURL+videoId;

    YoutubeService youtubeService = (YoutubeService) NewPipe.getService(NewPipe.getIdOfService("YouTube"));
    StreamExtractor streamExtractor = youtubeService.getStreamExtractor(url);
    streamExtractor.fetchPage();
    if(streamExtractor.getAudioStreams().size()>1)
    return streamExtractor.getAudioStreams().get(0).url;
    else if(streamExtractor.getVideoStreams().size()>1){
      return streamExtractor.getVideoStreams().get(0).url;
    }
    else {
      return "ERROR";
    }
  }

  boolean verifyURL (String url){
    OkHttpClient client = new OkHttpClient();

    Log.d("musicpiped","Checking Response Code");

    boolean needrefetch = true;

    Request request = new Request.Builder()
            .url(url)
            .head()
            .build();
    try {
      Response response = client.newCall(request).execute();
      Log.d("musicpiped","Response Code : "+response.code());
      if(response.code()==200){
        Log.d("musicpiped","Response OK");
        needrefetch = false;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return  needrefetch;
  }

   @Override
   protected String doInBackground(String... strings) {

     String url = strings[0];

     Log.d("musicpiped","CheckResponseCode");
     while (verifyURL(url)){
       try {
         url = getNewpipeURL();
       } catch (ExtractionException e) {
         e.printStackTrace();
       } catch (IOException e) {
         e.printStackTrace();
       }
     }
     return url;
   }

   @Override
   protected void onPostExecute(String s) {
     super.onPostExecute(s);
     if(s=="ERROR"){
       Toast.makeText(activity,"Can not play that track, Retry late",Toast.LENGTH_LONG);
       activity.next();
     }else{
       try {
         activity.playURL(s+"&videoId="+videoId);
       } catch (IOException e) {
         activity.next();
       }
     }
   }
 }
