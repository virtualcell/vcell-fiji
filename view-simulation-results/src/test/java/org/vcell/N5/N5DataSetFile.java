package org.vcell.N5;

import java.util.HashMap;

public class N5DataSetFile {
    public String uri;
    public String[] variables;
    public HashMap<Integer, String> mask;
    public double[][] histMax; //channel(variable), frame for [][]
    public double[][] histMin;
    public double[][] histAverage;
    public N5DataSetFile(String uri, String[] variables, HashMap<Integer, String> mask, double[][] histMax, double[][] histMin, double[][] histAverage){
        this.uri = uri;
        this.histMax = histMax;
        this.histMin = histMin;
        this.histAverage = histAverage;
        this.variables = variables;
        this.mask = mask;
    }

    public static N5DataSetFile[] alphaTestFiles(){
        N5DataSetFile frapSimulationResultsMasked = new N5DataSetFile("https://vcell-dev.cam.uchc.edu/n5Data/ezequiel23/c607b779af9481f.n5?dataSetName=6262029569",
                new String[]{"Dex"},
                new HashMap<Integer, String>(){{put(1, "Cyt"); put(0, "Ec");}},
                new double[][]{{10.0, 9.990392675155721, 9.83580092714469, 9.520539931524715, 9.162150060086567, 8.82335160436397, 8.523689113752786, 8.265381795870683, 8.044751960699015, 7.856809648125466,
                        7.6964442490469365, 7.559497112763759, 7.442415154355558}},
                new double[][]{{
                        0.0, 0.010215663873287447, 0.23461088459151397, 0.7634632958615505, 1.4139554046033762, 2.063749284472463, 2.6639075003302817, 3.200125640968558, 3.671910452018279, 4.083815254159328,
                        4.441933641025697, 4.752525652114409, 5.021489447662615, 5.25417298783172, 5.4553370523518065, 5.62917275251317, 5.7793451286004585, 5.909046810912881, 6.0210510711120975}},
                new double[][]{{
                        6.728509585652239, 6.7285095856523345, 6.728509585652324, 6.72850958565233, 6.728509585652335, 6.728509585652381, 6.728509585652381, 6.728509585652362, 6.728509585652365,
                        6.728509585652356, 6.7285095856523185, 6.728509585652278, 6.7285095856522865, 6.728509585652241, 6.728509585652113, 6.728509585652124, 6.7285095856521355, 6.728509585652127, 6.728509585652108}});

        return new N5DataSetFile[]{frapSimulationResultsMasked};
    }

}
