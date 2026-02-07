# SMT Mitigation

This bundle allows the mitigation of confidentiality violations via the addition and removal of labels.

# Installation
Installation and setup steps are described in detail, with exemplary commands for Ubuntu

Prerequisites:
Java 17

Git

Python

Make

1. Create a new folder and set it as working directory
    `mkdir reproduction && cd reproduction`

2. Installation

2.1 Clone the Z3 Prover repository
    `git clone git@github.com:Z3Prover/z3.git`

2.2 Compile the Z3 Prover with java Bindings according to the instructions provided on the github page (https://github.com/Z3Prover/z3). These operations may take several minutes.
``` 
    cd z3
    python scripts/mk_make.py --java
    cd build
    make
    sudo make install
```

2.3 Return to the base folder
    `cd ..`

2.4 Clone our implementation repositories
    `git clone git@github.com:niki21r/NikolasMA.git`
    `git clone git@github.com:niki21r/DFANikolasMA.git`

2.5. Download the appropriate Eclipse IDE for your system from (https://updatesite.palladio-simulator.com/DataFlowAnalysis/product/nightly/) and unpack it into a folder, which you may call "IDE".

    Now your reproduction folder should contain four folders:
    z3
    NikolasMA
    DFANikolasMA
    IDE
        
2.6 Step 2.2 created a jar file that contains the Java Bindings for the Z3 Prover. It has to be located, and copied to `reproduction/NikolasMA/bundles/dev.arcovia.mitigation.smt/lib/com.microsoft.z3.jar`

It is usually located in /reproduction/z3/build/
    From the base folder 

    mv z3/build/com.microsoft.z3.jar NikolasMA/bundles/dev.arcovia.mitigation.smt/lib/
   
# Setup
3. Start the installed IDE from the CLI or via Double-Click in a File Explorer
    `./IDE/DataFlowAnalysisBench`

3.1 Click 'Launch'

3.2 You may change the Theme to Dark Theme by going to Windows->Preferences->General->Appearance->Theme

3.3 Ensure that your JAVA 17 JRE is properly referenced at Windows -> Preferences -> Java -> Installed JREs

3.4 Switch to the Java Perspective
Window -> Perspective -> Open Perspective -> Other... -> Java

3.4 Import the required Dataflow Analysis Bundles
File -> Open Projects From File System -> Directory...  -> Select DFANikolasMA -> Open -> Deselect all -> Manually select 
    DFANikolasMA/bundles/org.dataflowanalysis.analysis
    DFANikolasMA/bundles/org.dataflowanalysis.analysis.dfd
    DFANikolasMA/bundles/org.dataflowanalysis.analysis.pcm
    DFANikolasMA/bundles/org.dataflowanalysis.converter
    DFANikolasMA/bundles/org.dataflowanalysis.dfd.datadictionary
    DFANikolasMA/bundles/org.dataflowanalysis.dfd.dataflowdiagram
    DFANikolasMA/bundles/org.dataflowanalysis.examplemodels

Then click finish.

3.5 Generate Model code for Dataflowdiagrams and Datadictionarys by doing

3.5.1 In the package explorer on the left go to org.dataflowanalysis.dfd.datadictionary -> model -> datadictionary.genmodel

3.5.2 In the editor for that file right click DataDictionary -> Generate Model Code

3.5.3 In the package explorer on the left go to 
org.dataflowanalysis.dfd.dataflowdiagram -> model -> dataflowdaigram.genmodel

3.5.4 In the editor for that file right click Dataflowdiagram -> Generate Model Code

3.6 Import the required Mitigation Bundles

File -> Open Projects from File System -> Directory... -> 
Select NikolasMA -> Open -> Deselect all -> Manually select

    NikolasMA/bundles/dev.arcovia.mitigation.sat
    NikolasMA/bundles/dev/arcovia.mitigation.smt
    NikolasMA/tests/dev.arcovia.mitigation.sat.stest
    NikolasMA/tests/dev.arcovia.mitigation.smt.tests

Then click finish

3.7 In the package explorer find the file dev.arcovia.mitigation.smt.tests/src/dev.arcovia.mitigation.smt.tests/ViolationsTest.java
3.7.1 Right Click it and Select Run As -> 3 JUnit Test

If the test runs, your installation is correct. You should see console output as well as successful test run.
