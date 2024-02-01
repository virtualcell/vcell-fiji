### Intro
The [VCell](https://vcell.org/) application can export spatial simulation results into multiple formats, with
[.N5](https://imagej.net/libs/n5) being one of them. N5 exports are 
stored remotely on VCell servers, allowing applications such as ImageJ
to access the results of these simulations, perform various types of analyses,
and avoid the need to save the simulation results locally.

### VCell Spatial Simulation
Simulation solvers generating 1-, 2-, or 3-dimensional spatial 
domains can be visualized in ImageJ using this plugin. 
Spatial dimensions and time directly correspond to fields within an ImageJ 
image. The image's channels depict different variables within a simulation.

### N5 and Datasets
Each N5 file is a direct mapping to a VCell simulation, and datasets are the
exports of that simulation.
It can be visualized as a box with the 
label of a simulation, and each
dataset is a different export for that simulation all 
stored within the same box. For every N5 file there is a link associated
with it which allows it to be accessed by any application that supports N5.

For example, with simulation A, if you choose to export only 2 of 3 variables,
that constitutes one dataset. In the same simulation, if you decide to export just 1 variable,
that would be an entirely different dataset. Now, considering simulation B, if you choose to
export 2 of 3 variables, it results in a different N5 file and a distinct dataset.

### Accessing Simulation Results
There are three methods for accessing simulation results within this plugin:

1. Get an N5 link, click the 'Remote Files' button, paste the link in the text box,
then view and select the dataset you want to open.

2. For exports executed on the same device this plugin is installed the following access
methods are available.
   1. Click the recent export button.

   2. The export table is populated with past exports and all the affiliated metadata
   needed to understand what was exported.

