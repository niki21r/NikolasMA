package dev.arcovia.mitigation.smt.actions;

import java.util.Optional;

import org.dataflowanalysis.converter.dfd2web.DataFlowDiagramAndDictionary;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Label;

/**
 * @author Nikolas Rank
 * Adds a Label to a given LabelType if it exists.
 */
public class LabelAction extends DataDictionaryAction{

	String type;
	String name;
	String id;

	public LabelAction(String type, String name, String id) {
		this.type = type;
		this.name = name;
		this.id = id;
	}
	
	public LabelAction(String type, String name) {
		this(type,name,String.valueOf(random.nextInt()));
	}
	
	@Override
	public DataFlowDiagramAndDictionary doAction(DataFlowDiagramAndDictionary dfd) {
		Optional<LabelType> opt = dfd.dataDictionary().getLabelTypes().stream().filter(x -> x.getEntityName().equals(type)).findFirst();
		if (opt.isEmpty()) {
			logger.debug("Couldn't find label type "+type);
		} else {
			Label label = factory.createLabel();
			label.setEntityName(name);
			label.setId(id);
			opt.get().getLabel().add(label);
		}
		return dfd;
	}

	@Override
	public DataFlowDiagramAndDictionary undoAction(DataFlowDiagramAndDictionary dfd) {
		Optional<LabelType> opt = dfd.dataDictionary().getLabelTypes().stream().filter(x -> x.getEntityName().equals(type)).findFirst();
		if (opt.isEmpty()) {
			logger.debug("Couldn't find label type "+type);
		} else {
			opt.get().getLabel().removeIf(x -> x.getEntityName().equals(name));
		}
		return dfd;
	}

}
