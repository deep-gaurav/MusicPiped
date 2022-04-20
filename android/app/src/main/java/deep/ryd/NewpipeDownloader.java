package deep.ryd;

import android.text.TextUtils;

import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.Localization;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;


/*
 * Created by Christian Schabesberger on 28.01.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * NewpipeDownloader.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public class NewpipeDownloader extends org.schabi.newpipe.extractor.downloader.Downloader {
    // Firefox ESR
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0";

    private static NewpipeDownloader instance;
    private String mCookies;
    private final OkHttpClient client;

    private NewpipeDownloader(OkHttpClient.Builder builder) {
        this.client = builder
                .readTimeout(30, TimeUnit.SECONDS)
                //.cache(new Cache(new File(context.getExternalCacheDir(), "okhttp"), 16 * 1024 * 1024))
                .build();
    }

    /**
     * It's recommended to call exactly once in the entire lifetime of the application.
     *
     * @param builder if null, default builder will be used
     */
    public static NewpipeDownloader init(@Nullable OkHttpClient.Builder builder) {
        return instance = new NewpipeDownloader(builder != null ? builder : new OkHttpClient.Builder());
    }

    public static NewpipeDownloader getInstance() {
        return instance;
    }

    public String getCookies() {
        return mCookies;
    }

    public void setCookies(String cookies) {
        mCookies = cookies;
    }

    /**
     * Get the size of the content that the url is pointing by firing a HEAD request.
     *
     * @param url an url pointing to the content
     * @return the size of the content, in bytes
     */
    public long getContentLength(String url) throws IOException {
        okhttp3.Response response = null;
        try {
            final okhttp3.Request request = new okhttp3.Request.Builder()
                    .head().url(url)
                    .addHeader("User-Agent", USER_AGENT)
                    .build();
            response = client.newCall(request).execute();

            String contentLength = response.header("Content-Length");
            return contentLength == null ? -1 : Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid content length", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP header field "Accept-Language" to the supplied string.
     *
     * @param siteUrl  the URL of the text file to return the contents of
     * @param localization the language and country (usually a 2-character code) to set
     * @return the contents of the specified text file
     */
    public String download(String siteUrl, Localization localization) throws IOException, ReCaptchaException {
        Map<String, String> requestProperties = new HashMap<>();
        requestProperties.put("Accept-Language", localization.getLanguageCode());
        return download(siteUrl, requestProperties);
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP headers included in the customProperties map.
     *
     * @param siteUrl          the URL of the text file to return the contents of
     * @param customProperties set request header properties
     * @return the contents of the specified text file
     * @throws IOException
     */
    public String download(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
        return getBody(siteUrl, customProperties).string();
    }

    public InputStream stream(String siteUrl) throws IOException {
        try {
            return getBody(siteUrl, Collections.emptyMap()).byteStream();
        } catch (ReCaptchaException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
    }

    private ResponseBody getBody(String siteUrl, Map<String, String> customProperties) throws IOException, ReCaptchaException {
        final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .method("GET", null).url(siteUrl);

        for (Map.Entry<String, String> header : customProperties.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        if (!customProperties.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT);
        }

        if (!TextUtils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final okhttp3.Request request = requestBuilder.build();
        final okhttp3.Response response = client.newCall(request).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested", siteUrl);
        }

        if (body == null) {
            response.close();
            return null;
        }

        return body;
    }

    /**
     * Download (via HTTP) the text file located at the supplied URL, and return its contents.
     * Primarily intended for downloading web pages.
     *
     * @param siteUrl the URL of the text file to download
     * @return the contents of the specified text file
     */
    public String download(String siteUrl) throws IOException, ReCaptchaException {
        return download(siteUrl, Collections.emptyMap());
    }

    public Response get(String siteUrl, Request request) throws IOException, ReCaptchaException {
        final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .method("GET", null).url(siteUrl);

        Map<String, List<String>> requestHeaders = request.headers();
        // set custom headers in request
        for (Map.Entry<String, List<String>> pair : requestHeaders.entrySet()) {
            for(String value : pair.getValue()){
                requestBuilder.addHeader(pair.getKey(), value);
            }
        }

        if (!requestHeaders.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT);
        }

        if (!TextUtils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final okhttp3.Request okRequest = requestBuilder.build();
        final okhttp3.Response response = client.newCall(okRequest).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested", siteUrl);
        }

        if (body == null) {
            response.close();
            return null;
        }

        return new Response(200, body.string(), response.headers().toMultimap(), null, null);
    }

    /*
    @Override
    public Response get(String siteUrl) throws IOException, ReCaptchaException {
        final Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.get(siteUrl);
        final Request newReq = requestBuilder.build();
        return get(siteUrl, newReq);
    }
*/
    @Override
    public Response execute(@javax.annotation.Nonnull Request request) throws IOException, ReCaptchaException {
        return get(request.url(), request);
        //return null;
    }

    public Response post(String siteUrl, Request request) throws IOException, ReCaptchaException {
        Map<String, List<String>> requestHeaders = request.headers();
        if(null == requestHeaders.get("Content-Type") || requestHeaders.get("Content-Type").isEmpty()){
            // content type header is required. maybe throw an exception here
            return null;
        }

        String contentType = requestHeaders.get("Content-Type").get(0);

        RequestBody okRequestBody = null;
        if(null != request.dataToSend()){
            okRequestBody = RequestBody.create(request.dataToSend(), MediaType.parse(contentType));
        }
        final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder()
                .method("POST",  okRequestBody).url(siteUrl);

        // set custom headers in request
        for (Map.Entry<String, List<String>> pair : requestHeaders.entrySet()) {
            for(String value : pair.getValue()){
                requestBuilder.addHeader(pair.getKey(), value);
            }
        }

        if (!requestHeaders.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT);
        }

        if (!TextUtils.isEmpty(mCookies)) {
            requestBuilder.addHeader("Cookie", mCookies);
        }

        final okhttp3.Request okRequest = requestBuilder.build();
        final okhttp3.Response response = client.newCall(okRequest).execute();
        final ResponseBody body = response.body();

        if (response.code() == 429) {
            throw new ReCaptchaException("reCaptcha Challenge requested", siteUrl);
        }

        if (body == null) {
            response.close();
            return null;
        }

        return new Response(200, body.string(), response.headers().toMultimap(), null, null);
    }
}
