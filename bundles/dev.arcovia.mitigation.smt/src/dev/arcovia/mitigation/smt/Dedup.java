package dev.arcovia.mitigation.smt;

public class Dedup {
	/*
	private final Map<String, Integer> pinIdToInt = new HashMap<>();
	private final Map<String, Integer> vtxIdToInt = new HashMap<>();
	private int nextPinInt = 1;
	private int nextVtxInt = 1;

	private int pinInt(Pin p) {
		int id = pinIdToInt.computeIfAbsent(p.getId(), k -> nextPinInt++);
		if ((id & 0xFFFF0000) != 0) throw new IllegalStateException("Pin id overflow (needs >16 bits): " + id);
		return id;
	}

	private int vtxInt(DFDVertex v) {
		String s = v.getReferencedElement().getId();
		int id = vtxIdToInt.computeIfAbsent(s, k -> nextVtxInt++);
		if ((id & 0xFFFF0000) != 0) throw new IllegalStateException("Vertex id overflow (needs >16 bits): " + id);
		return id;
	}

	private long packFlowId(int sv, int sp, int dv, int dp) {
		return ((long) (sv & 0xFFFF) << 48)
				| ((long) (sp & 0xFFFF) << 32)
				| ((long) (dv & 0xFFFF) << 16)
				| (long) (dp & 0xFFFF);
	}

	private final Map<TFGFlow, Long> packedIdCache = new IdentityHashMap<>();
	private long packedIdOf(TFGFlow f) {
		return packedIdCache.computeIfAbsent(f, x -> packFlowId(
				vtxInt(x.srcVertex), pinInt(x.srcPin),
				vtxInt(x.dstVertex), pinInt(x.dstPin)
		));
	}

	private static long[] canonicalPredIds(List<TFGFlow> preds, java.util.function.ToLongFunction<TFGFlow> idFn) {
		int n = preds.size();
		if (n == 0) return new long[0];

		long[] a = new long[n];
		for (int i = 0; i < n; i++) a[i] = idFn.applyAsLong(preds.get(i));

		java.util.Arrays.sort(a);

		int m = 1;
		for (int i = 1; i < n; i++) {
			if (a[i] != a[m - 1]) a[m++] = a[i];
		}
		return (m == n) ? a : java.util.Arrays.copyOf(a, m);
	}

	private static final class FlowKey {
		final Pin outPin;
		final long[][] forwardingDeps;
		final long[][] evalDeps;
		final int hash;

		FlowKey(Pin outPin, long[][] forwardingDeps, long[][] evalDeps) {
			this.outPin = outPin;
			this.forwardingDeps = forwardingDeps;
			this.evalDeps = evalDeps;
			this.hash = computeHash();
		}

		private int computeHash() {
			int h = outPin.hashCode();
			h = 31 * h + deepHash(forwardingDeps);
			h = 31 * h + deepHash(evalDeps);
			return h;
		}

		private static int deepHash(long[][] x) {
			int h = 1;
			for (long[] a : x) h = 31 * h + java.util.Arrays.hashCode(a);
			return h;
		}

		@Override public int hashCode() { return hash; }

		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof FlowKey k)) return false;
			if (!outPin.equals(k.outPin)) return false;
			return deepEquals(forwardingDeps, k.forwardingDeps) && deepEquals(evalDeps, k.evalDeps);
		}

		private static boolean deepEquals(long[][] a, long[][] b) {
			if (a.length != b.length) return false;
			for (int i = 0; i < a.length; i++) {
				if (!java.util.Arrays.equals(a[i], b[i])) return false;
			}
			return true;
		}
	}

	private final Map<FlowKey, BitVecExpr> dedupExprCache = new HashMap<>();
	private final Map<TFGFlow, FlowKey> flowKeyCache = new IdentityHashMap<>();
	private final Set<TFGFlow> visiting = Collections.newSetFromMap(new IdentityHashMap<>());

	private FlowKey buildFlowKey(TFGFlow flow) {
		FlowKey cached = flowKeyCache.get(flow);
		if (cached != null) return cached;

		Pin outPin = flow.srcPin;
		List<AbstractAssignment> assignments = outPinToAss.get(outPin);

		long[][] fwdDeps = new long[assignments.size()][];
		long[][] evlDeps = new long[assignments.size()][];

		for (int i = 0; i < assignments.size(); i++) {
			AbstractAssignment a = assignments.get(i);

			if (a instanceof ForwardingAssignment fa) {
				List<TFGFlow> preds = flow.thisFlowForwards.getOrDefault(fa, List.of());
				fwdDeps[i] = canonicalPredIds(preds, this::packedIdOf);
				evlDeps[i] = new long[0];
			} else if (a instanceof Assignment asg) {
				List<TFGFlow> preds = flow.thisFlowEvaluatesOn.getOrDefault(asg, List.of());
				fwdDeps[i] = new long[0];
				evlDeps[i] = canonicalPredIds(preds, this::packedIdOf);
			} else {
				fwdDeps[i] = new long[0];
				evlDeps[i] = new long[0];
			}
		}

		FlowKey key = new FlowKey(outPin, fwdDeps, evlDeps);
		flowKeyCache.put(flow, key);
		return key;
	}

	private void createDataFlowConstraintDedup(TFGFlow flow) {
		if (flowLabels.get(flow) != null) return;
		if (!visiting.add(flow)) return;

		flow.thisFlowForwards.values().forEach(list -> list.forEach(this::createDataFlowConstraintDedup));
		flow.thisFlowEvaluatesOn.values().forEach(list -> list.forEach(this::createDataFlowConstraintDedup));

		FlowKey key = buildFlowKey(flow);
		BitVecExpr cached = dedupExprCache.get(key);
		if (cached != null) {
			System.out.println("DEDUP Found");
			flowLabels.put(flow, cached);
			visiting.remove(flow);
			return;
		}

		Pin pin = flow.srcPin;
		List<AbstractAssignment> assignments = outPinToAss.get(pin);
		Map<Label, Integer> dataLabelMap = mappings.dataLabelToInt;

		BitVecExpr currLabels = ctx.mkBV(0, dataLabelMap.size());

		for (int i = 0; i < assignments.size(); i++) {
			AbstractAssignment assignment = assignments.get(i);

			if (assignment instanceof SetAssignment cast) {
				BitVecNum set = SMTUtil.makeBVMask(ctx, dataLabelMap, cast.getOutputLabels(), dataLabelMap.size());
				currLabels = ctx.mkBVOR(currLabels, set);
			} else if (assignment instanceof UnsetAssignment cast) {
				BitVecNum labels = SMTUtil.makeBVMask(ctx, dataLabelMap, cast.getOutputLabels(), dataLabelMap.size());
				currLabels = ctx.mkBVAND(currLabels, ctx.mkBVNot(labels));
			} else if (assignment instanceof ForwardingAssignment cast) {
				List<TFGFlow> forward = flow.thisFlowForwards.getOrDefault(cast, List.of());
				for (TFGFlow pre : forward) {
					BitVecExpr prevLabels = flowLabels.get(pre);
					if (prevLabels != null) currLabels = ctx.mkBVOR(currLabels, prevLabels);
				}
			} else if (assignment instanceof Assignment cast) {
				List<TFGFlow> evaluateOn = flow.thisFlowEvaluatesOn.getOrDefault(cast, List.of());
				BoolExpr term = createTerm(cast.getTerm(), evaluateOn);
				BitVecNum labels = SMTUtil.makeBVMask(ctx, dataLabelMap, cast.getOutputLabels(), dataLabelMap.size());
				BitVecExpr notLabels = ctx.mkBVNot(labels);
				currLabels = (BitVecExpr) ctx.mkITE(term,
						ctx.mkBVOR(currLabels, labels),
						ctx.mkBVAND(currLabels, notLabels));
			}
		}

		BitVecExpr pinNewSetLabels = pinSet.get(pin);
		currLabels = ctx.mkBVOR(currLabels, pinNewSetLabels);

		BitVecExpr pinNewUnset = pinUnset.get(pin);
		currLabels = ctx.mkBVAND(currLabels, ctx.mkBVNot(pinNewUnset));

		dedupExprCache.put(key, currLabels);
		flowLabels.put(flow, currLabels);

		visiting.remove(flow);
	}

*/
}
