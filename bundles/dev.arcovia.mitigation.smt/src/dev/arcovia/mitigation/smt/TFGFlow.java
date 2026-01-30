package dev.arcovia.mitigation.smt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.ForwardingAssignment;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;


/**
 * @author Nikolas Rank 
 * Represents an instance of a data flow of a specific Transpose Flow Graph
 *
 */
public class TFGFlow {

	// TFG this flow resides in
	public Pin srcPin;
	public DFDVertex srcVertex;
	public Pin dstPin;
	public DFDVertex dstVertex; 
	// DFD Flow that this TFGFlow flows along
	public Flow flow;
	// List of incoming TFG Flows to the same vertex that need to be forwarded, grouped by Assignment
	public Map<ForwardingAssignment, List<TFGFlow>> thisFlowForwards;
	// List of incoming TFG Flows to the same vertex that each Assignment need sto evaluate on
	public Map<Assignment, List<TFGFlow>> thisFlowEvaluatesOn;
	
	private static int counter = 0;
	public int id;
	
	public TFGFlow(Pin srcP, DFDVertex srcVertex, Pin dstP, DFDVertex dstVertex, Flow flow) {
		this.srcPin = srcP;
		this.srcVertex = srcVertex;
		this.dstPin = dstP;
		this.dstVertex = dstVertex;
		this.flow = flow;
		this.thisFlowForwards = new HashMap<>();
		this.thisFlowEvaluatesOn = new HashMap<>();
		this.id = counter++;
	}


	@Override
	public int hashCode() {
		return Objects.hash(id);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TFGFlow other = (TFGFlow) obj;
		return id == other.id;
	}


	@Override
	public String toString() {
		return "TFGFlow [flow=" + flow.getEntityName() + " sourceNode "+srcVertex.getName()+" dstNode "+dstVertex.getName()+", thisFlowForwards=" + " ,id=" + id + "]";
	}
}
