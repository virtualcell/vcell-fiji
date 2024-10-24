package org.vcell.N5.UI;

import org.vcell.N5.N5ImageHandler;
import org.vcell.N5.retrieving.SimResultsLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class RemoteFileSelection extends JDialog implements ActionListener{
    private JPanel mainPanel;
    private final JTextField linkTextField;
    private final JTextField s3AccessKeyTextField;
    private final JTextField s3SecretKeyTextField;
    public JCheckBox credentialsCheckBox;
    private final JTextField s3EndpointTextField;
    private final JTextField s3RegionTextField;
    private JTextField s3BucketNameTextField;
    public JCheckBox endpointCheckBox;
    public JButton submitS3Info;
    private JPanel credentialsPanel;
    private JPanel endpointPanel;

    public RemoteFileSelection(){
        this(null);
    }
    public RemoteFileSelection(JFrame parentFrame){
//        super(parentFrame, false);
        this.setTitle("Remote File Selection");
        credentialsPanel = new JPanel();
        endpointPanel = new JPanel();
        this.mainPanel = new JPanel();

        s3BucketNameTextField = new JTextField();
        s3RegionTextField = new JTextField();
        s3AccessKeyTextField = new JTextField();
        s3EndpointTextField = new JTextField();
        s3SecretKeyTextField = new JTextField();

        credentialsCheckBox = new JCheckBox("S3 Credentials");
        endpointCheckBox = new JCheckBox("S3 Endpoint");

        GridBagConstraints mainPanelConstraints = new GridBagConstraints();
        linkTextField = new JTextField();
        int panelWidth = 500;
        linkTextField.setPreferredSize(new Dimension((panelWidth - 100), 35));
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 0;
        mainPanel.add(linkTextField, mainPanelConstraints);

        submitS3Info = new JButton("Open N5 URL");
        mainPanelConstraints.gridx = 0;
        mainPanelConstraints.gridy = 1;
        mainPanel.add(submitS3Info);

        this.setContentPane(this.mainPanel);
        int panelHeight = 350;
        this.setSize(panelWidth, panelHeight);
        this.setResizable(true);

        this.credentialsPanel.setVisible(false);
        this.endpointPanel.setVisible(false);

        submitS3Info.addActionListener(this);
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


    @Override
    public void actionPerformed(ActionEvent e) {
        SimResultsLoader simResultsLoader = new SimResultsLoader(getS3URL(), "", -1, "");
        AdvancedFeatures advancedFeatures = MainPanel.controlButtonsPanel.advancedFeatures;
        N5ImageHandler.loadingManager.openN5FileDataset(new ArrayList<SimResultsLoader>(){{add(simResultsLoader);}},
                advancedFeatures.inMemory.isSelected(), advancedFeatures.rangeSelection.isSelected(),
                advancedFeatures.dataReduction.isSelected());
        this.setVisible(false);
    }
}
