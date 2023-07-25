package vcell.imagej.plugin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Node;

import java.io.IOException;
import java.net.URL;

import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URISyntaxException;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.text.html.StyleSheet;

//@SuppressWarnings("unused")
@Plugin(type = ContextCommand.class, menuPath = "Plugins>VCell> Base Version")
public class BaseImport extends ContextCommand {
    @Parameter
    private UIService uiService;

    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }

    public void loadWebsite(String url, JEditorPane editorPane) {
        try {
            editorPane.setPage(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleTableLinkClick(JTable table) {
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();
                int row = table.rowAtPoint(point);
                int column = table.columnAtPoint(point);
                Object value = table.getValueAt(row, column);

                if (value != null && value.toString().contains("<a href=")) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();
                int row = table.rowAtPoint(point);
                int column = table.columnAtPoint(point);
                Object value = table.getValueAt(row, column);

                if (value != null && value.toString().contains("<a href=")) {
                    try {
                        Document doc = Jsoup.parse(value.toString());
                        Element link = doc.select("a").first();
                        String url = link.attr("href");
                        Desktop.getDesktop().browse(new URI(url));
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private static String[] extractFormat(Element table) {
        Elements headerCells = table.select("th");
        String[] format = new String[headerCells.size()];

        // Check if there are no <th> elements
        if (headerCells.isEmpty()) {
            // Create a default format with empty strings
            for (int i = 0; i < format.length; i++) {
                format[i] = "";
            }
        } else {
            // Extract the format from the <th> elements
            for (int i = 0; i < headerCells.size(); i++) {
                format[i] = headerCells.get(i).text();
            }
        }
        return format;
    }

    private static class TableData {
        String text;
        String linkUrl;

        TableData(String text, String linkUrl) {
            this.text = text;
            this.linkUrl = linkUrl;
        }
    }

    private static TableData processTableCell(Element cell) {
        StringBuilder sb = new StringBuilder();
        String linkUrl = "";

        for (Node child : cell.childNodes()) {
            if (child instanceof TextNode) {
                sb.append(((TextNode) child).text());
            } else if (child instanceof Element && ((Element) child).tagName().equals("a")) {
                Element link = (Element) child;
                String linkText = link.text();
                linkUrl = link.attr("abs:href");
                sb.append(linkText);
            }
        }

        return new TableData(sb.toString(), linkUrl);
    }

    private static String[][] extractTableData(Element table) {
        Elements rows = table.select("tr");
        String[][] data = new String[rows.size()][];

        for (int i = 0; i < rows.size(); i++) {
            Elements cells = rows.get(i).select("td");
            data[i] = new String[cells.size()];

            for (int j = 0; j < cells.size(); j++) {
                Element cell = cells.get(j);
                TableData tableData = processTableCell(cell);
                data[i][j] = tableData.text;
            }
        }

        return data;
    }
    
    private static void handleTableLinkClick(JTable table, int row, int column) throws IOException {
        String value = (String) table.getValueAt(row, column);
        if (value != null && value.contains("<a href=")) {
            Document doc = Jsoup.parse(value);
			Element link = doc.select("a").first();
			String url = link.attr("abs:href");
			JEditorPane editorPane = new JEditorPane();
			editorPane.setEditable(false);
			editorPane.setContentType("text/html");
			editorPane.setText(value);
			editorPane.addHyperlinkListener(e -> {
			    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			        if (e instanceof HTMLFrameHyperlinkEvent) {
			            HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
			            HTMLDocument doc1 = (HTMLDocument) editorPane.getDocument();
			            doc1.processHTMLFrameHyperlinkEvent(evt);
			        } else {
			            try {
			                Desktop.getDesktop().browse(e.getURL().toURI());
			            } catch (IOException | URISyntaxException ex) {
			                ex.printStackTrace();
			            }
			        }
			    }
			});

			JScrollPane scrollPane = new JScrollPane(editorPane);
			scrollPane.setPreferredSize(new Dimension(800, 600));
			JOptionPane.showMessageDialog(null, scrollPane, "Link", JOptionPane.PLAIN_MESSAGE);
        }
    }


    @SuppressWarnings("serial")
    private class JTextAreaCellRenderer extends JTextArea implements TableCellRenderer {
        public JTextAreaCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());
            FontMetrics fm = getFontMetrics(getFont());
            int lineHeight = fm.getHeight();
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            int leading = fm.getLeading();
            int adjustedLineHeight = lineHeight - leading + descent;
            setLineHeight(adjustedLineHeight);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setText(value != null ? value.toString() : "");
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }

        private void setLineHeight(int lineHeight) {
            String html = "<html><body style='line-height:" + lineHeight + "px;'>%s</body></html>";
            setText(String.format(html, getText()));
        }
    }

    private static void adjustRowHeight(JTable table) {
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = table.getRowHeight();
            for (int column = 0; column < table.getColumnCount(); column++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }
            table.setRowHeight(row, rowHeight);
        }
    }

    @SuppressWarnings("serial")
    private static class QuotationCellRenderer extends JEditorPane implements TableCellRenderer {
        public QuotationCellRenderer() {
            setContentType("text/html");
            setEditable(false);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());
            setForeground(Color.BLUE);
            addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e instanceof HTMLFrameHyperlinkEvent) {
                        HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent) e;
                        HTMLDocument doc = (HTMLDocument) getDocument();
                        doc.processHTMLFrameHyperlinkEvent(evt);
                    } else {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (IOException | URISyntaxException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            if (value instanceof String) {
                String text = (String) value;
                setText(text);
            } else {
                setText("");
            }

            adjustWidth(table, column);
            adjustHeight(table, row, column);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }

        private void adjustWidth(JTable table, int column) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            setSize(tableColumn.getWidth(), getPreferredSize().height);
        }

        private void adjustHeight(JTable table, int row, int column) {
            int rowHeight = table.getRowHeight(row);
            int columnWidth = table.getColumnModel().getColumn(column).getWidth();

            setSize(columnWidth, Short.MAX_VALUE);
            int preferredHeight = getPreferredSize().height;
            if (preferredHeight > rowHeight) {
                rowHeight = preferredHeight;
                table.setRowHeight(row, rowHeight);
            }
        }
    }

    @SuppressWarnings("serial")
    private static class QuotationCellEditor extends DefaultCellEditor {
        private JTextArea textArea;

        public QuotationCellEditor() {
            super(new JTextField());
            textArea = new JTextArea();
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBorder(BorderFactory.createEmptyBorder());
            textArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        e.consume();
                    }
                }
            });
            setClickCountToStart(0);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textArea.setText(value != null ? value.toString() : "");
            adjustTextAreaWidth(table, column);
            return new JScrollPane(textArea);
        }

        @Override
        public Object getCellEditorValue() {
            return textArea.getText();
        }

        private void adjustTextAreaWidth(JTable table, int column) {
            int tableWidth = table.getColumnModel().getColumn(column).getWidth();
            int textWidth = textArea.getPreferredSize().width;
            if (textWidth < tableWidth) {
                textArea.setSize(tableWidth, textArea.getPreferredSize().height);
            }
        }
    }

    public void run() {
        try {
            String string = new String();
            String url = new String();
            JFrame ui = new JFrame();

            GenericDialog box = new GenericDialog("Web Model Search");

            box.addStringField("Model Name:", string, 45);
            box.addStringField("Model ID:", string, 45);
            box.addStringField("Model Owner:", string, 45);
            box.addStringField("Begin Date:", string, 45);
            box.addStringField("End Date:", string, 45);
            box.showDialog();

            boolean shouldContinue = true;

            while (shouldContinue) {
                if (box.wasCanceled() && !box.isVisible()) {
                    shouldContinue = false;
                    break;
                }

                if (box.wasOKed()) {
                    String modelName = box.getNextString();
                    String modelID = box.getNextString();
                    String modelOwner = box.getNextString();
                    String beginDate = box.getNextString();
                    String endDate = box.getNextString();

                    JFrame frame = new JFrame("Search Results");

                    url = "https://vcellapi-beta.cam.uchc.edu:8080/biomodel?bmName=" + modelName + "&bmId=" + modelID
                            + "&category=all" + "&owner=" + modelOwner + "&savedLow=&savedHigh=&startRow=1&maxRows=100&orderBy=date_desc";

                    try {
                        Document doc = Jsoup.connect(url).get();
                        doc.select("tbody").first().remove();

                        String[][] tableData = extractTableData(doc);
                        String[] columns = extractFormat(doc);

                        DefaultTableModel finalFormat = new DefaultTableModel(tableData, columns);

                        JTable table = new JTable(finalFormat);
                        adjustRowHeight(table);
                        table.setDefaultRenderer(Object.class, new QuotationCellRenderer());
                        table.setDefaultEditor(Object.class, null);
                        table.getColumnModel().getColumn(3).setCellRenderer(new QuotationCellRenderer());

                        table.addMouseListener(new MouseAdapter() {
                            public void mouseClicked(MouseEvent e) {
                                int row = table.rowAtPoint(e.getPoint());
                                int col = table.columnAtPoint(e.getPoint());
                                try {
									handleTableLinkClick(table, row, col);
								} catch (IOException e1) {
									e1.printStackTrace();
								}
                            }
                        });

                        JScrollPane panel = new JScrollPane(table);
                        panel.setPreferredSize(new Dimension(1500, 500));

                        frame.add(panel);
                        frame.pack();
                        frame.setVisible(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            ui.add(box);
            ui.pack();
            ui.setVisible(true);
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }
}
