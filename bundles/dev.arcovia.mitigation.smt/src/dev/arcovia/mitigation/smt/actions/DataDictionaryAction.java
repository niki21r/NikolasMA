package dev.arcovia.mitigation.smt.actions;

import org.apache.log4j.Logger;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import java.util.Random;

/**
 * Represents an Action that can be executed to modify a Data Dictionary
 * @author Nikolas Rank
 *
 */
public abstract class DataDictionaryAction implements Action{
    protected final Logger logger =
            Logger.getLogger(getClass());
	protected static final datadictionaryFactory factory = datadictionaryFactory.eINSTANCE;
	protected static final Random random = new Random();
		
}
