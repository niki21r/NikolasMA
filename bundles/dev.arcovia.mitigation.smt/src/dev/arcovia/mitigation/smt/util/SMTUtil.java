package dev.arcovia.mitigation.smt.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

/**
 * @author Nikolas Rank Contains relevant utility functions that are needed to
 *         convert from DFD representation to z3 representation or the other way
 *         around
 */
public class SMTUtil {

	/**
	 * Counts AST nodes in an array of BoolExpr (DAG size). Shared sub-terms across
	 * expressions are counted only once.
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
}
