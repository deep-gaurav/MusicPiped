package deep.ryd.rydplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

//import org.schabi.newpipe.extractor.Downloader;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import android.support.design.widget.Snackbar;


import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";

    TextView urlText;
    Activity self;
    core coremain;
    public static DBManager dbManager;
    PlayerService playerService;
    boolean isBound =false;

    int MYCHILD=6200;


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            PlayerService.MusicServiceBinder binder = (PlayerService.MusicServiceBinder)service;
            playerService = binder.getPlayerService();
            Log.i("ryd","Service Bound ");
            isBound=true;
            playerService.mainActivity=MainActivity.this;

            withService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playerService = null;
            Log.i("ryg","Service Not BOUND");
            isBound=false;
        }

    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }
    public class swipedpageadaptor extends PagerAdapter{
        public Object instantiateItem(View collection, int position) {

            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.cardView3;
                    break;
                case 1:
                    resId = R.id.queuebase;
                    break;
            }
            return findViewById(resId);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);
        }
    }

    protected void ready() {
        self = this;

        swipedpageadaptor swipedpageadaptor = new swipedpageadaptor();
        ViewPager viewPager = findViewById(R.id.pagerswipe);
        viewPager.setAdapter(swipedpageadaptor);

        if (playerService == null) {
            Log.i("ryd", "CREATE SERVICE ");
            Intent i = new Intent(this, PlayerService.class);

            startService(i);

            bindService(i, serviceConnection, BIND_AUTO_CREATE);
        }
        else {
            Log.i("ryd","Bound to existing service");
            playerService.mainActivity=this;
            isBound=true;
            withService();
        }
        dbManager= new DBManager(this);
        View basequeueview=findViewById(R.id.queuebase);
        basequeueview.bringToFront();

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();

        unbindService(serviceConnection);
    }
    protected void withService(){

        //urlText=findViewById(R.id.urlText);
        //Button submitButton = findViewById(R.id.submitButton);


        coremain=new core(
                this,
                findViewById(R.id.base_lay),
                (ImageView)findViewById(R.id.thumbView),
                (TextView)findViewById(R.id.header),
                (TextView)findViewById(R.id.author),
                (TextView)findViewById(R.id.currentTime),
                (TextView)findViewById(R.id.totalTime),
                (SeekBar)findViewById(R.id.seekBar),
                (ImageButton)findViewById(R.id.playButton),
                (ProgressBar)findViewById(R.id.loadingCircle),
                (ImageButton)findViewById(R.id.nextButton),
                (ImageButton)findViewById(R.id.prevButton),
                (ProgressBar)findViewById(R.id.loadingCircle2),
                null,
                null,
                (ImageButton)findViewById(R.id.playButton2),
                dbManager
        );



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("ryd","CHILD ACTIVITY RESULT "+requestCode+" ");
        if(requestCode==MYCHILD && resultCode==Activity.RESULT_OK){
            changeSong(data.getStringExtra("newurl"));
        }

    }

    public void changeSong(String url){

        Log.i("ryd","EMPTYING QUEUE TO CHANGE SONG");
        playerService.queue=new ArrayList<>();
        playerService.currentIndex=0;
        coremain.playurl=url;
        //urlText.setText(url);
        coremain.changeSong();
    }

    public void playStream(List<StreamInfo> queue,int startIndex){

        Log.i("ryd","SETTING NEW QUEUE TO PLAY STREAM");
        playerService.queue=queue;
        playerService.currentIndex=startIndex;


        //playerService.queue=queue;
        //playerService.currentIndex=startIndex;
        playerService.playfromQueue();

    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        //savedInstanceState.putByteArray("streamInfo",coremain.);
        // etc.
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        //boolean myBoolean = savedInstanceState.getBoolean("MyBoolean");
        //double myDouble = savedInstanceState.getDouble("myDouble");
        //int myInt = savedInstanceState.getInt("MyInt");
        //String myString = savedInstanceState.getString("MyString");
    }


}

class core{
    public String playurl="";
    public MainActivity context;
    public View baselay;
    public ImageView thumbView;
    public TextView header;
    public TextView author;
    public List<AudioStream> audioStreams;
    public StreamInfo streamInfo;
    public TextView currentTime;
    public TextView totalTime;
    public SeekBar seekBar;
    public ImageButton playButton;
    public ProgressBar circleLoader;
    public ImageButton nextButton,prevButton;
    public core self=this;
    public ProgressBar circleLoader2;
    public EditText urlEditText;
    public Button submitButton;
    public ImageButton playButton2;
    DBManager dbManager;

