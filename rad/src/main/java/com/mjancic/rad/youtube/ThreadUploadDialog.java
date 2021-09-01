package com.mjancic.rad.youtube;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


public class ThreadUploadDialog extends Thread{

    private String videospath;
    private String username;

    public void setVideospath(String videospath) {
        this.videospath = videospath;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void run(){
        UploadDialog uploadDialog = new UploadDialog(videospath,username);
        uploadDialog.setVisible(true);
    }
}
