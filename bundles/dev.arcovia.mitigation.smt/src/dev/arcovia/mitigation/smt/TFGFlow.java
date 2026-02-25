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
 * Represents an instance of a flow of a specific Transpose Flow Graph
 *
 */
public class TFGFlow {

	private final Pin srcPin;
	private final DFDVertex srcVertex;
	private final Pin dstPin;
	private final DFDVertex dstVertex; 
	// DFD Flow that this TFGFlow flows along
	private final Flow flow;
	// List of incoming TFG Flows to the same vertex that need to be forwarded, grouped by Assignment
	private final Map<ForwardingAssignment, List<TFGFlow>> thisFlowForwards;
	// List of incoming TFG Flows to the same vertex that each Assignment needs to evaluate on
	private final Map<Assignment, List<TFGFlow>> thisFlowEvaluatesOn;
	
	private static int counter = 0;
	private final int id;
	
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
		return "TFGFlow [flow=" + flow.getEntityName() + " sourceNode "+srcVertex.getName()+" dstNode "+dstVertex.getName()+",id=" + id + "]";
	}


	public Pin getSrcPin() {
		return srcPin;
	}


	public DFDVertex getSrcVertex() {
		return srcVertex;
	}


	public Pin getDstPin() {
		return dstPin;
	}


	public DFDVertex getDstVertex() {
		return dstVertex;
	}


	public Flow getFlow() {
		return flow;
	}


	public Map<ForwardingAssignment, List<TFGFlow>> getThisFlowForwards() {
		return thisFlowForwards;
	}


	public Map<Assignment, List<TFGFlow>> getThisFlowEvaluatesOn() {
		return thisFlowEvaluatesOn;
	}


	public int getId() {
		return id;
	}
}
