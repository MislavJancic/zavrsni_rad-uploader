package com.mjancic.rad;

import javax.swing.*;

import com.mjancic.rad.youtube.YoutubeDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Component
public class MainDialog extends JFrame{
    private JPanel mainPanel;
    private JButton youtubeButton;
    private JLabel labelBoiLabel;
    private JButton button1;

    @Autowired
    private YoutubeDialog youtubeDialog;
    public MainDialog(){
        this.youtubeDialog = youtubeDialog;
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        this.pack();
        youtubeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                newYoutubeUpload();
            }
        });
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        });
    }

    public void newYoutubeUpload(){
        labelBoiLabel.setText("new text");
        //open new frame
        youtubeDialog.setVisible(true);
    }
}