    public QueueListAdaptor queueListAdaptor;

    @Deprecated
    public void play() throws IOException {
        //new setThumb().execute(this);

        //CachedImageDownloader cachedImageDownloader = new CachedImageDownloader(streamInfo.getThumbnailUrl(),thumbView);
        //cachedImageDownloader.execute();
        //setLoadingCircle2(true);

        if(context.playerService.queue.isEmpty()) {
            context.playerService.queue.add(streamInfo);
            context.playerService.currentIndex=0;
        }
        else
            context.playerService.queue.set(context.playerService.currentIndex,streamInfo);


        context.playerService.streamInfo=streamInfo;
        context.playerService.control_MP(PlayerService.ACTION_PLAY);

        context.playerService.umP.reset();
        //Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
        //Log.i("ryd","Ping Status "+audioStreams.get(0).url+ " "+ping_status);


        //context.playerService.umP.setDataSource(audioStreams.get(0).getUrl());
        //context.playerService.umP.prepareAsync();
        new PlayerService.UpdateSongStream().execute();
        context.playerService.isuMPready=false;
        context.playerService.umP.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                context.playerService.isuMPready=true;


                updateStreaminDB(context.playerService.streamInfo,dbManager);
                toggle();
                setLoadingCircle1(false);
                setLoadingCircle2(false);

            }
        });
        context.playerService.umP.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what){
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        setLoadingCircle2(true);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        setLoadingCircle2(false);
                        break;

                };

                return false;
            }
        });


        start();


    }

    public  static void updateStreaminDB(StreamInfo streamInfo, DBManager dbManager){

        Log.i("ryd","Updating Stream in DB ");
        dbManager=dbManager.open();
        dbManager.addSong(streamInfo.getName(),
                streamInfo.getUrl(),
                streamInfo.getUploaderName(),
                streamInfo.getThumbnailUrl(),
                streamInfo.getUploaderAvatarUrl(),
                streamInfo.getUploaderUrl(),
                audiostreamtoString(streamInfo.getAudioStreams().get(0)));
        dbManager.close();
    }
    public void setLoadingCircle2(final boolean state){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state)
                    circleLoader2.setVisibility(View.VISIBLE);
                else {
                    circleLoader2.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
    public void setLoadingCircle1(final boolean state){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state)
                    circleLoader.setVisibility(View.VISIBLE);
                else {
                    circleLoader.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public static String audiostreamtoString(AudioStream audioStream){
        String url=audioStream.url;
        String averageBitrate = String.valueOf(audioStream.average_bitrate);
        String mediaFormatId= String.valueOf(audioStream.getFormat().id);

        String finalStr=url+" "+averageBitrate+" "+mediaFormatId;
        return finalStr;
    }

    public static AudioStream StringtoAudioStream(String str){
        String array[]= str.split(" ");
        String url=array[0];
        Integer averageBitrate = Integer.parseInt(array[1]);
        MediaFormat mediaFormat = MediaFormat.getFormatById(Integer.parseInt(array[2]));

        AudioStream audioStream = new AudioStream(url,mediaFormat,averageBitrate);

        return audioStream;
    }
    public void start(){

        streamInfo = context.playerService.streamInfo;

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                queueListAdaptor.updateQueue(context.playerService.queue);
                //submitButton.setEnabled(true);
                //circleLoader.setVisibility(View.INVISIBLE);
                setLoadingCircle1(false);
                Log.i("ryd","Playing song "+streamInfo.getName());
                header.setText(streamInfo.getName());
                author.setText(streamInfo.getUploaderName());
                //Log.i("rydp", "Mediaplayer duration "+(new Integer(uMP.getDuration())).toString());
            }
        });
        //toggle();
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Picasso.get()
                        .load(streamInfo.getThumbnailUrl())
                        .into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                context.playerService.thumbnail=bitmap;
                                thumbView.setImageBitmap(bitmap);
                                context.playerService.buildNotification(context.playerService.ID);
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {

                            }
                        });
                //circleLoader2.setVisibility(View.INVISIBLE);
                context.playerService.start();
            }
        });
    }
    public void showError(final String error){

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //submitButton.setEnabled(true);
                Snackbar snackbar=Snackbar.make(baselay,error,Snackbar.LENGTH_LONG);
                snackbar.setAction("Send Error", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(intent.EXTRA_TEXT,error);
                        context.startActivity(Intent.createChooser(intent,"Send Error"));
                    }
                });
                snackbar.show();
                //circleLoader.setVisibility(View.INVISIBLE);
                setLoadingCircle1(false);
            }
        });


    }

    public static String sectotime(long sec,boolean isMili){
        if(isMili)
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

    core(Activity context
            ,View baselay,
         ImageView thumbView,
         TextView header,
         TextView author,
         TextView currentTime,
         TextView totalTime,
         SeekBar seekBar,
         ImageButton playButton,
         ProgressBar circleLoader,
         ImageButton nextButton,
         ImageButton prevButton,
         ProgressBar circleLoader2,
         EditText urlEditText,
         Button submitButton,
         ImageButton playButton2,
         DBManager dbManager) {

        //INIT
        this.context = (MainActivity) context;
        this.baselay = baselay;
        this.thumbView = thumbView;
        this.header = header;
        this.author = author;
        this.currentTime = currentTime;
        this.totalTime = totalTime;
        this.seekBar = seekBar;
        this.playButton = playButton;
        this.circleLoader = circleLoader;
        this.nextButton = nextButton;
        this.prevButton = prevButton;
        this.circleLoader2 = circleLoader2;
        this.urlEditText = urlEditText;
        this.submitButton = submitButton;
        this.playButton2 = playButton2;
        this.dbManager=dbManager;

        //uMP = new MediaPlayer();
        //uMP=((MainActivity) context).playerService.umP;
        ready();
    }
    public void ready(){
        if(queueListAdaptor==null){

            RecyclerView queueRecycler = context.findViewById(R.id.queueRecycler);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false);
            queueRecycler.hasFixedSize();
            queueRecycler.setLayoutManager(layoutManager);

            queueListAdaptor = new QueueListAdaptor(context);
            queueRecycler.setAdapter(queueListAdaptor);
        }
        if(context.playerService.isuMPready){
            streamInfo=context.playerService.streamInfo;
            //uMP=context.playerService.umP;
            start();
        }
        final ImageButton repeat=context.findViewById(R.id.repeatButton);
        if(context.playerService.isLooping){
            DrawableCompat.setTint(repeat.getDrawable(), ContextCompat.getColor(context, R.color.colorAccent));
        }
        Timer timer = new Timer(false);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(context.playerService.umP.isPlaying()) {
                            currentTime.setText(sectotime(context.playerService.umP.getCurrentPosition(), true));
                            seekBar.setProgress(context.playerService.umP.getCurrentPosition()/1000);


                            totalTime.setText(sectotime(context.playerService.umP.getDuration(),true));
                            seekBar.setMax((int)context.playerService.umP.getDuration()/1000);
                        }

                    }

                });
            }
        },500,500);

        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton b= (ImageButton)v;
                if(context.playerService.isLooping) {
                    b.setImageResource(R.drawable.ic_repeat_one_white_24dp);
                    b.setColorFilter(null);
                    context.playerService.isLooping=false;
                }
                else {
                    context.playerService.isLooping=true;
                    b.setColorFilter(ContextCompat.getColor(b.getContext(),R.color.colorAccent));
                    //DrawableCompat.setTint(b.getDrawable(), ContextCompat.getColor(context, R.color.colorAccent));
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    context.playerService.umP.seekTo(progress*1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton self = (ImageButton) v;
                toggle();
            }
        });
        playButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.playerService.nextSong();
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.playerService.previousSong();
            }
        });

        //circleLoader.setVisibility(View.INVISIBLE);
        //circleLoader2.setVisibility(View.INVISIBLE);

        setLoadingCircle1(false);
        setLoadingCircle2(false);

        if(context.playerService.umP.isPlaying()){
            playButton.setImageResource(android.R.drawable.ic_media_pause);
            playButton2.setImageResource(android.R.drawable.ic_media_pause);
        }


    }

    public void changeSong(){
        //playurl=urlEditText.getText().toString();
        //circleLoader.setVisibility(View.VISIBLE);
        setLoadingCircle1(true);
        //submitButton.setEnabled(false);
        new testPipe().execute(this);
    }

    public void toggle(){
        if(context.playerService.isuMPready){
            if(context.playerService.umP.isPlaying()){
                context.playerService.umP.pause();
                playButton.setImageResource(android.R.drawable.ic_media_play);
                playButton2.setImageResource(android.R.drawable.ic_media_play);
                context.playerService.buildNotification(context.playerService.ID);
            }
            else{
                context.playerService.umP.start();
                playButton.setImageResource(android.R.drawable.ic_media_pause);
                playButton2.setImageResource(android.R.drawable.ic_media_pause);
                context.playerService.buildNotification(context.playerService.ID);
            }
        }
    }


    public void make_Queue(){
        
    }

    class QueueListAdaptor extends RecyclerView.Adapter<QueueListAdaptor.QueViewHolder>{

        List<StreamInfo> queue=new ArrayList<>();
        MainActivity activity;

        public void updateQueue(List<StreamInfo> newqueue){
            queue.clear();
            for(int i=0;i<newqueue.size();i++){
                queue.add(i,newqueue.get(i));
            }
            notifyDataSetChanged();
        }

        QueueListAdaptor(MainActivity mainActivity){
            super();

            activity=mainActivity;
            Log.i("ryt","PlayerService Queue Size "+mainActivity.playerService.queue.size());
            //this.playerService = playerService;
            queue.clear();
            for (int i=0;i<mainActivity.playerService.queue.size();i++)
                queue.add(mainActivity.playerService.queue.get(i));
            //this.queue=playerService.queue.subList(0,playerService.queue.size()-1);

        }
        @NonNull
        @Override
        public QueueListAdaptor.QueViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            ViewGroup.LayoutParams params;
            params=new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            //viewGroup.setLayoutParams(params);

            CardView cardView = new CardView(viewGroup.getContext());
            cardView.setLayoutParams(params);

            TypedValue outValue = new TypedValue();
            cardView.getContext().getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
            cardView.setForeground(cardView.getContext().getDrawable(outValue.resourceId));


            cardView.addView( LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.queueelement,viewGroup,false));

            QueViewHolder queViewHolder = new QueViewHolder(cardView);
            return queViewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull QueueListAdaptor.QueViewHolder queViewHolder, int i) {

            TextView Sno= (TextView)queViewHolder.cardView.findViewById(R.id.queuenumber);
            TextView SongName= (TextView)queViewHolder.cardView.findViewById(R.id.queueContent);


            Sno.setText(String.valueOf(i-activity.playerService.currentIndex));
            SongName.setText(queue.get(i).getName());
        }


        @Override
        public int getItemCount() {
            return queue.size();
        }

        class QueViewHolder extends RecyclerView.ViewHolder{

            public CardView cardView;
            public QueViewHolder(@NonNull View itemView) {
                super(itemView);

                cardView=(CardView)itemView;
            }
        }
    }

    public void updateUI(){

    }

}

