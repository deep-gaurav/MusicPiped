package me.devsilver.musicpiped;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArraySet;
import android.widget.ImageView;
import android.widget.RemoteViews;

import android.support.v4.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.utils.Localization;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;


public class PlayerService extends Service {
    public static int ACTION_STOP=0;
    public static int ACTION_PLAY=1;
    public static int ACTION_PAUSE=2;
    public static int ACTION_REPLACE_QUEUE=3;
    public static int ACTION_ADD_TO_QUEUE=4;
    public static int ACTION_PLAY_INDEX=5;
    public static int ACTION_TOGGLE_REPEAT=6;
    public static int ACTION_REORDER_QUEUE=7;
    public static int ACTION_REMOVE_INDEX=8;
    public static int ACTION_TOGGLE_SHUFFLE=9;
    public static int ACTION_SEEK=10;

    public static String PLAYER_ACTION_FILTER="me.devsilver.musicpiped.playerservice.mainAction";


    private String CHANNEL_ID ="PLAYERNOTIFICATION";
    private static MediaPlayer UMP;

    private List queue;

    private int currentIndex=-1;

    private Bitmap notificationImage;

    private ImageView thumbstore;
    private int repeatMode=0;
    private boolean shuffle=false;

    private int Playerstate=0;

    private Timer t;

    private AudioManager audioManager;

    public static boolean isBound=false;

    MusicDBManager musicDBManager;

    HttpProxyCacheServer proxyCacheServer;
    Gson gson;

    BroadcastReceiver broadcastReceiver;

    private AsyncTask refresher;
    private boolean focusOn;

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


