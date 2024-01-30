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
        JTextPane textArea = new JTextPane();

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
//        textArea.setText("Intro: \n" + intro + "Accessing Simulation Results: \n" + accessingSimulationResults + "N5 and Datasets: \n" + n5AndDataSet);
        textArea.setContentType("text/html");
        textArea.setSize(width, height);
        textArea.setPreferredSize(new Dimension(width, height));
        text = HelpExplanation.insertBRTags(text, textArea);
        textArea.setText(text);
        textArea.setEditable(false);
//        textArea.setLineWrap(true);
//        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setSize(width, height);
        helperPanel.setSize(width, height);
        helperPanel.add(scrollPane);


        JOptionPane pane = new JOptionPane(helperPanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
        jDialog = pane.createDialog("Helper Menu");
        jDialog.setModal(false);
        jDialog.setPreferredSize(new Dimension(width, height));
//        jDialog.setResizable(true);
    }

    public void displayHelpMenu(){
        jDialog.setVisible(true);
    }
    private static String insertBRTags(String text, JTextPane textPane) {
        StringBuilder result = new StringBuilder();
        FontMetrics fontMetrics = textPane.getFontMetrics(textPane.getFont());
        int width = textPane.getWidth() - 10; // Adjusted width to provide some padding

        // Break the text into lines
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        boolean paragraph = false;

        for (String word : words) {
            if(word.contains("<p>")) paragraph = true;
            if(!paragraph) result.append(word).append(" ");
            if (word.contains("</p>")) {
                paragraph = false;
                line.append(word);
                result.append(line);
                line = new StringBuilder();
            }
            if(paragraph){
                if (fontMetrics.stringWidth(line.toString() + " " + word) < width) {
                    line.append(word).append(" ");
                } else {
                    result.append(line).append("<br>");
                    line = new StringBuilder(word).append(" ");
                }
            }
        }

        return "<html><body>" + result.toString() + "</body></html>";
    }
}
