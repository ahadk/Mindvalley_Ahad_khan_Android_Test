package com.ahadkhan.asyndownloader;


import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class AsyncDownloader {
    private static final String TAG = "AsyncDownloader";
    private final Context context;
    private URL url;
    private File outDir;
    private AsyncDownloadListener listener;
    private static LruCache<String,Object> memoryCache;
    private boolean isCanceled;
    public AsyncDownloader(URL url,Context context) {
        this.url = url;
        this.context = context;
        outDir = context.getCacheDir();
        if (!outDir.exists())
            outDir.mkdir();
        if (memoryCache==null)
            memoryCache = new LruCache<>((int) (Runtime.getRuntime().maxMemory()/4));
    }

    public AsyncDownloader setListener(AsyncDownloadListener listener) {
        this.listener = listener;
        return this;
    }

    public void cancel()
    {
        this.isCanceled = true;
    }

    public void start()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                _start();
            }
        }).start();
    }

    private void _start(){
        if (memoryCache.get(url.toString())!=null)
            _onLoad(memoryCache.get(url.toString()));
        else
            try{
                HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                int responseCode = httpConn.getResponseCode();

                // always check HTTP response code first
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String fileName = "";
                    String disposition = httpConn.getHeaderField("Content-Disposition");
                    String contentType = httpConn.getContentType();
                    int contentLength = httpConn.getContentLength();

                    fileName = String.valueOf(Calendar.getInstance().getTime().getTime());

                    Log.e(TAG,"Content-Type = " + contentType);
                    Log.e(TAG,"Content-Disposition = " + disposition);
                    Log.e(TAG,"Content-Length = " + contentLength);
                    Log.e(TAG,"fileName = " + fileName);

                    // opens input stream from the HTTP connection
                    InputStream inputStream = httpConn.getInputStream();
                    String saveFilePath = outDir.getAbsolutePath() + File.separator + fileName;

                    // opens an output stream to save into file
                    FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                    int bytesRead = -1;
                    byte[] buffer = new byte[1024];
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        if (isCanceled)
                        {
                            isCanceled = false;
                            return;
                        }
                    }

                    outputStream.close();
                    inputStream.close();

                    Object container = null;
                    if (contentType.contains("image"))
                    {
                          container = BitmapFactory.decodeFile(saveFilePath);
                    }
                    else if (contentType.contains("text"))
                    {
                        container  = Methods.mReadJsonData(new File(saveFilePath));
                    }
                    if (container!=null && memoryCache.get(url.toString())==null && !isCanceled)
                    {
                        memoryCache.put(url.toString(),container);
                    }

                    _onLoad(container);

                    Log.e(TAG,"File downloaded");
                } else {
                    Log.e(TAG,"No file to download. Server replied HTTP code: " + responseCode);
                }
                httpConn.disconnect();
            }catch (Exception e) {
                _onFail(e);
                Log.e(TAG,e.toString());
            }
    }

    private void _onLoad(Object container)
    {
        if (listener!=null &&  container!=null && !isCanceled) {
            final Object finalContainer = container;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onLoad(finalContainer);
                    isCanceled = false;
                }
            });
        }
    }

    private void _onFail(final Exception e)
    {
        if (listener!=null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onFail(e);
                }
            });
        }
    }
}
