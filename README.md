# ZIO introduction

TODO

### Who Should Attend

TODO

### Prerequisites

TODO

### Building

You will need to have Java 8 or 11 installed. Follow the "Install Scala" instructions on [Getting Started](https://docs.scala-lang.org/getting-started/index.html) page of Scala documentation.

The project contains an [SBT](https://www.scala-sbt.org/) build file. SBT is the most common Scala
build tool. You can download the build tool [here](https://www.scala-sbt.org/).

SBT build files can be imported into [IntelliJ IDEA](https://www.jetbrains.com/idea/), an IDE that
has a [plugin](https://plugins.jetbrains.com/plugin/1347-scala) for developing Scala applications.

Alternately, some people use [Visual Studio Code](https://code.visualstudio.com/) to develop Scala
applications. If you choose to use Visual Studio Code, then make sure you install the 
[Metals](https://marketplace.visualstudio.com/items?itemName=scalameta.metals) plugin and the 
[Scala Syntax](https://marketplace.visualstudio.com/items?itemName=scala-lang.scala) plugin.

Even if not using IntelliJ IDEA or Visual Studio Code with Metals, you can build the project from
any terminal by using SBT directly.

After opening a terminal and changing the directory to wherever you downloaded the files, simply
enter the following command:

```
sbt
```

This will start SBT in interactive mode, where you can type commands into the console in order to 
compile, run, and test your SBT project.

To compile the code in the project, type the following command at the SBT prompt:

```
compile
```

If there are any compiler errors, they will be reported to you, and you can edit the source code to
fix the compiler errors and try again.

Alternately, you can put SBT into continuous compilation mode. In this mode, SBT will attempt to 
compile your project whenever any files change state.

To place SBT into this mode, type the following command at the SBT prompt:

```
~compile
```


# Legal

Copyright&copy; 2021 Scalac sp. z o.o. All rights reserved.
