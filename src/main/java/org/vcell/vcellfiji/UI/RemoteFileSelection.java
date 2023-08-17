package org.vcell.vcellfiji.UI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class RemoteFileSelection extends JFrame{
    private JPanel mainPanel;
    private JTextField linkTextField;
    private JTextField s3AccessKeyTextField;
    private JTextField s3SecretKeyTextField;
    public JCheckBox credentialsCheckBox;
    private JTextField s3EndpointTextField;
    private JTextField s3RegionTextField;
    private JTextField s3BucketNameTextField;
    public JCheckBox endpointCheckBox;
    public JButton submitS3Info;
    private JPanel credentialsPanel;
    private JPanel endpointPanel;

    public RemoteFileSelection(){
        this.setTitle("Remote File Selection");
        this.setContentPane(this.mainPanel);
        this.setSize(400, 400);


        this.credentialsPanel.setVisible(false);
        this.endpointPanel.setVisible(false);

        this.credentialsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                credentialsPanel.setVisible(!credentialsPanel.isVisible());
            }
        });

        this.endpointCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                endpointPanel.setVisible(!endpointPanel.isVisible());
            }
        });
    }

    public HashMap<String, String> returnCredentials(){
        HashMap<String, String> hashMap = new HashMap<>();
        if (this.s3AccessKeyTextField.getText().isEmpty() || this.s3SecretKeyTextField.getText().isEmpty()){
            return null;
        }
        hashMap.put("AccessKey", this.s3AccessKeyTextField.getText());
        hashMap.put("SecretKey", this.s3SecretKeyTextField.getText());
        return hashMap;
    }

    // having this be an enum map instead of hash map would be better
    public HashMap<String, String> returnEndpoint(){
        HashMap<String, String> hashMap = new HashMap<>();
        if(this.s3BucketNameTextField.getText().isEmpty() || this.s3RegionTextField.getText().isEmpty() || this.s3EndpointTextField.getText().isEmpty()){
            return null;
        }
        S3Config s3Config;
        hashMap.put("Endpoint", this.s3EndpointTextField.getText());
        hashMap.put("Region", this.s3RegionTextField.getText());
        hashMap.put("BucketName", this.s3BucketNameTextField.getText());
        return hashMap;
    }

    public String getS3URL(){
        return linkTextField.getText();
    }



    public enum S3Config{
        Endpoint,
        Region,
        BucketName
    }

    public enum S3Credentials{
        AccessKey,
        SecretKey
    }


}
