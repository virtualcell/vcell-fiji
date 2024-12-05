package org.vcell.N5.UI.Filters;

import org.vcell.N5.UI.MainPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SearchBar extends JPanel implements KeyListener {
    public static final JTextField searchTextField = new JTextField();

    public SearchBar(){
        this.add(new JLabel("Search: "));
        searchTextField.setPreferredSize(new Dimension(600, 20));
        searchTextField.addKeyListener(this);
        this.add(searchTextField);
//        this.setPreferredSize(new Dimension(600, 20));
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getSource().equals(searchTextField)){
            MainPanel.n5ExportTable.updateTableData(searchTextField.getText());
        }
    }
}
