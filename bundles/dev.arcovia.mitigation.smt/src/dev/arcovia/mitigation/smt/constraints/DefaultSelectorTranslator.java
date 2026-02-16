package dev.arcovia.mitigation.smt.constraints;

import java.util.HashMap;
import java.util.Map;

import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.dsl.selectors.AbstractSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.DataCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VariableNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsListSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexCharacteristicsSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexNameSelector;
import org.dataflowanalysis.analysis.dsl.selectors.VertexTypeSelector;

import com.microsoft.z3.BoolExpr;

import dev.arcovia.mitigation.smt.SMT;

public final class DefaultSelectorTranslator implements SelectorTranslator {
	private final Map<Class<?>, AbstractSelectorHandler<?>> handlers = new HashMap<>();
	private final SMT smt;

	public DefaultSelectorTranslator(SMT smt) {
		this.smt = smt;

		register(DataCharacteristicsSelector.class, new DataCharacteristicsHandler());
		register(DataCharacteristicListSelector.class, new DataCharacteristicListHandler());
		register(VariableNameSelector.class, new DataNameHandler());
		register(VertexCharacteristicsListSelector.class, new VertexCharacteristicListHandler());
		register(VertexTypeSelector.class, new VertexTypeHandler());
		register(VertexNameSelector.class, new VertexNameHandler());
		register(VertexCharacteristicsSelector.class, new VertexCharacteristicsHandler());
	}

	private <T extends AbstractSelector> void register(Class<T> cls, AbstractSelectorHandler<T> h) {
		handlers.put(cls, h);
	}

	@Override
	public BoolExpr toBool(AbstractSelector selector, DFDVertex vertex, SelectorRole role) {
		var handler = findHandler(selector.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("No selector handler registered for " + selector.getClass().getName());
		}
		@SuppressWarnings("unchecked")
		AbstractSelectorHandler<AbstractSelector> h = (AbstractSelectorHandler<AbstractSelector>) handler;
		return h.encode(selector, vertex, role, smt);
	}

	private AbstractSelectorHandler<?> findHandler(Class<?> cls) {
		var h = handlers.get(cls);
		if (h != null)
			return h;

		for (var e : handlers.entrySet()) {
			if (e.getKey().isAssignableFrom(cls))
				return e.getValue();
		}
		return null;
	}
}
