package org.vcell.vcellfiji;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VCellGUI extends JFrame {
    private JButton LocalFiles;
    public JPanel mainPanel;
    private final JFrame jFrame;
    public JFileChooser localFileDialog;

    private JToolBar menuBar;

    public VCellGUI() {
        jFrame = this;
        localFileDialog = new JFileChooser();
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            SwingUtilities.updateComponentTreeUI(jFrame);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.setTitle("VCell Manager");
        this.setContentPane(this.mainPanel);
        this.setSize(300, 300);
        this.setVisible(true);
        

        LocalFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                localFileDialog.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                localFileDialog.showOpenDialog(jFrame);
//                System.out.print(localFileDialog.getSelectedFile());
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
