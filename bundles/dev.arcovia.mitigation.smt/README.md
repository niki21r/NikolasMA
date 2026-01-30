# SMT Mitigation

This bundle allows the mitigation of confidentiality violations via the addition and removal of labels.  
It relies on the Z3 SMT Solver.

---

# Installation

Installation and setup steps are described in detail below, with exemplary commands for Ubuntu.

## Prerequisites

- Java 17  
- C++ 20 or higher  
- Git  
- Python  
- Make  

---

## 1. Create a Working Directory

```bash
mkdir reproduction && cd reproduction
```

---

## 2. Installation

### 2.1 Clone the Z3 Prover Repository

Clone the Z3 Prover repository or use the one provided in this package.  
We developed our implementation with version **4.15.5**.

```bash
git clone git@github.com:Z3Prover/z3.git
cd z3
git checkout z3-4.15.5
```

---

### 2.2 Install the Z3 Prover

Ensure that you create the Java bindings.  
We developed this project with Z3 version **4.15.5 – 64 bit**.

```bash
python scripts/mk_make.py --java
cd build
make
sudo make install
```

---

### 2.3 Return to the Base Folder

```bash
cd ../../
```

---

### 2.4 Clone Our Implementation Repository

```bash
git clone git@github.com:niki21r/NikolasMA.git
cd NikolasMA/
git checkout 4319cb0
```

---

### 2.5 Clone the Data Flow Analysis Repository

Clone the repository or use the one provided in this package.

```bash
git clone git@github.com:DataFlowAnalysis/DataFlowAnalysis.git
cd DataFlowAnalysis/
git checkout 5eaa027
```

---

### 2.6 Download the Eclipse IDE

Download the appropriate Eclipse IDE for your system from:

https://updatesite.palladio-simulator.com/DataFlowAnalysis/product/nightly/

Unpack it into a folder (e.g., `IDE`).  
You can also use the one provided in this package.

Your `reproduction` folder should now contain:

```
z3
NikolasMA
DataFlowAnalysis
IDE
```

---

# Setup

## 3. Start the IDE

Start the installed IDE via CLI or double-click in a file explorer:

```bash
./IDE/DataFlowAnalysisBench
```

---

### 3.1 Create Workspace

Click **Launch** when prompted to create a workspace.

---

### 3.2 Optional: Change Theme

To switch to Dark Theme:

```
Window → Preferences → General → Appearance → Theme
```

You may need to restart the IDE.

---

### 3.3 Ensure Java 17 is Referenced

```
Window → Preferences → Java → Installed JREs
```

---

### 3.4 Switch to Java Perspective

```
Window → Perspective → Open Perspective → Other... → Java
```

---

### 3.5 Import Required Dataflow Analysis Bundles

```
File → Open Projects From File System → Directory...
Select DataFlowAnalysis → Open → Deselect all → Manually select:
```

- `DataFlowAnalysis/bundles/org.dataflowanalysis.analysis`
- `DataFlowAnalysis/bundles/org.dataflowanalysis.analysis.dfd`
- `DataFlowAnalysis/bundles/org.dataflowanalysis.analysis.pcm`
- `DataFlowAnalysis/bundles/org.dataflowanalysis.converter`
- `DataFlowAnalysis/bundles/org.dataflowanalysis.dfd.datadictionary`
- `DataFlowAnalysis/bundles/org.dataflowanalysis.dfd.dataflowdiagram`
- `DataFlowAnalysis/bundles/org.dataflowanalysis.examplemodels`

Click **Finish**.

---

### 3.6 Generate Model Code

#### 3.6.1 Dataflowdiagram

In the package explorer:

```
org.dataflowanalysis.dfd.dataflowdiagram → model → dataflowdaigram.genmodel
```

Right-click the root object **Dataflowdiagram** → **Generate Model Code**

#### 3.6.2 DataDictionary

In the package explorer:

```
org.dataflowanalysis.dfd.datadictionary → model → datadictionary.genmodel
```

Right-click the root object **DataDictionary** → **Generate Model Code**

---

