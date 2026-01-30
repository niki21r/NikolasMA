package dev.arcovia.mitigation.smt.actions;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.*;


/**
 * Adds a label type to a Datadictionary
 * @author niki
 *
 */
public class LabelTypeAction extends DataDictionaryAction {
	
	private String name;
	private String id;
	
	public LabelTypeAction(String name) {
		this(name, String.valueOf(random.nextInt()));
	}
	
	public LabelTypeAction(String name, String id) {
		this.name = name;
		this.id = id;
	}
	
	
	@Override
	public DataFlowDiagramAndDictionary doAction(DataFlowDiagramAndDictionary dfd) {
		LabelType newType = factory.createLabelType();
		newType.setEntityName(name);
		newType.setId(id);
		dfd.dataDictionary().getLabelTypes().add(newType);
		logger.debug("Added Label Type "+name);
		return dfd;
	}

	@Override
	public DataFlowDiagramAndDictionary undoAction(DataFlowDiagramAndDictionary dfd) {
		dfd.dataDictionary().getLabelTypes().removeIf(x -> x.getEntityName().equals(name));
		logger.debug("Removed label type "+name);
		return dfd;
	}
}	