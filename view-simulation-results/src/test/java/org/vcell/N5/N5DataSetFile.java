package org.vcell.N5;

import java.util.HashMap;

public class N5DataSetFile {
    public final String uri;
    public final String[] variables;
    public final HashMap<Integer, String> mask;
    public final double[][] histMax; //channel(variable), frame for [][]
    public final double[][] histMin;
    public final double[][] histAverage;
    public final double totalArea;
    public final double[] testDomainArea;
    public N5DataSetFile(String uri, String[] variables, HashMap<Integer, String> mask, double[][] histMax, double[][] histMin, double[][] histAverage, double totalArea, double[] testDomainArea){
        this.uri = uri;
        this.histMax = histMax;
        this.histMin = histMin;
        this.histAverage = histAverage;
        this.variables = variables;
        this.mask = mask;
        this.totalArea = totalArea;
        this.testDomainArea = testDomainArea;
    }
//  https://vcell-dev.cam.uchc.edu/n5Data/ezequiel23/c607b779af9481f.n5?dataSetName=4130889219

    public static N5DataSetFile[] alphaTestFiles(){
        N5DataSetFile frapSimulationResultsMasked = new N5DataSetFile("https://vcell-dev.cam.uchc.edu/n5Data/ezequiel23/c607b779af9481f.n5?dataSetName=4130889219",
                new String[]{"Dex"},
                new HashMap<Integer, String>(){{put(1, "Cyt"); put(0, "Ec");}},
                new double[][]{{10.0, 9.990392675155721, 9.83580092714469, 9.520539931524715, 9.162150060086567, 8.82335160436397, 8.523689113752786, 8.265381795870683, 8.044751960699015, 7.856809648125466,
                        7.6964442490469365, 7.559497112763759, 7.442415154355558}},
                new double[][]{{
                        0.0, 0.010215663873287447, 0.23461088459151397, 0.7634632958615505, 1.4139554046033762, 2.063749284472463, 2.6639075003302817, 3.200125640968558, 3.671910452018279, 4.083815254159328,
                        4.441933641025697, 4.752525652114409, 5.021489447662615, 5.25417298783172, 5.4553370523518065, 5.62917275251317, 5.7793451286004585, 5.909046810912881, 6.0210510711120975}},
                new double[][]{{
                        6.728509585652239, 6.7285095856523345, 6.728509585652324, 6.72850958565233, 6.728509585652335, 6.728509585652381, 6.728509585652381, 6.728509585652362, 6.728509585652365,
                        6.728509585652356, 6.7285095856523185, 6.728509585652278, 6.7285095856522865, 6.728509585652241, 6.728509585652113, 6.728509585652124, 6.7285095856521355, 6.728509585652127, 6.728509585652108}},
                484,
                new double[]{170.94880000000251,313.05120000000511});

        N5DataSetFile anns5DTIRFSimulation = new N5DataSetFile("https://vcell-dev.cam.uchc.edu/n5Data/ezequiel23/a530ce83268de2a.n5?dataSetName=5371385960",
                new String[]{"Dark", "Flour"},
                new HashMap<Integer, String>(){{put(0, "ec"); put(1, "cytosol"); put(2, "Nucleus");}},
                new double[][]{
                        {2.2250738585072014E-308, 2.2250738585072014E-308, 2.2250738585072014E-308, 3.52810806343277, 3.67238125879494,
                                0.37504412562962713, 0.1941393552238283},
                        {10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0}
                },
                new double[][]{
                        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
                        {10.0, 10.0, 10.0, 6.471891936567229, 6.32761874120506, 9.624955874370375, 9.805860644776171}
                },
                new double[][]{
                        {0.0, 0.0, 0.0, 0.007280282509686344, 0.013997372023832945, 0.013997372023832947, 0.013997372023832949},
                        {9.999999999999494, 9.999999999999494, 9.999999999999494, 9.992719717489994, 9.986002627975711,
                                9.98600262797569, 9.986002627975722}
                },
                143301.0176,
                new double[]{124412.82212538144, 15317.6945829, 3570.5008917656078}
        );

        return new N5DataSetFile[]{frapSimulationResultsMasked, anns5DTIRFSimulation};
    }

}
