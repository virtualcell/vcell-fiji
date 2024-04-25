package org.vcell.N5;


import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.log.slf4j.SLF4JLogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.vcell.N5.UI.N5ExportTable;


/*
    Able to open N5 files locally, display the datasets that can be chosen from it, and open the datasets within ImageJ.
    Flow of operations is select an N5 file, get dataset list, select a dataset and then open it.
    http://vcellapi-beta.cam.uchc.edu:8088/test/test.n5

 */

@Plugin(type = Command.class, menuPath = "Plugins>VCell>VCell Simulation Results Viewer")
public class N5ImageHandler implements Command {
    public static final String formatName = "N5";
    @Parameter
    public static LogService logService;
    public static N5ExportTable exportTable;

    @Override
    public void run() {
        exportTable = new N5ExportTable();
        initializeLogService();
//        N5ImageHandler.logService.setLevel(LogService.DEBUG);
        exportTable.displayExportTable();
    }

    public static Logger getLogger(Class classToLog){
        return logService.subLogger(classToLog.getCanonicalName(), LogService.DEBUG);
    }

    public static void initializeLogService(){
        if (N5ImageHandler.logService == null){
            N5ImageHandler.logService = new SLF4JLogService();
            N5ImageHandler.logService.initialize();
        }
    }

    public static void main(String[] args) {
        N5ImageHandler n5ImageHandler = new N5ImageHandler();
        initializeLogService();
        n5ImageHandler.run();
    }

}
