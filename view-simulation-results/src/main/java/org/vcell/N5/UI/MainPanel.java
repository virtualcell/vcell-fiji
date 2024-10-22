package org.vcell.N5.UI;

import org.vcell.N5.UI.Filters.SearchBar;
import org.vcell.N5.UI.Filters.TimeFilter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Enumeration;

public class MainPanel {
    private static JDialog exportTableDialog;
    private final int paneWidth = 800;

    public final static ControlButtonsPanel controlButtonsPanel = new ControlButtonsPanel();
    public final static N5ExportTable n5ExportTable = new N5ExportTable();
    public final ExportDetailsPanel exportDetailsPanel = new ExportDetailsPanel();
    public final RemoteFileSelection remoteFileSelection = new RemoteFileSelection();
    public final static TimeFilter timeFilter = new TimeFilter();
    public final static SearchBar searchBar = new SearchBar();

    public MainPanel(){
        JPanel parentPanel = new JPanel();


        n5ExportTable.initialize(controlButtonsPanel, exportDetailsPanel, timeFilter);
        controlButtonsPanel.initialize(n5ExportTable, remoteFileSelection);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(controlButtonsPanel, BorderLayout.SOUTH);

        parentPanel.setLayout(new BorderLayout());
        parentPanel.add(northPanel, BorderLayout.NORTH);
        JSplitPane jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, n5ExportTable, exportDetailsPanel);
        jSplitPane.setContinuousLayout(true);
        parentPanel.add(jSplitPane, BorderLayout.CENTER);

        JPanel filters = new JPanel(new BorderLayout());
        Border lowerEtchedBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        filters.setBorder(BorderFactory.createTitledBorder(lowerEtchedBorder, " Filters "));
        filters.add(timeFilter, BorderLayout.NORTH);
        filters.add(searchBar, BorderLayout.SOUTH);
        parentPanel.add(filters, BorderLayout.SOUTH);

        parentPanel.setPreferredSize(new Dimension(paneWidth, 650));
        JOptionPane pane = new JOptionPane(parentPanel, JOptionPane.PLAIN_MESSAGE, 0, null, new Object[]{"Close"});
        exportTableDialog = pane.createDialog("VCell Exports");
        exportTableDialog.setModal(false);
        exportTableDialog.setResizable(true);
        exportTableDialog.setVisible(true);
    }

    public static void changeCursor(Cursor cursor){
        exportTableDialog.setCursor(cursor);
    }

    public static void setEnableParentAndChild(Container container, boolean enable){
        container.setEnabled(enable);
        for (Component component : container.getComponents()){
            if (component instanceof Container){
                setEnableParentAndChild((Container) component, enable);
            }
            component.setEnabled(enable);
            if(component instanceof JTable){
                Enumeration<TableColumn> columns = ((JTable) component).getColumnModel().getColumns();
                while (columns.hasMoreElements()){
                    columns.nextElement().setHeaderRenderer(new DefaultTableCellRenderer(){
                        @Override
                        public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column) {
                            Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
                            c.setForeground(enable ? Color.BLACK : Color.GRAY);
                            return c;
                        }
                    });
                }
            }
        }
    }
}
