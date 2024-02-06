package org.vcell.N5.UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class RemoteFileSelection extends JDialog{
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
    private int panelWidth = 500;
    private int panelHeight = 350;

    public RemoteFileSelection(JFrame parentFrame){
        super(parentFrame, true);
        this.setTitle("Remote File Selection");
        credentialsPanel = new JPanel();
        endpointPanel = new JPanel();
        this.mainPanel = new JPanel();

        credentialsCheckBox = new JCheckBox("S3 Credentials");
        endpointCheckBox = new JCheckBox("S3 Endpoint");

        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        linkTextField = new JTextField();
        linkTextField.setPreferredSize(new Dimension((panelWidth - 100), 35));
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(linkTextField, mainPanelConstraints);

        submitS3Info = new JButton("Open N5 URL");
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(submitS3Info);


        this.setContentPane(this.mainPanel);
        this.setSize(panelWidth, panelHeight);
        this.setResizable(true);



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
        if(this.s3RegionTextField.getText().isEmpty() || this.s3EndpointTextField.getText().isEmpty()){
            return null;
        }
        hashMap.put("Endpoint", this.s3EndpointTextField.getText());
        hashMap.put("Region", this.s3RegionTextField.getText());
        return hashMap;
    }

    public String getS3URL(){
        return linkTextField.getText();
    }



}
