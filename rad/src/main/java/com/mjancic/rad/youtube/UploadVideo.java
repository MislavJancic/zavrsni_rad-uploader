package com.mjancic.rad.youtube;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.common.collect.Lists;
import com.mjancic.rad.database.DbConnection;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.nio.file.Files.newInputStream;

public class UploadVideo {
    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private YouTube youtube;
    /**
     * Define a global variable that specifies the MIME type of the video
     * being uploaded.
     */
    private String VIDEO_FILE_FORMAT = "video/*";

    private String name;

    DbConnection dbConnection;

    public UploadVideo(String name) {
        this.name = name;
        dbConnection = new DbConnection();
    }

    private Credential authenticate(){
        Auth auth = new Auth();
        Credential credential = null;
        try {
            credential = auth.authorize(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return credential;
    }

    public int uploadVideo(String videoPath, String videoTitle){
        Auth auth = new Auth();
        youtube = new YouTube.Builder(auth.HTTP_TRANSPORT, auth.JSON_FACTORY,
                authenticate()).setApplicationName(
                "Youtube Uploader").build();

        System.out.println("Uploading: " + videoTitle);


        //Load video metadata from database
        VideoDataHolder videoDataHolder = dbConnection.getVideo(name,videoTitle);

        // Add extra information to the video before uploading.
        Video videoObjectDefiningMetadata = new Video();

        // Video visibility
        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus(videoDataHolder.getStatus());
        videoObjectDefiningMetadata.setStatus(status);

        // Most of the video's metadata is set on the VideoSnippet object.
        VideoSnippet snippet = new VideoSnippet();
        List<String> tags = new ArrayList<String>();
        //strip extension
        String videoName = videoTitle;
        videoName = FilenameUtils.removeExtension(videoName);
        snippet.setTitle(videoName);
        snippet.setDescription(videoDataHolder.getDescription());

        String[] tagsToBe = videoDataHolder.getTags().split(",");
        //remove braces
        for (String s: tagsToBe) {
            if(s.contains("[")){
                s = s.replace("[","");
            }
            if(s.contains("]")){
                s = s.replace("]","");
            }

            tags.add(s.trim());
        }

        snippet.setTags(tags);

        // Add the completed snippet object to the video resource.
        videoObjectDefiningMetadata.setSnippet(snippet);

        InputStreamContent mediaContent = null;
        try {
            mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT,
                    newInputStream(Path.of(videoPath + "/" +videoTitle)));
        } catch (IOException e) {

            e.printStackTrace();
        }

        // Insert the video. The command sends three arguments. The first
        // specifies which information the API request is setting and which
        // information the API response should return. The second argument
        // is the video resource that contains metadata about the new video.
        // The third argument is the actual video content.

        try {
            YouTube.Videos.Insert videoInsert = youtube.videos()
                    .insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);
            // Set the upload type and add an event listener.
            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

            // Indicate whether direct media upload is enabled. A value of
            // "True" indicates that direct media upload is enabled and that
            // the entire media content will be uploaded in a single request.
            // A value of "False," which is the default, indicates that the
            // request will use the resumable media upload protocol, which
            // supports the ability to resume an upload operation after a
            // network interruption or other transmission failure, saving
            // time and bandwidth in the event of network failures.
            uploader.setDirectUploadEnabled(false);

            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            System.out.println("Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            System.out.println("Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            System.out.println("Upload in progress");
                            //System.out.println("Upload percentage: " + uploader.getProgress());
                            break;
                        case MEDIA_COMPLETE:
                            System.out.println("Upload Completed!");
                            break;
                        case NOT_STARTED:
                            System.out.println("Upload Not Started!");
                            break;
                    }
                }
            };

            uploader.setProgressListener(progressListener);

            // Call the API and upload the video.
            Video returnedVideo = videoInsert.execute();

            // Print data about the newly inserted video from the API response.
            System.out.println("\n================== Returned Video ==================\n");
            System.out.println("  - Id: " + returnedVideo.getId());
            System.out.println("  - Title: " + returnedVideo.getSnippet().getTitle());
            System.out.println("  - Tags: " + returnedVideo.getSnippet().getTags());
            System.out.println("  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
            System.out.println("  - Video Count: " + returnedVideo.getStatistics().getViewCount());

            if(!returnedVideo.isEmpty()) return videoDataHolder.getVideoID();

        }catch (IOException e){
            System.out.println("Upload not completed.");
            e.printStackTrace();
            String exception = e.toString();
            if(exception.contains("403")) return -2;
            if(exception.contains("400")) return -3;
        }
        return -1;
    }

}
