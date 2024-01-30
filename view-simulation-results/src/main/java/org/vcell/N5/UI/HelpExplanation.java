package org.vcell.N5.UI;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HelpExplanation {
    private JDialog jDialog;

    public HelpExplanation(){
        int height = 400;
        int width = 700;

        JPanel helperPanel = new JPanel();
        JTextPane textPane = new JTextPane();

        Parser parser = Parser.builder().build();
        String md;
        URL mdPath = ClassLoader.getSystemClassLoader().getResource("Help.md");
        try {
            md = new String(Files.readAllBytes(Paths.get(mdPath.getPath())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Node node = parser.parse(md);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String text = renderer.render(node);
        textPane.setContentType("text/html");
        textPane.setSize(width, height);
        textPane.setPreferredSize(new Dimension(width, height));
        textPane.setText(text);
        textPane.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setSize(width, height);
        helperPanel.setSize(width, height);
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
