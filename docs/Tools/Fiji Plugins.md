

### Big Data Server
Very simple java application that is ran separately from Fiji. It is ran on the server that has
all the data files that are desired to be shared. Its [github page](https://github.com/bigdataviewer/bigdataviewer-server/)
is rarely updated, and does not contain any documentation.

It seems to be more of a feature that is tacked onto big data viewer, with itself
being a plugin that does not seem extremely extensive. Can not save the images
after simple processing using big data viewer.

### Big Data Viewer
A plugin that allows for viewing large files in an efficient manner. 
The tools that allow for processing these images is not as extensive as other plugins.


### [Big Data Processor](https://academic.oup.com/bioinformatics/article/37/18/3079/6140778?login=true)
Extensive tool for analyzing large images efficiently, but seems to not work with its own examples
because some libraries are missing from the Fiji version.


### Bio Formats
Innate plugin to the ImageJ application, it helps allow for so many different file formats to be accessible.
It has a remote access tool which allows you to open an image remotely. Seems as if once the image is opened
the majority of tools for analysis for the image are available.

To open large files it requires using a virtual stack though. I am still not sure if it can
be the solution to opening large files though. It may efficient enough for our model
results though.

No spatial context, only part of the data can be viewed, and slow for high resolution files.


### [N5-IJ](https://github.com/saalfeldlab/n5-ij)

https://forum.image.sc/t/n5-plugins-for-fiji/45469


### [MoBIE](https://github.com/mobie/mobie-viewer-fiji)
Plugin for exploring and sharing big multi-modal image and associated tabular data.
Helps to share specific setup of image analysis.
Can access large OME.ZARR files remotely through S3 buckets. It then opens these files
within big data viewer.

https://www.youtube.com/channel/UCtRtv0JkEEW5zLFO7d_nglg


### Big Data Viewer Playground
Allows for an extension of features to the vanilla big data viewer. One of the important
features is its ability to [open remote OMERO files](https://omero-guides.readthedocs.io/en/latest/fiji/docs/view_mobie_zarr.html)


https://www.youtube.com/watch?v=LHI7vXiUUms&t=4534s

https://forum.image.sc/











