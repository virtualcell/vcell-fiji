# Goal
To build Fiji plugins which allow for easy use of the VCell servers, allowing for the best of both worlds.
A powerful image processing tool and the models generated from VCells simulations. There
are already plenty of solutions which help to handle large amounts of data,
the only thing is that data is mounted on the users computer.

When trying to access these files in Bio-Format

# Problem
Users of VCell are able to create a simulated model, but exporting and analyzing data from these models
is not intuitive and can be laborious.

If the generated models can be viewed within ImageJ, then the tools already present within ImageJ itself can
be used to process the image and export data easily.

***Hurdles***
- Loading the images within ImageJ may take an extensive amount of time because of how large they are
- 

# Potential Solutions
- Access the VCell API
  - VCell already has a REST API that can handle exportation of data, and it can be granular
  - The API might not be granular enough, or does not contain certain features which are required

- Big Data Viewer/Processor


Have the simulation result within the remote repository.
Preview the data preemptively with the Big Data viewer.
Then stream the information to the user when they want to access the model within ImageJ.
