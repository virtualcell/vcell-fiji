## IDE
The IDE used to develop within this project is IntelliJ due to
- Speciality in developing Java projects
- Modern interface which allows for easy development
- Extensive plugin marketplace (although some have to be paid for)

## GUI Libraries
Java swing seems to be the best suited library for GUI development
- Still supported, and will continue to be supported
- Supports development within legacy systems
- Integrated in Java base set libraries since forever

Although there is the issue of it not supporting hardware acceleration or 3D graphics.

## Sci-Java
[Sci-Java](https://imagej.net/libs/scijava) is a collaboration project which integrates software for scientific processing.
The libraries and applications which are most interesting are
- [ImgLib2](https://imagej.net/libs/imglib2)
  - Core libraries for N-dimensional image processing.
  - Focus on displaying images and the processing of them.
- [SCIFO](https://imagej.net/libs/scifio)
  - Core libraries for N-dimensional image I/O.
  - Does not actually display the images
- [Fiji](https://imagej.net/software/fiji)
  - The [ImageJ2](https://imagej.net/software/imagej2/) application with a large set of plugins included.
- Big Data
  - Two part system, one for clients and other for the server
  - [BigDataServer](https://imagej.net/plugins/bdv/server)
    - An application which runs on a server, and handles clients requests to view large image files, supposedly doing it extremely efficiently and quickly
    - Need to open it within a hyper stack, it only shows the 
  - [BigDataViewer](https://imagej.net/plugins/bdv)
    - Re-slicing browser and Fiji plugin for terabyte-sized multi-view image sequences

## ImageJ Plugin Development
ImageJ seems to be the central application where plugins are added to and other development. Fiji is just ImageJ with plugins already integrated.
Development for ImageJ should carry over to plugins for Fiji.
There is a [guide](https://imagej.net/develop/plugins) for developing these plugins. 
Some of the key takeaways are
- Declaring the main class as a plugin so that ImageJ can recognize it
- Using the context when ImageJ initializes
  - When using variables that control large segments of ImageJ such as the Logs, it is best to use pre-created parameters using a @parameter decorator otherwise the new instance will not have any pointers connecting it to ImageJ itself.
- Plugin Types
  - If the plugin desires to be accessible to other plugins, it should be declared as a service
    - When a plugin is declared as a service then there can only be one instance of it associated with the ImageJ context
  - If the plugin is a function that is running alone with an expected output it should be declared as a command
  - There are also many more plugin types which can be used as well
- Using native Java libraries (libraries written in a different language used by the system) can be used but would be a [pain](https://imagej.net/develop/native-libraries)
- Tips for developers transition from other C++ to [java](https://imagej.net/develop/cpp-tips)
- There is quick-hand list of [tips](https://imagej.net/develop/tips) for developers for completing essential tasks most plugins need to do

### Development [lifecycle](https://imagej.net/develop/releasing)
***Development*** 

For debugging there is a [template](https://github.com/imagej/example-imagej2-command) intended to be used.

Still nothing concrete found though on how to actually debug the application.
There are plenty of resources to integrate a plugin within Fiji, but nothing to 
execute the application and debug.

It does seem that taking a jar file, and sticking it within the Fiji plugins folder allows
for Fiji to recognize the plugin, but that seems to be a static fix , and does not allow
dynamic debugging.

Some potential leads into setting up a proper debugging setup:

- https://imagej.net/develop/debugging
- https://i-ric.org/en/forum/launching-fiji-within-intellij-java-program/
- https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000503104-ImageJ-scripting?page=1#community_comment_360000312360
- https://github.com/imagingbook/imagej1-plugins-ide-setup/tree/master
- https://intellij-support.jetbrains.com/hc/en-us/community/posts/360002617360-Writting-ImageJ-Fiji-in-IntelliJ
- https://micro-manager.org/How_to_debug_and_develop_MM2.0
- 

Have it similar to VCell current debugging

***Production***

Commit to the main branch so that the changes can be solidified within the project.

***Release***

Once an accumulation of changes within the main branch is created, create a jar
file of the project (maven artifact). This file can be submitted to the Maven project
so that other developers can add to the plugin if wanted, but more importantly for the
users of ImageJ, to the [updater website](https://imagej.net/update-sites/setup#creating-a-hosted-update-site).

The updater website is the ecosystem of plugins for ImageJ, and allows for easy
implementation of plugins and updating.

Can use GitHub actions such that when a commitment is made to a specific branch,
it does the entire process of one final ring of test cases, and than can publish the
compiled JAR files to the required repositories.