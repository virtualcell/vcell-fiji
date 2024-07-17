package org.vcell.N5.UI;


import com.google.common.io.CharStreams;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;

public class HelpExplanation {
    private JDialog jDialog;

    public HelpExplanation(){
        int height = 400;
        int width = 700;

        JPanel helperPanel = new JPanel();
        JTextPane textPane = new JTextPane();

        String text;
        try {
            InputStream inputStream = ClassLoader.getSystemResourceAsStream("Help.html");
            text = CharStreams.toString(new InputStreamReader(inputStream));
//            text = new String(Files.readAllBytes(Paths.get(mdPath.getPath())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        textPane.setContentType("text/html");
        textPane.setSize(width, 1000);
        textPane.setPreferredSize(new Dimension(width, 1000));
        textPane.setText(text);
        textPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setSize(width, height);
        scrollPane.setPreferredSize(new Dimension(width, height));
        helperPanel.setSize(width, height);
        helperPanel.setPreferredSize(new Dimension(width, height));
        helperPanel.add(scrollPane);


        JOptionPane pane = new JOptionPane(helperPanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
        jDialog = pane.createDialog("Helper Menu");
        jDialog.setModal(false);
        jDialog.setPreferredSize(new Dimension(width, height));
    }

    public void displayHelpMenu(){
        jDialog.setVisible(true);
    }
}
