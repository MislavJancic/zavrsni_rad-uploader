package com.mjancic.rad.youtube;

import com.mjancic.rad.database.DbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UploadDialog extends JFrame{
    private JButton cancelButton;
    private JButton doneButton;
    private JPanel panelMain;
    private JButton startUploadButton;
    private JLabel status;

    private String videosPath;
    private String userName;

    UploadVideo uploadVideo;

    private final long ONE_DAY = 86_500_000;
    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials"; // at ~/.oauth-credentials/

    DbConnection dbConnection;
    Thread uploadThread;

    public UploadDialog(String videosPath, String userName) {
        super(userName);
        this.setContentPane(panelMain);
        this.pack();
        this.dbConnection = new DbConnection();
        this.videosPath = videosPath;
        this.userName = userName;
        uploadVideo = new UploadVideo(userName);
        doneButton.setEnabled(false);
        doneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setCancelButton();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setCancelButton();
            }
        });
        startUploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //disable button
                startUploadButton.setEnabled(false);
                //start upload
                uploadThread = new Thread(() -> uploadSelectedFolder());
                uploadThread.start();
            }
        });
    }
    private void setCancelButton(){
        if(uploadThread.isAlive()) uploadThread.stop();
        //close window
        dispose();
    }

    private void uploadSelectedFolder(){

        try{
            ResultSet resultSet = dbConnection.getVideosFromDb(userName,videosPath);
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int n = resultSetMetaData.getColumnCount();
            ArrayList<VideoDataHolder> list = new ArrayList<>(n);

            //load video titles from db
            while(resultSet.next()){
                VideoDataHolder videoDataHolder = new VideoDataHolder();
                videoDataHolder.setVideoTitle(resultSet.getString("title"));
                videoDataHolder.setProcessed(resultSet.getBoolean("is_processed"));
                list.add(videoDataHolder);
                System.out.println("list.add: "+videoDataHolder.getVideoTitle());
            }
            resultSet.close();

            //start upload
            for(int index = 0; index < list.size(); index++){
                VideoDataHolder holder = list.get(index);
                if(!(holder.isProcessed())){
                    status.setText("Currently uploading: "+holder.getVideoTitle());
                    int videoID = uploadVideo.uploadVideo(videosPath, holder.getVideoTitle());
                    if(videoID == -3){
                        videoErrorHandlerCode400();
                        break;
                    }
                    if(videoID == -2){
                        videoErrorHandlerCode403();
                        index--;
                    }
                    System.out.println("Return video id:" + videoID);
                    dbConnection.videoSetProcessed(videoID);
                }
            }
            status.setText("No longer uploading.");
        }catch (SQLException | InterruptedException e){
            e.printStackTrace();
        }
        doneButton.setEnabled(true);
        startUploadButton.setEnabled(true);
    }
    private void videoErrorHandlerCode400(){
        System.out.println("Token expired or revoked (Code 400)");
        //delete user credentials
        File file = new File(System.getProperty("user.home")
                +"/"+CREDENTIALS_DIRECTORY+"/tmp"+userName);
        file.setWritable(true);
        file.delete();
    }
    private void videoErrorHandlerCode403() throws InterruptedException {
        LocalDateTime localDateTime = LocalDateTime.now();
        System.out.println("Daily upload limit reached. Waiting one day. (Code 403)");
        status.setText("Waiting 24h from: "+ localDateTime.toString());
        wait(ONE_DAY);
    }

}