### 3.7 Import Required Mitigation Bundles

```
File → Open Projects from File System → Directory...
Select NikolasMA → Open → Deselect all → Manually select:
```

- `NikolasMA/bundles/dev.arcovia.mitigation.sat`
- `NikolasMA/bundles/dev/arcovia.mitigation.smt`
- `NikolasMA/tests/dev.arcovia.mitigation.sat.tests`
- `NikolasMA/tests/dev.arcovia.mitigation.smt.tests`

Click **Finish**.

---

### 3.8 Run Test

Locate:

```
dev.arcovia.mitigation.smt.tests/src/dev/arcovia/mitigation/smt/tests/evaluation/RuntimeComparisonTest.java
```

Right-click → **Run As → JUnit Test**

If the test completes successfully, the installation is correct.  
You should see console output and a successful test run.

---

# Inspecting and Generating Evaluation Results

The package contains our evaluation results.  
You can regenerate them by running the respective tests located in:

```
NikolasMA/tests/dev.arcovia.mitigation.smt.tests/src/dev/arcovia/mitigation/smt/tests/evaluation
```

Evaluation data is written to:

```
NikolasMA/tests/dev.arcovia.mitigation.smt.tests/testresults/results
```

Python scripts are provided to reproduce the plots.  
They rely on matplotlib.

Example virtual environment setup:

```bash
cd NikolasMA/tests/dev.arcovia.mitigation.smt.tests/testresults/results/
python3 -m venv venv
source venv/bin/activate
pip install matplotlib
```

---

# Script Overview

## 1. Violations

Shows that the approach removes all violations from the evaluation dataset.

```bash
cd violationResults/
python make_violations_png.py
```

- Requires: `data.json`
- Outputs: `violations.png`

---

## 2. Runtime Results

### 2.1 Runtime to Expression Tree Size

```bash
cd runtimeResults/100runs/
python plot.py
```

- Requires: `data.json`
- Outputs: `dag_vs_runtime.png`

---

### 2.2 Runtime Effect of Complexity Reduction

```bash
cd runtimeResults/complexityReduction/
python plots.py
```

- Requires: `data.json`
- Outputs: `plots.png`

---

### 2.3 Runtime Comparison

Runtime comparison between our approach and Niehues et al.

```bash
cd runtimeResults/comparison
python plots.py
```

- Requires: `data.json`
- Outputs three plots in `runtimeResults/comparison/plots`
- `sat_vs_smt.png` is used for the thesis

---

## 3. Memory Results

```bash
cd memoryResults/freshJvm100runs/
python plot.py
```

- Requires: `data.json`
- Outputs: `dag_vs_memory.png`

---

## 4. Modification Results

### 4.1 Our Approach in Isolation

```bash
cd modificationResults/
python make_modifications_png.py
```

- Requires: `data.json`
- Outputs: `modifications.png`

---

### 4.2 Comparison to Niehues (Additions Only)

```bash
cd modificationResults/comparison/add
python plot.py
```

- Requires: `data.json`
- Outputs: `plot.png`

Additional statistics:

```bash
python wilcoxon.py
```

---

### 4.3 Comparison to Niehues (All Modifications Allowed)

```bash
cd modificationResults/comparison/all
python plot.py
```

- Requires: `data.json`
- Outputs: `plot.png`

---

## 5. Scalability Results

Located in:

```bash
cd scalabilityResults/
```

One plotting script is used for all plots.

Scalability dimensions:

- `labels`
- `labelsAfterNeverFlows`
- `labelsBeforeNeverFlows`
- `labelTypes`
- `numConstraints`
- `tfgAmount`
- `tfgLength`

Each dimension contains configuration folders (e.g., `10Runs60Minutes`).

Each configuration folder contains:

- `smtData.json`
- `satData.json`

The plotting script expects:

- A folder containing `smtData.json` and `satData.json`
- A string describing the x-axis

Example:

```bash
python plot.py labels/10Runs60Minutes/ "Amount of newly introduced dummy labels"
```

The resulting plot is stored as `plot.png` in the respective folder.