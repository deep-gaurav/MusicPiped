package deep.ryd;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.FileNameGenerator;

import java.io.File;
import java.net.URI;

public class URLProxyFactory {

  private static HttpProxyCacheServer sharedProxy;

  private URLProxyFactory() {
  }

  public static HttpProxyCacheServer getProxy(Context context) {
    return sharedProxy == null ? (sharedProxy = newProxy(context)) : sharedProxy;
  }

  private static HttpProxyCacheServer newProxy(Context context) {
    return new HttpProxyCacheServer.Builder(context)
            .fileNameGenerator(new TrackFileNameGenerator())
            .maxCacheFilesCount(50)
            .maxCacheSize(1024 * 1024 * 500)
            .build();
  }
}

class TrackFileNameGenerator implements FileNameGenerator {

  @Override
  public String generate(String url) {
    Uri uri = Uri.parse(url);
    String vidId=uri.getQueryParameter("videoId");

    Log.d("musicpiped","Generated filename "+vidId);
    return vidId;
  }
}
