package com.mjancic.rad.database;

import com.mjancic.rad.youtube.VideoDataHolder;
import com.mjancic.rad.youtube.VideoDataHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@Qualifier("db")
public class DbConnection {
    private PreparedStatement preparedStatement;

    public Connection connect(){
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:uploaderDatabase.db");
            System.out.println("Connected to database.");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to database.");
        }
        return connection;
    }

    public ResultSet getVideosFromDb(String username, String videosPath){
        Connection connection = connect();
        ResultSet resultSet;
        String sql = "SELECT video.title, video.is_processed FROM video, accounts WHERE video.path = ? " +
                "AND video.account_id = accounts.account_id AND accounts.name = ?";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,videosPath);
            preparedStatement.setString(2, username);

            resultSet = preparedStatement.executeQuery();
            return resultSet;
        }
        catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteVideos(String name){
        Connection connection = connect();
        preparedStatement = null;

        try{
            Integer id = this.getUserID(name);
            String sql = "DELETE FROM video WHERE account_id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,id.toString());
            preparedStatement.execute();
            System.out.println("Videos deleted.");
            return true;

        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public void deleteAccount(String name){
        Connection connection = connect();
        preparedStatement = null;

        try{

            String sql = "DELETE FROM accounts WHERE name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,name);
            preparedStatement.execute();
            System.out.println("Account deleted.");

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> getAllAccounts(){
        ArrayList<String> accountNames = new ArrayList<String>();
        Connection connection = connect();

        ResultSet resultSet = null;

        try {

            String sql = "SELECT * FROM accounts";
            preparedStatement = connection.prepareStatement(sql);
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                accountNames.add(resultSet.getString("name"));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return accountNames;
    }

    public void insertAccount(String name,byte[] bytes){
        Connection connection = connect();

        try{

            String sql = "UPDATE accounts SET credentials = ? WHERE name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(2,name);
            preparedStatement.setBytes(1,bytes);
            preparedStatement.execute();
            System.out.println("Account updated.");
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void videoSetProcessed(int videoID){
        Connection connection = connect();

        try{
            String sql = "UPDATE video SET is_processed = 1 WHERE video_id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1,videoID);
            preparedStatement.execute();
            System.out.println("Video set to processed.");
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void insertVideoData(Connection connection,int userID, String title, 
                                String path, List<String> tags, String description, 
                                String privacy){

        try{
            String sql = "INSERT INTO video (account_id, title, description, tags, path, status) VALUES (?,?,?,?,?,?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1,userID);
            preparedStatement.setString(2, title);
            preparedStatement.setString(3,description);
            preparedStatement.setString(4,tags.toString());
            preparedStatement.setString(5,path);
            preparedStatement.setString(6,privacy);

            preparedStatement.execute();
        }catch(SQLException e){
            e.printStackTrace();
        }finally {
            try {
                preparedStatement.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    public VideoDataHolder getVideo(String username, String videoTitle){
        Connection connection = connect();

        ResultSet resultSet = null;
        VideoDataHolder video = new VideoDataHolder();
        try{
            String sql = "SELECT video.video_id, accounts.name, video.title, video.path, video.description, video.status, video.tags, video.is_processed  FROM accounts, video WHERE video.account_id = accounts.account_id AND accounts.name = ? AND video.title = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2,videoTitle);

            resultSet = preparedStatement.executeQuery();
            video.setVideoID(resultSet.getInt("video_id"));
            video.setVideoTitle(resultSet.getString("title"));
            video.setVideoPath(resultSet.getString("path"));
            video.setDescription(resultSet.getString("description"));
            video.setUser(resultSet.getString("name"));
            video.setTags(resultSet.getString("tags"));
            video.setStatus(resultSet.getString("status"));
            video.setProcessed(resultSet.getBoolean("is_processed"));

        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            try {
                resultSet.close();
                preparedStatement.close();
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return video;
    }

    public int getUserID(String user){
        Connection connection = connect();

        ResultSet resultSet = null;
        int res = 0;
        try{
            String sql = "SELECT account_id FROM accounts WHERE name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,user);
            resultSet = preparedStatement.executeQuery();
            res = resultSet.getInt("account_id");
        }catch(SQLException e){
            e.printStackTrace();
        }
        finally {
            try{
                preparedStatement.close();
                resultSet.close();
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }

        return res;
    }

    public void insertName(String name){
        Connection connection = connect();

        try{
            String sql = "INSERT INTO accounts (name) VALUES (?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,name);
            preparedStatement.execute();
            System.out.println("Name inserted");

        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            try {
                preparedStatement.close();
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    public byte[] getAccountCredential (String name){
        Connection connection = connect();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        byte[] inputStream = null;

        try{
            String sql = "SELECT credentials FROM accounts WHERE name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,name);
            resultSet = preparedStatement.executeQuery();
            //reading
            inputStream = resultSet.getBytes(1);
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            try {
                preparedStatement.close();
                resultSet.close();
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return inputStream;
    }
    public String getClientSecrets(){
        Connection connection = connect();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String result = null;

        try {
            String sql = "SELECT client_secrets_json FROM client_secrets where ROWID = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1,1);
            resultSet = preparedStatement.executeQuery();
            result = resultSet.getString(1);

        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            try {
                preparedStatement.close();
                resultSet.close();
                connection.close();
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
        return result;
    }
}
