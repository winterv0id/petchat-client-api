package com.petchat.api.httpclient;

import java.io.File;

public interface ProgressListener {
    void onProgress(long downloaded, long total);
    void onComplete(File file);
    void onError(Exception ex);
}
