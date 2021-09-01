package com.mjancic.rad.youtube;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.util.Preconditions;
import com.mjancic.rad.Browser;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class AuthorizationCodeInstalledAppOverride extends AuthorizationCodeInstalledApp {

    /** Authorization code flow. */
    private final AuthorizationCodeFlow flow;

    /** Verification code receiver. */
    private final VerificationCodeReceiver receiver;

    private static final Logger LOGGER =
            Logger.getLogger(AuthorizationCodeInstalledApp.class.getName());

    /**
     * @param flow     authorization code flow
     * @param receiver verification code receiver
     */
    public AuthorizationCodeInstalledAppOverride(AuthorizationCodeFlow flow, VerificationCodeReceiver receiver) {
        super(flow, receiver);
        this.flow = Preconditions.checkNotNull(flow);
        this.receiver = Preconditions.checkNotNull(receiver);
    }


    @Override
    public Credential authorize(String userId) throws IOException {
        try {
            Credential credential = flow.loadCredential(userId);
            if (credential != null
                    && (credential.getRefreshToken() != null || credential.getExpiresInSeconds() > 60)) {
                return credential;
            }
            // open in browser
            String redirectUri = receiver.getRedirectUri();
            System.out.println(redirectUri);

            AuthorizationCodeRequestUrl authorizationUrl =
                    flow.newAuthorizationUrl().setRedirectUri(redirectUri);
            onAuthorization(authorizationUrl);

            new Thread(()-> openInBrowser(authorizationUrl.toString())).start();
            // receive authorization code and exchange it for an access token
            String code = receiver.waitForCode();


            TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
            // store credential and return it
            return flow.createAndStoreCredential(response, userId);
        } finally {
            receiver.stop();
        }
    }

    private void openInBrowser(String redirectUri) {
        //open URI in browser
        try {
            Browser.browse(new URI(redirectUri));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
