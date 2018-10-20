package deep.ryd.rydplayer;

import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;

public class ProxyFactory {

    private static HttpProxyCacheServer sharedProxy;

    private ProxyFactory() {
    }

    public static HttpProxyCacheServer getProxy(Context context) {
        return sharedProxy == null ? (sharedProxy = newProxy(context)) : sharedProxy;
    }

    private static HttpProxyCacheServer newProxy(Context context) {
        return new HttpProxyCacheServer.Builder(context)
                .maxCacheSize(1024 * 1024 * context.getSharedPreferences("Settings",Context.MODE_PRIVATE).getInt("cacheSize",100))
                .build();
    }
}
