### Intro
The VCell application can export the simulations it creates into multiple formats.
The .N5 format type is stored remotely on VCell servers, allowing other applications, such as ImageJ,
to access the results of these simulations, perform various types of analyses,
and avoid the need to save the simulation results locally.

### Accessing Simulation Results
There are three methods for accessing simulation results within this plugin:

1. Copy the export link from the VCell app, click the 'Remote Files' button, paste the link in the text box, and then open it.
This method is best when you need to save a set of exports for the long term future.

2. Click the recent export button to open the most recent simulation you exported in the N5 format.

3. Open the export table and access any export within that table using the open button. The table's information
is stored locally on your computer. If your computer's drive fails, the table will be empty, but any links
saved elsewhere can still be used.


### N5 and Datasets
N5 files correspond one-to-one with simulations within VCell in terms of grouping. 
Data sets serve as the grouping category for different export variances.
For example, with simulation A, if you choose to export only 2 of 3 variables,
that constitutes one dataset. In the same simulation, if you decide to export just 1 variable,
that would be an entirely different dataset.

Now, considering simulation B, if you choose to export 2 of 3 variables, it results in a different N5 file and a distinct dataset.

