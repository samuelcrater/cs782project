Sam Crater
Term project for the Spring 2021 section of CS782.

This is a Java implementation of Extended Isolation Forest with a modified partitioning criteria.

This repository is an Eclipse project; you may run it either through Eclipse or through the command line.

Eclipse:
You may load the project in Eclipse via File -> "Open project from file system". You then need to add the included library to the class path. Right click on project -> Build Path -> Configure Build Path -> Libraries -> Add External Jar. You can then hit "Run" (Ctrl + F11)

Command line:
From a command line in the project directory, run the following:
	javac -cp ".;commons-math3-3.6.1.jar" -d "./bin" src/*.java
	java -cp ".;commons-math3-3.6.1.jar;bin" Utility

Note: On Linux systems, ";" should be ":"