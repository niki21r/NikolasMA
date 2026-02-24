# SMT Mitigation

This bundle allows the mitigation of confidentiality violations via the addition and removal of labels.

# Installation
Installation and setup steps are described in detail, with exemplary commands for Ubuntu

Prerequisites:
Java 17

C++ 20 or higher

Git

Python

Make

1. Create a new folder and set it as working directory
    `mkdir reproduction && cd reproduction`

2. Installation

2.1 Clone the Z3 Prover repository
    `git clone git@github.com:Z3Prover/z3.git`

2.2 Install the Z3 Prover (github.com/Z3Prover/z3) on your machine according to ther installation instruction.
    We developed this project with Z3 version 4.15.5 - 64 bit 

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
    NikolasMA/tests/dev.arcovia.mitigation.sat.tests
    NikolasMA/tests/dev.arcovia.mitigation.smt.tests

Then click finish

3.7 In the package explorer find the file dev.arcovia.mitigation.smt.tests/src/dev.arcovia.mitigation.smt.tests/src/dev/arcovia/mitigation/smt/tests/RuntimeComparisonTest.java
3.7.1 Right Click it and Select Run As -> 3 JUnit Test

If the test completes successfully, your installation is correct. You should see console output as well as successful test run.


## Z3 Binding

Because the java binding for the Z3 solver is poorly documented we provide some guidance here.  
Z3 Prover provides a few example for encoding problems in java at https://github.com/Z3Prover/z3/tree/master/examples/java  

As we only use a small subset of its capabilities, most of it is not required to understand our implementation.

We explain a few points about the java binding that we found unintuitive or worthy of an explanation while developing.

---

1. **Z3Context ("ctx")**

   Z3Context ("ctx") is an object that encapsulates a problem. All encoding has to be created using this ctx.

   For example:

   ```java
   ctx.mkTrue();
   ```

   or

   ```java
   IntExpr base =
       (IntExpr) ctx.mkITE(ctx.mkXor(cur, ref),
           ctx.mkInt(1),
           ctx.mkInt(0));
   ```

2. **Sorts**

   A sort is a Z3 representation of a Type. For example IntSort is the type for Integers.

3. **Expressions and Typed Interfaces**

   Everything is an expression. Duplicate Interfaces for typed expressions exist. E.g. for Boolean Expressions these are `Expr<BoolSort>` and `BoolExpr`. The generic version exists for user-created Sorts. We do not make use of this feature. This duplicate representation is the reason for a lot of Explicit Type Casts like in the last example. Because we prefered BoolExpr over Expr<BoolSort> during development.

3. **Creating "const" Objects**

   Unintuitively, creating "const" objects does create named variables that the solver can assign values to instead of constants. For example:

   ```java
   ctx.mkBoolConst(node.getEntityName() + "_label_" + label.getEntityName());
   ```

   creates a named boolean variable for a node label.

4. **Named Unmodifiable Variables**

   It is not possible to create named unmodifiable variables (you would assume these would be named constants), therefore we do not do this during implementation. A workaround would be to create a named "const" and assert its equality to a constant value.

   For example:

   ```java
   var const = ctx.mkBoolConst("exampleConstant");
   var eq = ctx.mkEq(const, ctx.mkTrue());
   opt.Assert(eq);
   ```

5. **Creating Solvers**

   We can create solvers using the context. For general SMT/SAT problems you can use:

   ```java
   ctx.mkSolver();
   ```

   However since we model an optimization problem we use the Optimize ("opt") interface instead. It extends the capabilities of normal solvers to optimization problems. It can be created using:

   ```java
   ctx.mkOptimize();
   ```

6. **Asserting Statements**

   Statements that need to be true have to be asserted using the solver. For example:

   ```java
   BoolExpr condition = ctx.mkBoolConst("ourCondition");
   opt.Assert(ctx.mkEq(condition, ctx.mkTrue()));
   ```

   The solver also exposes the `opt.Add()` method. It is identical to the `Assert()` method. Both methods take an array of `BoolExpr`s as input. Passing single `BoolExpr` objects is also possible but leads to a IDE warning. Therefore we often create new arrays when asserting. For example:

   ```java
   opt.Assert(new BoolExpr[] { constraintTranslator.translateConstraint(constr, vertex) });
   ```

