package deep.ryd;


import android.content.Context;
import android.net.Uri;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.FileNameGenerator;

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
            .build();
  }
}

class TrackFileNameGenerator implements FileNameGenerator {

  @Override
  public String generate(String url) {
    Uri uri = Uri.parse(url);
    String vidId=uri.getQueryParameter("videoId");

    return vidId;
  }
}
