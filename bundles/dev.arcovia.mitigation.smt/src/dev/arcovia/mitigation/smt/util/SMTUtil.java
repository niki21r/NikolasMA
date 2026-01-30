package dev.arcovia.mitigation.smt.util;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.dataflowanalysis.dfd.datadictionary.Label;

import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;

/**
 * @author Nikolas Rank Contains relevant utility functions that are needed to
 *         convert from DFD representation to z3 representation or the other way
 *         around
 */
public class SMTUtil {

	/**
	 * Counts AST nodes in an array of BoolExpr (DAG size).
	 * Shared sub-terms across expressions are counted only once.
	 */
	public static long countAstNodes(BoolExpr[] exprs) {
	    long total = 0;

	    for (BoolExpr expr : exprs) {
	        Objects.requireNonNull(expr, "expr");
	        total += countTree(expr);
	    }
	    return total;
	}

	private static long countTree(Expr<?> expr) {
	    Deque<Expr<?>> stack = new ArrayDeque<>();
	    stack.push(expr);

	    long count = 0;
	    while (!stack.isEmpty()) {
	        Expr<?> cur = stack.pop();
	        count++;

	        int n = cur.getNumArgs();
	        for (int i = 0; i < n; i++) {
	            stack.push(cur.getArgs()[i]);
	        }
	    }
	    return count;
	}
	
    public static long countUniqueAstNodes(BoolExpr[] exprs) {
        Objects.requireNonNull(exprs, "exprs");

        // Expr.getId() is an int; store as Integer or use a primitive set if you have one.
        Set<Integer> visited = new HashSet<>();
        long total = 0;

        for (BoolExpr expr : exprs) {
            Objects.requireNonNull(expr, "expr");
            total += countUniqueDag(expr, visited);
        }
        return total;
    }

    /**
     * Counts unique nodes reachable from expr, updating visited.
     * Returns how many *new* nodes were discovered from this root.
     */
    private static long countUniqueDag(Expr<?> expr, Set<Integer> visited) {
        Deque<Expr<?>> stack = new ArrayDeque<>();
        stack.push(expr);

        long count = 0;
        while (!stack.isEmpty()) {
            Expr<?> cur = stack.pop();
            int id = cur.getId();

            // Already seen this AST node somewhere (maybe under another parent/root)
            if (!visited.add(id)) {
                continue;
            }

            count++;

            // Avoid allocating the args array repeatedly by calling once.
            Expr<?>[] args = cur.getArgs();
            for (Expr<?> arg : args) {
                stack.push(arg);
            }
        }
        return count;
    }
	

	
	/**
	 * Given a BitVector, returns the List of Labels it encodes
	 * 
	 * @param mask          BigInteger, encoding a bitvector
	 * @param labelToIntMap maps labels to their respective bit position
	 * @return Labels contained in this BitVector
	 */
	public static List<Label> labelsFromMask(BigInteger mask, Map<Label, Integer> labelToIntMap) {

		List<Label> result = new ArrayList<>();

		for (Map.Entry<Label, Integer> entry : labelToIntMap.entrySet()) {
			int bitIndex = entry.getValue();

			if (mask.testBit(bitIndex)) {
				result.add(entry.getKey());
			}
		}

		return result;
	}

	/**
	 * Creates a Z3 Bitvector, given a list of labels
	 * 
	 * @param ctx      Z3 context that the BV will be created in
	 * @param indexMap Maps Labels to their indices in the bitvector
	 * @param labels   List of labels to encode
	 * @param size     Size of the bitvector
	 * @return Z3 BitVecNum that encodes the input labels
	 */
	public static BitVecNum makeBVMask(Context ctx, Map<Label, Integer> indexMap, List<Label> labels, int size) {
		int mask = 0;
		boolean[] hasIndex = new boolean[size];

		for (Label label : labels) {
			if (indexMap.containsKey(label)) {
				int idx = indexMap.get(label);
				if (idx >= 0 && idx < size) {
					hasIndex[idx] = true;
				}
			}
		}

		for (int bit = 0; bit < size; bit++) {
			if (hasIndex[bit]) {
				mask |= (1 << bit);
			}
		}

		return ctx.mkBV(mask, size);
	}
}
