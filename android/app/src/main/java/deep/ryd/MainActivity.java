package deep.ryd;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.ryanheise.audioservice.AudioService;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeService;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.utils.Localization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends FlutterActivity  {
  private static final String ID = "deep.musicpiped/urlfix";
  private MethodChannel channel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    GeneratedPluginRegistrant.registerWith(this);

    NewpipeDownloader.init(null);
    NewPipe.init(NewpipeDownloader.getInstance(),new Localization("GB","en"));
    channel=new MethodChannel(getFlutterView(), ID);
    channel.setMethodCallHandler(
            new MethodChannel.MethodCallHandler() {
              @Override
              public void onMethodCall(MethodCall call, MethodChannel.Result result) {
                if(call.method.equals("getURL")){
                  String url = call.argument("url");
                  if(URLProxyFactory.getProxy(MainActivity.this).isCached(url)){
                    result.success(URLProxyFactory.getProxy(MainActivity.this).getProxyUrl(url));
                  }else{
                    Uri uri = Uri.parse(url);
                    String vidId=uri.getQueryParameter("videoId");
                    Log.d("musicpiped","GET NEWPIPE URL for VIdeo "+vidId);
                    new URLVerifier(MainActivity.this,vidId,channel,result).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,url);

                  }
                }else if(call.method.equals("isCached")){
                  boolean r=URLProxyFactory.getProxy(MainActivity.this).isCached(call.argument("url"));
                  result.success(r);
                }
              }
            });
  }

  @Override
  protected void onDestroy() {

    channel.invokeMethod("close",null);

    android.os.Process.killProcess(android.os.Process.myPid());
//    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
//    audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_STOP));
    super.onDestroy();
  }
}

class URLVerifier extends AsyncTask<String,String,String> {

  String videoId;
  MainActivity activity;
  MethodChannel channel;
  MethodChannel.Result result;

  URLVerifier(MainActivity context, String videoId, MethodChannel channel, MethodChannel.Result result){
    this.activity=context;
    this.channel=channel;
    this.videoId=videoId;
    this.result=result;
  }

  List<String> getNewpipeURL() throws ExtractionException, IOException {

    Log.d("musicpiped","Get newpipe URL");

    String youtubeURL="https://www.youtube.com/watch?v=";

    String url = youtubeURL+videoId;

    YoutubeService youtubeService = (YoutubeService) NewPipe.getService(NewPipe.getIdOfService("YouTube"));
    StreamExtractor streamExtractor = youtubeService.getStreamExtractor(url);
    streamExtractor.fetchPage();
    List<String> urls = new ArrayList<>();
    for(AudioStream s: streamExtractor.getAudioStreams()){
      urls.add(s.url);
    }
    return urls;
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
      if(response.isSuccessful()){
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
    for(int tries=0;tries<3 && verifyURL(url); tries++){
      try {
        List<String> newURLS = getNewpipeURL();
        for(String newurl : newURLS){
          if(!verifyURL(newurl)){
            url = newurl;
            break;
          }
        }
      } catch (ExtractionException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if(verifyURL(url)){
      url = "ERROR";
    }
    return url;
  }

  @Override
  protected void onPostExecute(String s) {
    super.onPostExecute(s);
    if(s=="ERROR"){
      Toast.makeText(activity,"Can not play that track, Retry later",Toast.LENGTH_LONG).show();

      result.success(s+"&videoId="+videoId);
    }else{
      s+="&videoId="+videoId;
      s=URLProxyFactory.getProxy(activity).getProxyUrl(s);
      result.success(s);
    }
  }
}