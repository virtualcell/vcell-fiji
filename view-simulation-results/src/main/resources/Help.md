### Intro
The VCell (https://vcell.org/) application can export spatial simulation results into multiple formats, with
.N5 (https://imagej.net/libs/n5) being one of them. N5 exports are 
stored remotely on VCell servers, allowing applications such as ImageJ
to directly access the results of these simulations and perform analyses.

### VCell Spatial Simulation
Simulation solvers generating 2- or 3-dimensional spatial 
domains can be visualized in ImageJ using this plugin. 
Spatial dimensions and time directly correspond to fields within an ImageJ 
image. The image's channels depict different variables within a simulation.

### N5 and Datasets
Each N5 store is a direct mapping to a VCell simulation, and contains one or more
datasets. Each dataset holds numerical data and metadata corresponding to an Image in ImageJ.
Each N5 store is fully identified through its N5 URL which can be shared and opened by other applications
which support the N5 format.


### Accessing Simulation Results

For VCell exports generated from your local VCell installation:
- Either, Click the recent export button.

- Or, Open the export table and view all past exports with their affiliated metadata.

From N5 URL (VCell Install Not Required):
- Click the 'Remote Files' button, paste the N5 URL,
then select dataset from list.