7. **Optimization Objectives (Cost Functions)**

   Optimization objectives (cost functions) can be defined using two ways. For an Integer Cost Function these are equivalent.

   7.1 Given an Arithmetic Expression (our cost function is a subclass of this, namely IntExpr), the Optimize object can be called like this:

   ```java
   opt.MkMinimize(costFunction);
   ```

   We use this approach. costFunction is a large IntExpr that encodes the total cost objective for all labels on all nodes and pins.

   7.2 Alternatively you can "soft assert" singular expressions and assign a cost for breaking it. For example:

   ```java
   opt.AddSoft(ctx.mkEq(nodeLabelChanged), "1", null);
   ```

   Soft assertions can be grouped using a string id as the third parameter. If this is null they go into the default group.

8. **Solving**

   So far this covers the creation of expressions and cost objectives, which is everything we use in our scope.

   When everything is encoded, the solver can be instructed to find a solution using:

   ```java
   Status st = opt.Check();
   ```

   This function call is synchronous and returns as soon as the solver found a minimal solution OR proved that no satisfying solution exists. The status is an enum and can be compared to possible outcomes:

   ```java
   st != Status.SATISFIABLE
   ```

   `Status.UNKNOWN` is also a possible result. However this should never ocurr in the scope of our work. It can possibly ocurr when using the Z3 Solver and combining theories that go beyond SAT, such as Z3 Arrays, BitVector numbers, real numbers and quantifiers. Since we do not use these, this should never ocurr.

9. **Querying the Model**

   When the solver has found a solution, i.e.:

   ```java
   st == Status.SATISFIABLE
   ```

   a "Model" object can be queried:

   ```java
   Model m = opt.getModel();
   ```

   This object contains the variables assignments of the solution. We make use of this to create repair operations and query the cost of the solution:

   ```java
   IntExpr costValExpr = (IntExpr) m.eval(costFunction, true);
   ```

   Note that the second parameter (`true`) forces the solver to choose a concrete value for the queried variable. This may be necessary  because the solver may not have reasoned about a variable value because it is irrelevant or both values (for bools) could be valid for the found solution.

   These evaluated expressions can then be turned into java objects by some parsing:

   ```java
   int cost = Integer.parseInt(costValExpr.toString());
   ```

   **Example for extracting Repair Operations for Node Labels depending on the Node Label BoolExpr objects before and after the solving:**

   ```java
   BoolExpr beforeExpr = beforeMap.get(lbl);
   BoolExpr afterExpr = afterMap.get(lbl);

   boolean beforeVal = ((BoolExpr) m.evaluate(beforeExpr, true)).isTrue();
   boolean afterVal = ((BoolExpr) m.evaluate(afterExpr, true)).isTrue();

   if (!beforeVal && afterVal) {
       changes.add(new NodeLabelAddOperation(n, lbl));
   } else if (beforeVal && !afterVal) {
       changes.add(new NodeLabelRemoveOperation(n, lbl));
   }
   ```

10. **Empty Arrays**

   We often create expressions on arrays that may be empty, for example:

   ```java
   BoolExpr thisFlowMatches = ctx.mkOr(anySelectorLabelPresent.toArray(new BoolExpr[0]));
   ```

   The general semantics for these is that OR evaluates to false on an empty array, while AND evaluates to true on an empty array.

11. **Verbosity**

   Verbosity of the solver can be configured by using:

   ```java
   Global.setParameter("verbose", "2");
   ```

   before creating a context. The number can be freely chosen, however we found that 2 offers a good verbosity level for development and insights. If this is configured the solver periodically prints solving stats to stdout during the solving process. These can give a insight into its internal clause storage, solving steps such as simplifications as well as its cost bounds for optimization. However the output of these values is poorly documented.

   This issue provides some partial insight:  
   https://github.com/Z3Prover/z3/issues/1787

11. **Obtaining multiple solutions**

    It is possible to add new assertions to the solver after a model has been obtained. Then a new solution can be obtained. This could in theory be used to find additional solutions, by creating an Assertion that forbids the same assignments for all Node Labels and Pin Assignments and then querying the solver for another solution. 

12. **In-Depth Guide**

   An in-depth guide/tutorial on programming Z3 and its internal workings is available here:  
   https://z3prover.github.io/papers/z3internals.html

13. **Going beyond**

    Chapter 8 of the previous link is about tactics. Tactics allow the user to configure the solving strategy and behavior of Z3. While we played around a bit with configuring these, we did not find a configuration that offered consistently better performance than simply not configuring them. We suspect that the solvers internal decision process on finding the best tactic is pretty mature and hard to beat. 

    Z3 Solver is inherently single-threaded. Its internal process to find a solution is based on probabilistic heuristics. A solution may be found faster for some runs, depending on its random seed. Therefore a speedup in solving time for machines with n cores may be possible. Conceptually you would create n contexts and optimize instances in parallel  and configure them with a different random seed. You could then terminate all threads as soon as one of them found a solution. Parallel solving approaches could also use different tactics.