    @Override
    public void onCreate() {

        Thread.setDefaultUncaughtExceptionHandler(handleAppCrash);
        if(UMP!=null){
            stopSelf();
        }
        UMP = new MediaPlayer();
        thumbstore=new ImageView(this);


        gson =new Gson();
        createNotificationChannel();

        musicDBManager = new MusicDBManager(this);

        proxyCacheServer = URLProxyFactory.getProxy(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAYER_ACTION_FILTER);

        broadcastReceiver=new ServiceBroadCastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,intentFilter);
        registerReceiver(new ServiceBroadCastReceiver(),intentFilter);
        System.out.println("SERVICE STARTED");


        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(queue!=null && queue.size()>0){
                    buildNotification(1);

                    BroadcastUpdate(false);
                }
            }
        },0,1000);

        if(queue==null){
            queue=new ArrayList();
        }

        SharedPreferences preferences = getSharedPreferences("PlayerServicePrefs",Context.MODE_PRIVATE);

        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);


        if (preferences.contains("queue")){
            try {

                currentIndex = preferences.getInt("index",0);
                shuffle=preferences.getBoolean("shuffle",false);
                repeatMode = preferences.getInt("repeatMode",0);
                Set<String> stringSet=preferences.getStringSet("queue",null);
                String strings[] = new String[stringSet.size()];
                for(String s:stringSet){
                    int i = Integer.parseInt(s.split(";",2)[0]);
                    strings[i]=(s.split(";",2)[1]);
                }
                for(String tracks:strings){

                    queue.add(gson.fromJson(tracks,HashMap.class));

                }
                //playFromQueue();
            } catch (Exception e){
                e.printStackTrace();
            }

        }
        BroadcastUpdate(true);
        super.onCreate();

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        BroadcastUpdate(true);
        return super.onStartCommand(intent, flags, startId);
    }

    private void BroadcastUpdate(Boolean full){
        if(!isBound){
            return;
        }
        //SEND BROADCAST
        Intent intent1 = new Intent();
        intent1.setAction(MainActivity.ACTIVITY_ACTION_FILTER);

        intent1.putExtra(Intent.ACTION_MAIN,MainActivity.ACTION_STATUS_UPDATE);
        if(full){

            intent1.putExtra("queue",(Serializable) queue);
            intent1.putExtra("queueupdate",true);
        }else{
            intent1.putExtra("queueupdate",false);
        }
        intent1.putExtra("isPlaying",UMP.isPlaying());
        try {
            intent1.putExtra("currentPlayingTime", UMP.getCurrentPosition());
            intent1.putExtra("totalTimme", UMP.getDuration());
        }
        catch (Exception e){
            e.printStackTrace();
        }
        intent1.putExtra("currentIndex",currentIndex);
        intent1.putExtra("repeatMode",repeatMode);
        intent1.putExtra("shuffle",shuffle);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
    }

    @Override
    public void onDestroy() {

        musicDBManager.close();
        t.cancel();
        t.purge();
        UMP.stop();
        UMP.reset();
        UMP.release();
        UMP=null;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);

        SharedPreferences preferences = getSharedPreferences("PlayerServicePrefs",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.putInt("index",currentIndex);
        Set<String> stringSet = new ArraySet<>();
        for(int i=0;i<queue.size();i++){
            stringSet.add(i+";"+(gson.toJson(queue.get(i))));
        }
        editor.putStringSet("queue",stringSet);
        editor.putBoolean("shuffle",shuffle);
        editor.putInt("repeatMode",repeatMode);
        editor.apply();
        super.onDestroy();
    }

    public class LocalBinder extends Binder{
        PlayerService getService(){
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        if (queue.size() > 0) {
            BroadcastUpdate(true);
        }
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isBound=false;
        return super.onUnbind(intent);
    }

    private void playFromQueue(){
        try {
            final AbstractMap abstractMap =(AbstractMap) queue.get(currentIndex);
            final List formats = (List)abstractMap.get("adaptiveFormats");
            List thummbnails= (List)abstractMap.get("videoThumbnails");
            UMP.stop();
            for(int i=0;i<formats.size();i++){
                final AbstractMap cf= (AbstractMap) formats.get(i);
                if(cf.get("type").toString().contains("audio")){
                    UMP.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            Playerstate=1;
                            final int curI = currentIndex;
                            AbstractMap music=(AbstractMap)queue.get(currentIndex);
                            if(!focusOn){
                                focusOn=true;
                                audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                                    @Override
                                    public void onAudioFocusChange(int i) {

                                        if(i==AudioManager.AUDIOFOCUS_LOSS || i==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                                            focusOn=false;
                                            try {
                                                UMP.pause();
                                                scrobble(
                                                        PlayerService.this,
                                                        ((AbstractMap)queue.get(currentIndex)).get("author").toString(),
                                                        ((AbstractMap)queue.get(currentIndex)).get("title").toString(),
                                                        UMP.getDuration(),
                                                        2
                                                );
                                            } catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                },AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
                            }
                            if(music!=null)
                                musicDBManager.AddMusic(music);
                            try{

                                scrobble(
                                    PlayerService.this,
                                    abstractMap.get("author").toString(),
                                    abstractMap.get("title").toString(),
                                    UMP.getDuration(),
                                    0
                                );
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            mp.start();

                            BroadcastUpdate(true);

                        }
                    });
                    UMP.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            try{
                                scrobble(
                                    PlayerService.this,
                                    ((AbstractMap)queue.get(currentIndex)).get("author").toString(),
                                    ((AbstractMap)queue.get(currentIndex)).get("title").toString(),
                                    UMP.getDuration(),
                                    3
                                );
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                            if(Playerstate!=0)
                                handleNext();
                            
                            Playerstate=0;

                        }
                    });

                    Playerstate=0;

                    if (proxyCacheServer.isCached(cf.get("url").toString()+"&videoId="+abstractMap.get("videoId").toString())){
                        UMP.stop();

                        try {
                            UMP.reset();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        UMP.setDataSource(proxyCacheServer.getProxyUrl(cf.get("url").toString()+"&videoId="+abstractMap.get("videoId").toString()));
                        UMP.prepareAsync();
                    }
                    else {
                        if(refresher!=null){
                            refresher.cancel(true);
                        }
                        refresher=new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... voids) {
                                try {


                                    URL url = new URL(cf.get("url").toString());
                                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                    connection.setRequestMethod("GET");
                                    connection.connect();

                                    int responseCode = connection.getResponseCode();

                                    if (responseCode != 200) {
                                        Downloader.init(null);
                                        NewPipe.init(Downloader.getInstance(),new Localization("US","IN"));
                                        int sid =NewPipe.getIdOfService("YouTube");
                                        YoutubeService ys = (YoutubeService) NewPipe.getService(sid);
                                        String youtubeURL="https://www.youtube.com/watch?v=";
                                        String vidId=(String) abstractMap.get("videoId");
                                        StreamInfo streamInfo = StreamInfo.getInfo(ys,youtubeURL+vidId);
                                        cf.put("url",streamInfo.getAudioStreams().get(0).getUrl());
                                        playFromQueue();
                                    } else {
                                        UMP.stop();
                                        try {
                                            UMP.reset();
                                        } catch (Exception e){
                                            e.printStackTrace();
                                        }
                                        UMP.setDataSource(proxyCacheServer.getProxyUrl(cf.get("url").toString()+"&videoId="+abstractMap.get("videoId").toString()));
                                        UMP.prepareAsync();

                                    }

                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ExtractionException e) {
                                    e.printStackTrace();
                                }
                                return null;

                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                refresher=null;
                                super.onPostExecute(aVoid);
                            }
                        };
                        refresher.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,null);
                    }

                    break;
                }
            }
            for(int i=0;i<thummbnails.size();i++){
                if(((AbstractMap)thummbnails.get(i)).get("quality").equals("default")){
                    Picasso.get().load(((AbstractMap)thummbnails.get(i)).get("url").toString())
                            .into(thumbstore);
                    break;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void handleNext(){
        System.out.println("NEXT CALLED");
        if(shuffle){
            currentIndex=Math.abs(new Random().nextInt()%queue.size());
            playFromQueue();
        }
        else {
            if (repeatMode == 0) {
                if (currentIndex < queue.size() - 1) {
                    currentIndex++;
                    playFromQueue();
                }
            } else if (repeatMode == 1) {
                if (currentIndex == queue.size() - 1) {
                    currentIndex = 0;
                } else {
                    currentIndex++;
                }
                playFromQueue();
            } else if (repeatMode == 2) {
                playFromQueue();
            }
        }
    }



    public class ServiceBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            {
                if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_PLAY){
                    try{
                        if(Playerstate==1)
                        {
                            UMP.start();
                            scrobble(
                                    PlayerService.this,
                                    ((AbstractMap)queue.get(currentIndex)).get("author").toString(),
                                    ((AbstractMap)queue.get(currentIndex)).get("title").toString(),
                                    UMP.getDuration(),
                                    1
                            );
                        }

                        else {
                            playFromQueue();
                        }
                    } catch (Exception e){
                        playFromQueue();
                        e.printStackTrace();
                    }

                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_PAUSE){
                    try{
                        UMP.pause();
                        scrobble(
                                PlayerService.this,
                                ((AbstractMap)queue.get(currentIndex)).get("author").toString(),
                                ((AbstractMap)queue.get(currentIndex)).get("title").toString(),
                                UMP.getDuration(),
                                2
                        );
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_PLAY_INDEX){
                    try{
                        int i=intent.getIntExtra("index",currentIndex);
                        currentIndex=i;
                        playFromQueue();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_TOGGLE_REPEAT){
                    try{
                        repeatMode=intent.getIntExtra("mode",0);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_TOGGLE_SHUFFLE){
                    try {
                        shuffle=intent.getBooleanExtra("shuffle",false);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_SEEK){
                    try {
                        UMP.seekTo((int)intent.getLongExtra("msec",UMP.getCurrentPosition()));
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                BroadcastUpdate(false);
            }
            {
                if(intent.getIntExtra(Intent.ACTION_MAIN,0)==0){
                    UMP.stop();
                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_REPLACE_QUEUE){
                    try {
                        queue=(List)(intent.getSerializableExtra("queue"));
                        currentIndex=intent.getIntExtra("index",0);
                        playFromQueue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_ADD_TO_QUEUE){
                    try{
                        queue.addAll(((List)(intent.getSerializableExtra("queue"))));

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }


                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_REORDER_QUEUE){
                    try{
                        int oldpos=intent.getIntExtra("oldpos",0);
                        int newpos=intent.getIntExtra("newpos",0);
                        Object ob = queue.remove(oldpos);
                        if(oldpos==currentIndex){
                            currentIndex=newpos;
                        }
                        else if(newpos==currentIndex){
                            currentIndex=oldpos;
                        }
                        queue.add(newpos, ob);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else if(intent.getIntExtra(Intent.ACTION_MAIN,0)==ACTION_REMOVE_INDEX){
                    try{
                        int indextoRemove=intent.getIntExtra("index",0);
                        if(currentIndex==indextoRemove){
                            if(queue.size()-1==currentIndex){
                                if(currentIndex==0){
                                    UMP.stop();
                                }
                                else{
                                    currentIndex-=1;
                                }
                            }
                            else{
                                currentIndex+=1;
                            }
                            if(UMP.isPlaying()){
                                playFromQueue();
                            }
                            else{
                                UMP.stop();
                            }
                        }
                        else if(currentIndex>indextoRemove){
                            currentIndex-=1;
                        }
                        queue.remove(indextoRemove);

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

                BroadcastUpdate(true);

            }

            if(intent.hasExtra("action")){
                String action=intent.getStringExtra("action");

                if(action.equals("Pause")){
                    if(UMP.isPlaying()){
                        UMP.pause();
                        try{
                            scrobble(
                                    PlayerService.this,
                                    ((AbstractMap)queue.get(currentIndex)).get("author").toString(),
                                    ((AbstractMap)queue.get(currentIndex)).get("title").toString(),
                                    UMP.getDuration(),
                                    2
                            );
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            if(Playerstate==1){
                                UMP.start();
                                scrobble(
                                        PlayerService.this,
                                        ((AbstractMap)queue.get(currentIndex)).get("author").toString(),
                                        ((AbstractMap)queue.get(currentIndex)).get("title").toString(),
                                        UMP.getDuration(),
                                        1
                                );
                            } else {
                                playFromQueue();
                            }
                        } catch (Exception e){
                            playFromQueue();
                        }
                    }
                } else if(action.equals("Next")){
                    handleNext();
                } else if(action.equals("Previous")){
                    if(currentIndex>0){
                        currentIndex--;
                        playFromQueue();
                    }
                } else if(action.equals("Close")){
                    Intent i = new Intent();
                    i.setAction(MainActivity.ACTIVITY_ACTION_FILTER);
                    i.putExtra(Intent.ACTION_MAIN,MainActivity.ACTION_CLOSE);
                    LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(i);

                    stopForeground(true);
                    musicDBManager.close();
                    stopSelf();
                }
            }

        }
    }


    public void buildNotification(int id) {

        NotificationCompat.Builder builder = null;

        try {
            builder = notifbuilder();
            if (builder != null)
                startForeground(id, builder.build());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public NotificationCompat.Builder notifbuilder() {

        Context context = this;
        Random generator = new Random();

        Intent activitylauncher = new Intent(PlayerService.this,MainActivity.class);
        PendingIntent activitypendingIntent = PendingIntent.getActivity(this,0,activitylauncher,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent = new Intent();
        intent.setAction(PLAYER_ACTION_FILTER);
        intent.putExtra("action", "Close");

        PendingIntent closepIntent = PendingIntent.getBroadcast(context, generator.nextInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent2 = new Intent();
        intent2.setAction(PLAYER_ACTION_FILTER);
        intent2.putExtra("action", "Next");
        PendingIntent nextIntent = PendingIntent.getBroadcast(context, generator.nextInt(), intent2, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent3 = new Intent();
        intent3.setAction(PLAYER_ACTION_FILTER);
        intent3.putExtra("action", "Previous");
        PendingIntent previousIntent = PendingIntent.getBroadcast(context, generator.nextInt(), intent3, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);


        contentView.setImageViewResource(R.id.prevButton, R.drawable.ic_skip_previous_white_24dp);
        contentView.setImageViewResource(R.id.nextButton, R.drawable.ic_skip_next_white_24dp);
        contentView.setImageViewResource(R.id.closeButton, R.drawable.ic_close_white_24dp);
        if (thumbstore != null && thumbstore.getDrawable() != null) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) thumbstore.getDrawable();
            notificationImage = bitmapDrawable.getBitmap();
            notificationImage = addGradient(notificationImage,Color.WHITE,Color.BLACK);
        }
        contentView.setImageViewBitmap(R.id.notifThumb,notificationImage);
        contentView.setTextColor(R.id.notifTimer, Color.LTGRAY);

        contentView.setTextViewText(R.id.notifTitle, ((AbstractMap)queue.get(currentIndex)).get("title").toString());
        contentView.setTextColor(R.id.notifTitle, Color.DKGRAY);
        contentView.setTextViewText(R.id.notifText, ((AbstractMap)queue.get(currentIndex)).get("author").toString());
        contentView.setTextColor(R.id.notifText, Color.LTGRAY);

        contentView.setOnClickPendingIntent(R.id.closeButton, closepIntent);
        contentView.setOnClickPendingIntent(R.id.nextButton, nextIntent);
        contentView.setOnClickPendingIntent(R.id.prevButton, previousIntent);


        Intent intent1 = new Intent();
        intent1.setAction(PLAYER_ACTION_FILTER);

        if (Playerstate==1) {
            contentView.setTextViewText(R.id.notifTimer, sectotime(UMP.getCurrentPosition()) + "/" + sectotime(UMP.getDuration()));

            contentView.setProgressBar(R.id.notifProgress, (int) UMP.getDuration(), (int) UMP.getCurrentPosition(), false);
        } else {
            contentView.setTextViewText(R.id.notifTimer, "0:00" + "/" + "0:00");

            contentView.setProgressBar(R.id.notifProgress, 0, 0, true);
        }


        if (UMP.isPlaying()) {

            intent1.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_PAUSE);
            contentView.setImageViewResource(R.id.play_pause_notif, R.drawable.ic_pause_white_24dp);
        } else {

            intent1.putExtra(Intent.ACTION_MAIN,PlayerService.ACTION_PLAY);
            contentView.setImageViewResource(R.id.play_pause_notif, R.drawable.ic_play_arrow_white_24dp);
        }

        PendingIntent pauseIntent = PendingIntent.getBroadcast(context, generator.nextInt(), intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.play_pause_notif,pauseIntent);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        builder
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setCustomBigContentView(contentView)
                .setCustomContentView(contentView)
                .setContentIntent(activitypendingIntent)
                .setOnlyAlertOnce(true);


        return builder;
    }

    public void createNotificationChannel() {
        String NOTIFICATION_CHANNEL_ID = CHANNEL_ID;
        String channelName = "My Background Service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }
    }
    public String sectotime(int sec){
        sec=sec/1000;
        Integer minutes=(int)sec / 60;
        Integer leftsecons=(int)sec%60;
        String time;
        if(leftsecons<10)
            time=minutes.toString()+":0"+leftsecons.toString();
        else
            time=minutes.toString()+":"+leftsecons.toString();

        return time;

    }
    public Bitmap addGradient(Bitmap src, int color1, int color2)
    {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(src, 0, 0, null);

        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0,0,w,0, color1, color2, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        canvas.drawRect(0,0, (float) (w),h,paint);

        return result;
    }
    public static void scrobble(Context context, String uploadername, String name,int duration, int type) {
        int START = 0;
        int RESUME = 1;
        int PAUSE = 2;
        int COMPLETE = 3;

        Intent bCast = new Intent("com.adam.aslfms.notify.playstatechanged");
        bCast.putExtra("state", type);
        bCast.putExtra("app-name", "Music Piped");
        bCast.putExtra("app-package", "deep.ryd.rydplayer");
        bCast.putExtra("artist", uploadername);
        bCast.putExtra("track", name);
        bCast.putExtra("duration", duration);
        context.sendBroadcast(bCast);


    }
}