class testPipe extends AsyncTask<core,Integer,Integer> {


    PlayerService playerService;
    @Override
    protected Integer doInBackground(core... cores) {


        final core mcore=cores[0];
        String url=mcore.playurl;

        Downloader.init(null);
        NewPipe.init(Downloader.getInstance());
        try{

            int sid = NewPipe.getIdOfService("YouTube");
            YoutubeService ys= (YoutubeService)NewPipe.getService(sid);

            StreamInfo streamInfo= StreamInfo.getInfo(ys,url);
            mcore.streamInfo=streamInfo;
            //StreamExtractor streamExtractor=ys.getStreamExtractor(url);
            //Toast.makeText(c, streamExtractor.getUrl(), Toast.LENGTH_LONG).show();

            //Toast.makeText(c, "SERVICE ID YOUTUBE "+ new Integer(sid).toString(), Toast.LENGTH_LONG).show();
            //StreamInfo streamInfo = StreamInfo.getInfo(url);
            //Log.i("rydp","SERVICE ID YOUTUBE "+ new Integer(sid).toString());
            //System.out.println("SERVICE ID YOUTUBE "+ new Integer(sid).toString());
            mcore.audioStreams=streamInfo.getAudioStreams();
            Log.i("rydp","GET NAME "+streamInfo.getAudioStreams().get(0).getUrl());
            //Log.i("rydp",streamInfo.getName());
            mcore.playurl=streamInfo.getAudioStreams().get(0).getUrl();

            mcore.context.playerService.queue.clear();
            mcore.context.playerService.queue.add(streamInfo);
            mcore.context.playerService.currentIndex=0;

            playerService=mcore.context.playerService;
        }
        catch (Exception e){
            e.printStackTrace();
            Writer writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            mcore.showError("Cannot play given url ERROR \n"+writer.toString());



        }

        return 0;
    }

    @Override
    protected void onPostExecute(Integer integer){
        playerService.playfromQueue();
    }
}
