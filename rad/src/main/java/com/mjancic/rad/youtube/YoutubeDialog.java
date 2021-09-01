package com.mjancic.rad.youtube;

import ch.qos.logback.core.util.FileUtil;
import com.mjancic.rad.database.DbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

@Component
public class YoutubeDialog extends JFrame {
    private JPanel panelMain;
    private JList listAccounts;
    private JButton buttonNewAccount;
    private JButton buttonStartUpload;
    private JLabel labelAccountsList;
    private JLabel labelActions;
    private JButton buttonBrowserFolder;
    private JLabel labelSelectedFolder;
    private JButton loadAccountsButton;
    private JTextField usernameTextField;
    private JButton deleteAccountButton;
    private JButton addTagButton;
    private JTextField tagTextField;
    private JButton privateButton;
    private JButton publicButton;
    private JLabel privacySetting;
    private JButton clearVideosFromDbButton;
    private DefaultListModel listAccountsModel;
    private ArrayList<String> accountsList;

    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials"; // at ~/.oauth-credentials/

    private final long MAX_VIDEO_SIZE = 128_000_000; //always multiply by 1000
    private String videospath;
    private String userName;


    @Autowired
    DbConnection dbConnection;


    public YoutubeDialog(DbConnection dbConnection) {
        super("New Youtube Upload");
        this.dbConnection = dbConnection;
        this.setContentPane(panelMain);
        this.pack();
        //accounts list initial load
        refreshAccountsList();

        //usernameTextField = new JTextField(30);

        buttonNewAccount.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setButtonNewAccount();
            }
        });
        buttonStartUpload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //loadVideos();
                startUpload();

            }
        });
        listAccounts.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                getSelectedAccountName();
            }
        });
        buttonBrowserFolder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                browseFolder();
            }
        });
        loadAccountsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                refreshAccountsList();
            }
        });
        deleteAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setDeleteAccountButton();
            }
        });
        addTagButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addTags();
            }
        });
        privateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                privacySetting.setText("private");
            }
        });
        publicButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                privacySetting.setText("public");
            }
        });
        clearVideosFromDbButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearVideos();
            }
        });
    }
    private void setDeleteAccountButton(){
        if(usernameTextField.getText().length() > 0){
            //delete user credentials file
            File file = new File(System.getProperty("user.home")+"/"+CREDENTIALS_DIRECTORY+"/tmp"+userName);
            file.setWritable(true);
            if(file.delete() || !file.exists()){
                System.out.println("File deleted.");
                dbConnection.deleteAccount(usernameTextField.getText());
            }
            else System.out.println("File not deleted: Task failed successfully.");
            System.out.println("Account deleted");
            //dbConnection.deleteAccount(usernameTextField.getText());
            refreshAccountsList();
        }
    }

    private void setButtonNewAccount(){
        if(usernameTextField.getText().length() > 0){
            dbConnection.insertName(usernameTextField.getText());
            refreshAccountsList();
            System.out.println(usernameTextField.getText());
        }else{
            System.out.println("Please type in username.");

        }

    }
    List<String> tags = new ArrayList<String>();

    private void addTags(){
        tags.add(tagTextField.getText());
    }

    private void loadVideos(){
        userName = usernameTextField.getText();
        int userID = dbConnection.getUserID(userName);

        Connection connection = dbConnection.connect();

        if(!videospath.isBlank()){

            File[] files = new File(videospath).listFiles();
            String descriptionText = "";
            for (File file : files) {
                //check if file is a supported video
                try {
                    if (file.isFile() && (file.toString().endsWith("mp4") ||
                            file.toString().endsWith("mov") ||
                            file.toString().endsWith("mpeg4") ||
                            file.toString().endsWith("avi") ||
                            file.toString().endsWith("flv") ||
                            file.toString().endsWith("wmv") ||
                            file.toString().endsWith("webm") ||
                            file.toString().endsWith("mov")) &&
                            Files.size(Path.of(file.getPath()))<= MAX_VIDEO_SIZE*1000) {
                        System.out.println("loadVideos(): " + file.getName());

                        //check if subtitles exist
                        String descriptionPath = videospath+"/"+file.getName()+".txt";
                        File description = new File(descriptionPath);
                        File descriptionDefault = new File(videospath+"/default.txt");

                        if (description.exists()){
                            try
                            {
                                descriptionText = new String ( Files.readAllBytes( Paths.get(descriptionPath) ) );
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }else if(descriptionDefault.exists()){
                            descriptionText = new String ( Files.readAllBytes( Paths.get(videospath+"/default.txt") ) );
                        }
                        dbConnection.insertVideoData(connection,userID,file.getName(),videospath,tags,descriptionText,privacySetting.getText());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }//because of Files.size(Path.of(file.getPath())
            }
        }
    }

    private void startUpload(){
        if(usernameTextField.getText().length() > 0 && videospath != null){
            loadVideos();
            ThreadUploadDialog threadUploadDialog = new ThreadUploadDialog();
            threadUploadDialog.setUsername(usernameTextField.getText());
            threadUploadDialog.setVideospath(videospath);
            threadUploadDialog.start();
        }
    }

    private void getSelectedAccountName(){
        accountsList = new ArrayList<String>();;
        accountsList = dbConnection.getAllAccounts();

        int index =  listAccounts.getSelectedIndex();
        if(index >=0){
            usernameTextField.setText(accountsList.get(index));
        }
    }

    private void refreshAccountsList(){
        accountsList = new ArrayList<String>();;
        accountsList = dbConnection.getAllAccounts();
        listAccountsModel = new DefaultListModel();
        listAccounts.setModel(listAccountsModel);
        listAccounts.removeAll();
        for (String s : accountsList){
            listAccountsModel.addElement(s);
        }

    }

    public void browseFolder(){
        String chooserTitle="Choose directory with videos to be uploaded";
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(chooserTitle);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //onemoguci prikaz svih datoteka
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            labelSelectedFolder.setText(chooser.getSelectedFile().toString());
            videospath = chooser.getSelectedFile().toString();
        }
        else {
            labelSelectedFolder.setText("Folder not selected");
            videospath = null;
        }

    }
    private void clearVideos(){
        dbConnection.deleteVideos(usernameTextField.getText());
    }

}
