package dev.arcovia.mitigation.smt.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.microsoft.z3.BoolExpr;
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
}
