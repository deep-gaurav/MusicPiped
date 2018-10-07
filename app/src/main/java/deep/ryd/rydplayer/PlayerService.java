package deep.ryd.rydplayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerService extends Service {

    public MediaPlayer umP;
    public StreamInfo streamInfo;
    public MusicServiceBinder mBinder = new MusicServiceBinder();
    public Activity launch;

    static PlayerService mainobj;

    public boolean isLooping=false;

    public  int ID=1;
    private String CHANNEL_ID = "player";

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    public Bitmap thumbnail;
    public boolean isuMPready=false;
    PendingIntent launchIntent;

    public void control_MP( String action ) {

        if( action.equalsIgnoreCase( ACTION_PLAY ) ) {
            //mediaController.getTransportControls().play();
        } else if( action.equalsIgnoreCase( ACTION_PAUSE ) ) {
            //mediaController.getTransportControls().pause();
        } else if( action.equalsIgnoreCase( ACTION_FAST_FORWARD ) ) {
            //mediaController.getTransportControls().fastForward();
        } else if( action.equalsIgnoreCase( ACTION_REWIND ) ) {
            //mediaController.getTransportControls().rewind();
        } else if( action.equalsIgnoreCase( ACTION_PREVIOUS ) ) {
            //mediaController.getTransportControls().skipToPrevious();
        } else if( action.equalsIgnoreCase( ACTION_NEXT ) ) {
            //mediaController.getTransportControls().skipToNext();
        } else if( action.equalsIgnoreCase( ACTION_STOP ) ) {
            //mediaController.getTransportControls().stop();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainobj=this;
        createNotificationChannel();
        Intent intent = new Intent(this, Main2Activity.class);
        intent.putExtra("some data", "txt");  // for extra data if needed..

        Random generator = new Random();

        launchIntent=PendingIntent.getActivity(this, generator.nextInt(), intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void start(){

        buildNotification(ID);
        umP.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        buildNotification(ID);
                        if(isLooping)
                            umP.start();
                    }
                }

        );
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(umP.isPlaying()) {
                    NotificationCompat.Builder builder = notifbuilder();

                    NotificationManagerCompat.from(PlayerService.this).notify(ID, builder.build());
                }
            }
        },500,500);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //launch=intent.getExtras();
        //startService(new Intent(getApplicationContext(),getClass()));
        return mBinder;
    }

    class MusicServiceBinder extends Binder{

        public PlayerService getPlayerService(){
            return  PlayerService.this;
        }



    }
    public void play() throws IOException {
        umP.reset();
        umP.setDataSource(streamInfo.getAudioStreams().get(0).getUrl());
        umP.prepareAsync();
        umP.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                umP.start();
            }
        });
        start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("ryd","SERVICE DESTROYED");
    }

    public void CreateToast(String toast){
        Toast.makeText(PlayerService.this, toast, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(umP==null)
            umP=new MediaPlayer();
        //Toast.makeText(this, "Service Created", Toast.LENGTH_SHORT).show();
        Log.i("ryd","Service Started");
        return super.onStartCommand(intent, flags, startId);

    }


    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void buildNotification( int id){
        // Given a media session and its context (usually the component containing the session)
// Create a NotificationCompat.Builder

// Get the session's metadata

        NotificationCompat.Builder builder = notifbuilder();


                //.setSubText(description.getDescription()

// Display the notification and place the service in the foreground
        startForeground(id, builder.build());
    }

    public NotificationCompat.Builder notifbuilder(){
        Context context = this;
        Random generator = new Random();

        Intent intent=new Intent(context,ButtonReceiver.class);
        intent.putExtra("action","Close");

        PendingIntent closepIntent = PendingIntent.getBroadcast(context,generator.nextInt(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent1= new Intent(context,ButtonReceiver.class);
        intent.putExtra("action","Pause");
        PendingIntent pauseIntent = PendingIntent.getBroadcast(context,generator.nextInt(),intent,PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);

        contentView.setTextViewText(R.id.notifTitle,streamInfo.getName());
        contentView.setTextColor(R.id.notifTitle,Color.DKGRAY);
        contentView.setTextViewText(R.id.notifText,streamInfo.getUploaderName());
        contentView.setTextColor(R.id.notifText,Color.LTGRAY);
        contentView.setImageViewBitmap(R.id.notifThumb,thumbnail);
        contentView.setImageViewResource(R.id.prevButton,android.R.drawable.ic_media_previous);
        contentView.setImageViewResource(R.id.nextButton,android.R.drawable.ic_media_next);
        contentView.setTextColor(R.id.notifTimer,Color.LTGRAY);

        contentView.setOnClickPendingIntent(R.id.play_pause_notif,pauseIntent);

        if(isuMPready){
            contentView.setTextViewText(R.id.notifTimer,core.sectotime(umP.getCurrentPosition(),true)+"/"+core.sectotime(umP.getDuration(),true));

            contentView.setProgressBar(R.id.notifProgress, (int) umP.getDuration(), umP.getCurrentPosition(), false);
        }
        else {
            contentView.setTextViewText(R.id.notifTimer,"0:00"+"/"+"0:00");

            contentView.setProgressBar(R.id.notifProgress, 0, 0, true);
        }

        if(umP.isPlaying()) {
            contentView.setImageViewResource(R.id.play_pause_notif, android.R.drawable.ic_media_pause);
        }
        else {
            contentView.setImageViewResource(R.id.play_pause_notif, android.R.drawable.ic_media_play);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);

        builder
                .setSmallIcon(android.R.drawable.ic_media_play)
                // Add the metadata for the currently playing track
                //.setContentTitle(streamInfo.getName())
                //.setContentText(streamInfo.getUploaderName())
                //.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(launchIntent)
                .setCustomContentView(contentView)
                .setCustomBigContentView(contentView)
                .setOnlyAlertOnce(true);

        return builder;
    }

    public void createNotificationChannel(){
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

    public static class ButtonReceiver extends BroadcastReceiver{

        public ButtonReceiver(){
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i("ryd",intent.getExtras().toString());

            String action = intent.getStringExtra("action");
            Log.i("ryd","ACTION "+action);
            if(action.equals("Close")){
                mainobj.stopForeground(true);
                mainobj.umP.pause();
            }
            else if (action.equals("Pause")) {
                if(mainobj.umP.isPlaying()) {
                    mainobj.umP.pause();
                    mainobj.buildNotification(mainobj.ID);
                    mainobj.stopForeground(false);
                }
                else {
                    mainobj.umP.start();
                    mainobj.buildNotification(mainobj.ID);
                }
            }

        }
    }
}


