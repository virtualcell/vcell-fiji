/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


package org.vcell.Ricky;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.MacroInstaller;
import ij.plugin.PluginInstaller;
import ij.plugin.URLOpener;
import ij.plugin.frame.*;
import java.awt.Color;

import org.scijava.command.ContextCommand;

import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import java.io.File;
import java.io.IOException;


@Plugin(type = ContextCommand.class, menuPath = "Plugins>VCell> Load Web Image")
public class OnlineImageLoad extends ContextCommand {
	private static String url = null;

    /** If 'urlOrName' is a URL, opens the image at that URL. If it is
        a file name, opens the image with that name from the 'images.location'
        URL in IJ_Props.txt. If it is blank, prompts for an image
        URL and open the specified image. */
    public void run() {
        GenericDialog box = new GenericDialog("Web Image Load");
        box.setInsets(10, 32, 0);
        box.addMessage("Enter Model ID", null, Color.darkGray);
        box.addStringField("ID:", url, 45);
        box.showDialog();
        if (box.wasCanceled())
            return;
        url = box.getNextString();
        url = url.trim();
            url = "https://vcellapi-beta.cam.uchc.edu:8080/biomodel/" + url + "/diagram";
       /* if (url.endsWith("/"))
            IJ.runPlugIn("ij.plugin.BrowserLauncher", url.substring(0, url.length()-1)); */
       /* else if (url.endsWith(".html") || url.endsWith(".htm") || url.endsWith(".pdf") ||  url.indexOf(".html#")>0 || noExtension(url))
            IJ.runPlugIn("ij.plugin.BrowserLauncher", url); */
         if (url.endsWith(".txt")||url.endsWith(".ijm")||url.endsWith(".js")||url.endsWith(".java"))
            openTextFile(url, false);
        else if (url.endsWith(".jar")||url.endsWith(".class"))
            IJ.open(url);
        else {
            IJ.showStatus("Opening: " + url);
            double startTime = System.currentTimeMillis();
            ImagePlus imp = new ImagePlus(url);
            WindowManager.checkForDuplicateName = true;
            FileInfo fi = imp.getOriginalFileInfo();
            if (fi!=null && fi.fileType==FileInfo.RGB48)
                imp = new CompositeImage(imp, IJ.COMPOSITE);
            else if (imp.getNChannels()>1 && fi!=null && fi.description!=null && fi.description.indexOf("mode=")!=-1) {
                int mode = IJ.COLOR;
                if (fi.description.indexOf("mode=composite")!=-1)
                    mode = IJ.COMPOSITE;
                else if (fi.description.indexOf("mode=gray")!=-1)
                    mode = IJ.GRAYSCALE;
                imp = new CompositeImage(imp, mode);
            }
            if (fi!=null && (fi.url==null || fi.url.length()==0)) {
                fi.url = url;
                imp.setFileInfo(fi);
            }
            imp.show(Opener.getLoadRate(startTime,imp));
        }
        IJ.register(URLOpener.class);  
    }
    
    boolean noExtension(String url) {
        int lastSlash = url.lastIndexOf("/");
        if (lastSlash==-1) lastSlash = 0;
        int lastDot = url.lastIndexOf(".");
        if (lastDot==-1 || lastDot<lastSlash || (url.length()-lastDot)>6)
            return true; 
        else
            return false;
    }
    
    void openTextFile(String urlString, boolean install) {
        StringBuffer sb = null;
        try {
            URL url = new URL(urlString);
            InputStream in = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            sb = new StringBuffer() ;
            String line;
            while ((line=br.readLine()) != null)
                sb.append (line + "\n");
            in.close ();
        } catch (IOException e) {
            if  (!(install&&urlString.endsWith("StartupMacros.txt")))
                IJ.error("URL Opener", ""+e);
            sb = null;
        }
        if (sb!=null) {
            if (install)
                (new MacroInstaller()).install(new String(sb));
            else {
                int index = urlString.lastIndexOf("/");
                if (index!=-1 && index<=urlString.length()-1)
                    urlString = urlString.substring(index+1);
                (new Editor()).create(urlString, new String(sb));
            }
        }
    }
    
    private void cacheSampleImages() {
        String[] names = getSampleImageNames();
        int n = names.length;
        if (n==0) return;
        String dir = IJ.getDirectory("imagej")+"samples";
        File f = new File(dir);
        if (!f.exists()) {
            boolean ok = f.mkdir();
            if (!ok) {
                IJ.error("Unable to create directory:\n \n"+dir);
                return;
            }
        }
        IJ.resetEscape();
        for (int i=0; i<n; i++) {
            IJ.showStatus((i+1)+"/"+n+" ("+names[i]+")");
            String url = Prefs. getImagesURL()+names[i];
            byte[] data = PluginInstaller.download(url, null);
            if (data==null) continue;
            f = new File(dir,names[i]);
            try {
                FileOutputStream out = new FileOutputStream(f);
                out.write(data, 0, data.length);
                out.close();
            } catch (IOException e) {
                IJ.log(names[i]+": "+e);
            }
            if (IJ.escapePressed())
                {IJ.beep(); break;};
        }
        IJ.showStatus("");
    }
     
    public static String[] getSampleImageNames() {
        ArrayList<String> list = new ArrayList<>();
        Hashtable commands = Menus.getCommands();
        Menu samplesMenu = Menus.getImageJMenu("File>Open Samples");
        if (samplesMenu==null)
            return new String[0];
        for (int i=0; i<samplesMenu.getItemCount(); i++) {
            MenuItem menuItem = samplesMenu.getItem(i);
            if (menuItem.getActionListeners().length == 0) continue; 
            String label = menuItem.getLabel();
            if (label.contains("Cache Sample Images")) continue;
            String command = (String)commands.get(label);
            if (command==null) continue;
            String[] items = command.split("\"");
            if (items.length!=3) continue;
            String name = items[1];
            list.add(name);
        }
        return (String[])list.toArray(new String[list.size()]);
    }
}

