package com.ahadkhan.asyndownloader;


public interface AsyncDownloadListener {
    void onLoad(Object object);
    void onFail(Exception e);
}
