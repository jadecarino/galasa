---
title: "z/OS File RSE API Manager"
---

This Manager is at Alpha level. You can view the [Javadoc documentation for the Manager](https://javadoc.galasa.dev/overview-summary.html){target="_blank"}.


## Overview

This Manager is an internal implementation of the z/OS File Manager using RSE API. The RSE API File Manager is used in conjunction with the z/OS Manager. The z/OS Manager provides the interface for the z/OS file function and pulls in the RSE API File Manager to provide the implementation of the interface. If your test needs to instantiate a UNIX file, dataset, or VSAM data set, write and retrieve content from it, or configure and manipulate it then you can call the z/OS Manager in your test code and the z/OS Manager will call the RSE API File Manager to provide the implementation via the z/OS file function.  

See the [zOS Manager](./zos-manager.md) for details of the z/OS File Annotations.

## Including the Manager in a test

To use the z/OS File RSE API Manager in a test you must import the _@ZosImage_ annotation into the test, as shown in the following example: 

```java
@ZosImage
public IZosImage imagePrimary;
```

You also need to add the Manager dependency into the pom.xml file if you are using Maven, or into the build.gradle file if you are using Gradle. 

If you are using Maven, add the following dependencies into the _pom.xml_ in the _dependencies_ section:

```xml
<dependency>
    <groupId>dev.galasa</groupId>
    <artifactId>dev.galasa.zosfile.rseapi.manager</artifactId>
</dependency>
```

If you are using Gradle, add the following dependencies into `build.gradle` in the _dependencies_ closure:

```groovy
dependencies {
    compileOnly 'dev.galasa:dev.galasa.zosfile.rseapi.manager'
}
```
