package com.mjancic.rad.youtube;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.collect.Lists;
import com.mjancic.rad.database.DbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

@Component
public class Auth {

    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials"; // at ~/.oauth-credentials/

    DbConnection dbConnection;

    public Auth() {
        dbConnection = new DbConnection();
    }

    public GoogleAuthorizationCodeFlow getAuthFlow(String name) throws IOException {
        List<String> scopes = Lists.newArrayList(
                "https://www.googleapis.com/auth/youtube.upload");

        Reader clientSecretReader = new StringReader(dbConnection.getClientSecrets());
        GoogleClientSecrets clientSecrets = null;
        try {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataStore<StoredCredential> datastore = null;
        try {
            FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(
                    new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
            datastore = fileDataStoreFactory.getDataStore("tmp" + name);


        } catch (IOException e) {
            e.printStackTrace();
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes).setCredentialDataStore(datastore)
                .build();

        return flow;
    }

    public Credential authorize(String user) throws IOException {
        // Build the local server and bind it to port 8080
        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();
        // Authorize.
        AuthorizationCodeInstalledAppOverride app = new AuthorizationCodeInstalledAppOverride(getAuthFlow(user),
                localReceiver);

        Credential credential = app.authorize(user);

        System.out.println("Auth: user = " + user);
        return credential;
    }


}
