package org.vcell.Ricky;

import java.io.IOException;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import net.imagej.ImageJ;

@Plugin(type = ContextCommand.class, menuPath = "Plugins>VCell> Full HTML Import")
public class OnlineModelSearch extends ContextCommand {
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
	 
	 public class websiteDisplayFrame extends JFrame {

		    public websiteDisplayFrame(String url) {
		       setTitle("Model Search");
		       setSize(1000, 800);
		    }
	 }    
	 
	public void run() {
		
		String url = "https://vcellapi-beta.cam.uchc.edu:8080/biomodel";
        websiteDisplayFrame frame = new websiteDisplayFrame(url);
		JEditorPane editorPane = new JEditorPane();
		loadWebsite(url, editorPane);
		editorPane.setVisible(true);
		editorPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(editorPane); 
		scrollPane.setVisible(true);
		frame.add(scrollPane);
		frame.setVisible(true);
		
	}
}