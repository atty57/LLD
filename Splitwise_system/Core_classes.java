// Core domain for an expense-sharing app (Splitwise-style).
// Design reference, not a single compilable unit (several public classes per file).

import java.util.*;

public class User {
    private final String userId;
    private final String name;
    private final String email;

    public User(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
}

// How a single expense is divided. Each strategy returns the amount owed per
// user. Keeping this as an interface is the core of the design: EQUAL, EXACT
// and PERCENT splits share no code but the same contract.
public interface SplitStrategy {
    // Returns userId -> amount owed. Must sum to totalAmount.
    Map<String, Double> computeShares(double totalAmount, List<String> participantIds, double[] values);
}

public class EqualSplit implements SplitStrategy {
    @Override
    public Map<String, Double> computeShares(double total, List<String> ids, double[] ignored) {
        Map<String, Double> shares = new HashMap<>();
        double each = round(total / ids.size());
        double running = 0;
        for (int i = 0; i < ids.size(); i++) {
            // Last participant absorbs the rounding remainder so shares sum exactly.
            double share = (i == ids.size() - 1) ? round(total - running) : each;
            shares.put(ids.get(i), share);
            running += share;
        }
        return shares;
    }

    static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}

public class ExactSplit implements SplitStrategy {
    @Override
    public Map<String, Double> computeShares(double total, List<String> ids, double[] amounts) {
        validate(total, ids, amounts);
        Map<String, Double> shares = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) shares.put(ids.get(i), amounts[i]);
        return shares;
    }

    private void validate(double total, List<String> ids, double[] amounts) {
        if (amounts == null || amounts.length != ids.size()) {
            throw new IllegalArgumentException("Need one exact amount per participant");
        }
        double sum = 0;
        for (double a : amounts) sum += a;
        if (Math.abs(sum - total) > 0.01) {
            throw new IllegalArgumentException("Exact amounts must sum to total");
        }
    }
}

public class PercentSplit implements SplitStrategy {
    @Override
    public Map<String, Double> computeShares(double total, List<String> ids, double[] percents) {
        if (percents == null || percents.length != ids.size()) {
            throw new IllegalArgumentException("Need one percent per participant");
        }
        double sum = 0;
        for (double p : percents) sum += p;
        if (Math.abs(sum - 100.0) > 0.01) {
            throw new IllegalArgumentException("Percents must sum to 100");
        }
        Map<String, Double> shares = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            shares.put(ids.get(i), EqualSplit.round(total * percents[i] / 100.0));
        }
        return shares;
    }
}

// A recorded expense: who paid, how much, and how it was split.
public class Expense {
    private final String expenseId;
    private final String description;
    private final double amount;
    private final String paidByUserId;
    private final Map<String, Double> shares;  // userId -> amount owed

    public Expense(String description, double amount, String paidByUserId,
                   List<String> participantIds, SplitStrategy strategy, double[] values) {
        this.expenseId = UUID.randomUUID().toString();
        this.description = description;
        this.amount = amount;
        this.paidByUserId = paidByUserId;
        this.shares = strategy.computeShares(amount, participantIds, values);
    }

    public String getPaidByUserId() { return paidByUserId; }
    public Map<String, Double> getShares() { return shares; }
    public double getAmount() { return amount; }
}
