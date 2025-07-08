---
title: "CLI prerequisites online"
---


The following section explains more about the software prerequisites that you need so that you are ready to install Galasa from the Galasa CLI repository in GitHub.


### Java JDK 

Required. Galasa tests and Managers are written in Java - you need to install a Java version 11 JDK or later to use it. _Note:_ We do not currently support Java 21 or later. After installing, you must set the `JAVA_HOME` environment variable to your Java JDK installation path and check it set successfully by running the command `echo $JAVA_HOME` on Mac or Unix, or `echo %JAVA_HOME%` on Windows (PowerShell). The returned result shows the path to your JDK installation.


### Maven 

You must install either Maven or Gradle in order to build Galasa projects, which are hierarchical file structures that provide the ability to store and run Galasa tests.  

### Gradle

You must install either Maven or Gradle in order to build Galasa projects, which are hierarchical file structures that provide the ability to store and run Galasa tests. 

The following table shows the current compatibility between Gradle and Galasa versions: 


| Gradle release |  Compatible Galasa version  |
| :---- | :-------- | 
| 6.8.x  | All |
| 6.9.x  | All |
| 7.x.x | All | 
| 8.x.x | 0.36.0 or later |


If you are upgrading to Gradle version 8 from an earlier version, see the `Upgrading tests to compile using Gradle version 8` section in the [Upgrading](../upgrading/index.md) documentation to understand the changes you need to make to create Galasa projects and build and compile Galasa test code. 

Remember to add Gradle to your PATH. You can check by running `echo $PATH` on Mac or Unix, or `echo %PATH%` on Windows (PowerShell).


### 3270 emulator 

Optional. Although you do not need a 3270 emulator to run a Galasa test (even if it tests a 3270 application) you can use one to [run Galasa Simbank online](../running-simbank-tests/simbank-cli.md), a simulated version of an application that helps you get acquainted with Galasa before connecting to a real mainframe to run your own tests. There are many such emulators available but IBM's Personal Communications (PCOMM) is frequently used, as is IBM's Host on Demand software, which includes support for Windows, Linux and MacOS.


## Next steps

To install Galasa for using in the command line, follow the instructions in the [Installing the Galasa CLI online](./installing-cli-tool.md) documentation.

