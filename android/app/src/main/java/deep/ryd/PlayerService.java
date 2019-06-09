package deep.ryd;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

public class PlayerService extends Service {

  @Override
  public void onCreate() {
    super.onCreate();
    MainActivity.playerService = this;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    showNotificaion("","",null,false,null);
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return new PlayerServiceBinder();
  }

  public Notification.Builder notifBuilder(String title, String author, MediaSession.Token mediasessiontoken, boolean playing, Bitmap thumbnail){

    Intent playintent = new Intent(MainActivity.PLAYER_ACTION_FILTER);
    playintent.putExtra(Intent.ACTION_MAIN, PlaybackState.ACTION_PLAY);
    PendingIntent playpendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            playintent,
            PendingIntent.FLAG_UPDATE_CURRENT
    );

    Intent pauseintent = new Intent(MainActivity.PLAYER_ACTION_FILTER);
    pauseintent.putExtra(Intent.ACTION_MAIN,PlaybackState.ACTION_PAUSE);
    PendingIntent pausependingintent = PendingIntent.getBroadcast(
            this,
            2,
            pauseintent,
            PendingIntent.FLAG_UPDATE_CURRENT
    );

    Intent nextintent = new Intent(MainActivity.PLAYER_ACTION_FILTER);
    nextintent.putExtra(Intent.ACTION_MAIN,PlaybackState.ACTION_SKIP_TO_NEXT);
    PendingIntent nextpendingintent = PendingIntent.getBroadcast(
            this,
            3,
            nextintent,
            PendingIntent.FLAG_UPDATE_CURRENT
    );

    Intent previousintent = new Intent(MainActivity.PLAYER_ACTION_FILTER);
    previousintent.putExtra(Intent.ACTION_MAIN,PlaybackState.ACTION_SKIP_TO_PREVIOUS);
    PendingIntent previouspendingintent = PendingIntent.getBroadcast(
            this,
            4,
            previousintent,
            PendingIntent.FLAG_UPDATE_CURRENT
    );

    Intent actionIntent = new Intent(this,MainActivity.class);

    PendingIntent actionpendingIntent = PendingIntent.getActivity(this,5,actionIntent,PendingIntent.FLAG_UPDATE_CURRENT);

    Notification.Action playpauseAction;
      if(playing){
        playpauseAction = new Notification.Action(
                R.drawable.ic_pause_black_24dp,
                "Pause",
                pausependingintent
        );
      }else {
        playpauseAction = new Notification.Action(
                R.drawable.ic_play_arrow_black_24dp,
                "Play",
                playpendingIntent
        );
      }



    Notification.Builder builder;
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
      builder = new Notification.Builder(this,MainActivity.CHANNEL_ID);
    }else{
      builder = new Notification.Builder(this);
    }


    return builder
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_music_note_black_24dp)
            .addAction(new Notification.Action(R.drawable.ic_skip_previous_black_24dp,"Previoous",previouspendingintent))
            .addAction(playpauseAction)
            .addAction(new Notification.Action(R.drawable.ic_skip_next_black_24dp,"Previoous",nextpendingintent))
            .setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediasessiontoken)
                    .setShowActionsInCompactView(0,1,2)
            )
            .setContentTitle(title)
            .setContentText(author)
            .setContentIntent(actionpendingIntent)
            .setLargeIcon(thumbnail);

  }

  @Override
  public boolean onUnbind(Intent intent) {
    stopForeground(true);
    stopSelf();
    return super.onUnbind(intent);
  }

  void showNotificaion(String title, String author, MediaSession.Token mediasessiontoken, boolean playing, Bitmap thumbnail){
    startForeground(
            1,
            notifBuilder(
                    title,
                    author,
                    mediasessiontoken,
                    playing,
                    thumbnail
            ).build()
    );
  }

  public class  PlayerServiceBinder extends Binder {
    PlayerService getService(){
      return PlayerService.this;
    }
  }
}
