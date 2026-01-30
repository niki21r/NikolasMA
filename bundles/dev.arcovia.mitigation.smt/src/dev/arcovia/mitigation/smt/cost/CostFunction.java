package dev.arcovia.mitigation.smt.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;

public final class CostFunction {

    private final Context ctx;
    private final List<IntExpr> terms = new ArrayList<>();

    private CostFunction(Context ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
    }

    public static CostFunction create(Context ctx) {
        return new CostFunction(ctx);
    }

    public CostFunction add(BoolExpr cur, BoolExpr ref, int weight) {
        if (weight == 0) {
            return this;
        }

        IntExpr base =
                (IntExpr) ctx.mkITE(ctx.mkXor(cur, ref),
                                    ctx.mkInt(1),
                                    ctx.mkInt(0));

        IntExpr weighted =
                (weight == 1)
                        ? base
                        : (IntExpr) ctx.mkMul(ctx.mkInt(weight), base);

        terms.add(weighted);
        return this;
    }
    
    public CostFunction addTerm(IntExpr term) {
    	terms.add(term);
    	return this;
    }

    public IntExpr build() {
        if (terms.isEmpty()) {
            return ctx.mkInt(0);
        }
        if (terms.size() == 1) {
            return terms.get(0);
        }
        return (IntExpr) ctx.mkAdd(terms.toArray(IntExpr[]::new));
    }
}
