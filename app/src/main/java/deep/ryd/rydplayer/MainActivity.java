package deep.ryd.rydplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

//import org.schabi.newpipe.extractor.Downloader;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.support.design.widget.Snackbar;


import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";

    TextView urlText;
    Activity self;
    core coremain;
    public static DBManager dbManager;

    int MYCHILD=6200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    protected void ready(){
        self=this;
        urlText=findViewById(R.id.urlText);
        Button submitButton = findViewById(R.id.submitButton);

        dbManager= new DBManager(this);

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
                (EditText)findViewById(R.id.urlText),
                (Button)findViewById(R.id.submitButton),
                (ImageButton)findViewById(R.id.playButton2),
                dbManager
        );

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coremain.changeSong();
            }
        });


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
        urlText.setText(url);
        coremain.changeSong();
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
}

class core{
    public MediaPlayer uMP;
    public String playurl="";
    public Activity context;
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
    public boolean isumpReady=false;
    public ProgressBar circleLoader;
    public ImageButton nextButton,prevButton;
    public core self=this;
    public ProgressBar circleLoader2;
    public EditText urlEditText;
    public Button submitButton;
    public ImageButton playButton2;
    DBManager dbManager;

    public void play() throws IOException {
        new setThumb().execute(this);
        uMP.reset();
        //Toast.makeText(context, "", Toast.LENGTH_SHORT).show();
        uMP.setDataSource(audioStreams.get(0).getUrl());
        uMP.prepareAsync();
        isumpReady=false;
        uMP.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isumpReady=true;
                dbManager=dbManager.open();
                dbManager.addSong(streamInfo.getName(),
                        streamInfo.getUrl(),
                        streamInfo.getUploaderName(),
                        streamInfo.getThumbnailUrl(),
                        streamInfo.getUploaderAvatarUrl(),
                        streamInfo.getUploaderUrl());
                dbManager.close();
                toggle();
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        circleLoader2.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                submitButton.setEnabled(true);
                circleLoader.setVisibility(View.INVISIBLE);
                circleLoader2.setVisibility(View.VISIBLE);
                header.setText(streamInfo.getName());
                author.setText(streamInfo.getUploaderName());
                totalTime.setText(sectotime(streamInfo.getDuration(),false));
                seekBar.setMax((int)streamInfo.getDuration());
                //Log.i("rydp", "Mediaplayer duration "+(new Integer(uMP.getDuration())).toString());
            }
        });



    }

    public void showError(final String error){

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                submitButton.setEnabled(true);
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
                circleLoader.setVisibility(View.INVISIBLE);
            }
        });


    }

    public String sectotime(long sec,boolean isMili){
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
        this.context = context;
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

        uMP = new MediaPlayer();
        ready();
    }
    public void ready(){
        Timer timer = new Timer(false);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(uMP.isPlaying()) {
                            currentTime.setText(sectotime(uMP.getCurrentPosition(), true));
                            seekBar.setProgress(uMP.getCurrentPosition()/1000);
                        }
                    }
                });
            }
        },500,500);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    uMP.seekTo(progress*1000);
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
                StreamInfoItem nextvid=streamInfo.getNextVideo();
                playurl=nextvid.getUrl();
                Log.i("rydt","NEXT VIDEO URL "+playurl);
                self.changeSong();
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        circleLoader.setVisibility(View.INVISIBLE);
        circleLoader2.setVisibility(View.INVISIBLE);
    }

    public void changeSong(){
        playurl=urlEditText.getText().toString();
        circleLoader.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        new testPipe().execute(this);
    }

    public void toggle(){
        if(isumpReady){
            if(uMP.isPlaying()){
                uMP.pause();
                playButton.setImageResource(android.R.drawable.ic_media_play);
                playButton2.setImageResource(android.R.drawable.ic_media_play);
            }
            else{
                uMP.start();
                playButton.setImageResource(android.R.drawable.ic_media_pause);
                playButton2.setImageResource(android.R.drawable.ic_media_pause);
            }
        }
    }

}

class testPipe extends AsyncTask<core,Integer,Integer> {


    @Override
    protected Integer doInBackground(core... cores) {


        core mcore=cores[0];
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
            mcore.play();
        }
        catch (Exception e){
            e.printStackTrace();
            Writer writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            mcore.showError("Cannot play given url ERROR \n"+writer.toString());


        }

        return 0;
    }
}

class setThumb extends AsyncTask<core,Integer,Integer>{

    @Override
    protected Integer doInBackground(core... cores) {
        try {
            URL urlConnection = new URL(cores[0].streamInfo.getThumbnailUrl());
            Log.i("rypd","Thumbnail URL downloading "+urlConnection.toString());
            HttpURLConnection connection = (HttpURLConnection) urlConnection
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            final Bitmap myBitmap = BitmapFactory.decodeStream(input);
            final core mcore =cores[0];
            cores[0].context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mcore.thumbView.setImageBitmap(myBitmap);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}