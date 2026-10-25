// Balance tracking + debt simplification.

import java.util.*;

// The ledger: a net balance between every pair of users.
// balances[a][b] > 0 means b owes a that amount.
public class BalanceSheet {
    // owedTo -> (owedBy -> amount)
    private final Map<String, Map<String, Double>> balances = new HashMap<>();

    // Apply one expense: everyone's share becomes a debt to the payer.
    public void addExpense(Expense expense) {
        String payer = expense.getPaidByUserId();
        for (Map.Entry<String, Double> share : expense.getShares().entrySet()) {
            String participant = share.getKey();
            if (participant.equals(payer)) continue;  // payer doesn't owe themselves
            adjust(payer, participant, share.getValue());
        }
    }

    // participant owes payer `amount` more. Net against any reverse debt.
    private void adjust(String payer, String participant, double amount) {
        double reverse = get(participant, payer);
        if (reverse >= amount) {
            set(participant, payer, reverse - amount);           // shrink reverse debt
        } else {
            set(participant, payer, 0);
            set(payer, participant, get(payer, participant) + (amount - reverse));
        }
    }

    public double get(String owedTo, String owedBy) {
        return balances.getOrDefault(owedTo, Map.of()).getOrDefault(owedBy, 0.0);
    }

    private void set(String owedTo, String owedBy, double amount) {
        balances.computeIfAbsent(owedTo, k -> new HashMap<>()).put(owedBy, amount);
    }

    // Everything a given user owes to others.
    public Map<String, Double> debtsOf(String userId) {
        Map<String, Double> result = new HashMap<>();
        for (String creditor : balances.keySet()) {
            double amt = get(creditor, userId);
            if (amt > 0.01) result.put(creditor, amt);
        }
        return result;
    }
}

// Reduces a group's tangled IOUs to the fewest transactions that settle up.
// Greedy max-debtor/max-creditor matching: not provably minimal, but near it
// and the standard interview answer.
public class DebtSimplifier {
    public static class Transfer {
        public final String from, to;
        public final double amount;
        Transfer(String from, String to, double amount) {
            this.from = from; this.to = to; this.amount = amount;
        }
        public String toString() { return from + " pays " + to + " " + amount; }
    }

    // ponytail: greedy settle; swap for min-cash-flow DP only if someone proves
    // the extra transaction count actually matters.
    public List<Transfer> simplify(Map<String, Double> netBalances) {
        // netBalances: userId -> net amount (positive = is owed, negative = owes)
        PriorityQueue<Map.Entry<String, Double>> creditors =
            new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));
        PriorityQueue<Map.Entry<String, Double>> debtors =
            new PriorityQueue<>(Comparator.comparingDouble(Map.Entry::getValue));

        for (var e : netBalances.entrySet()) {
            if (e.getValue() > 0.01) creditors.add(Map.entry(e.getKey(), e.getValue()));
            else if (e.getValue() < -0.01) debtors.add(Map.entry(e.getKey(), e.getValue()));
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            var credit = creditors.poll();
            var debt = debtors.poll();
            double settle = Math.min(credit.getValue(), -debt.getValue());
            transfers.add(new Transfer(debt.getKey(), credit.getKey(), round(settle)));

            double creditLeft = credit.getValue() - settle;
            double debtLeft = debt.getValue() + settle;
            if (creditLeft > 0.01) creditors.add(Map.entry(credit.getKey(), creditLeft));
            if (debtLeft < -0.01) debtors.add(Map.entry(debt.getKey(), debtLeft));
        }
        return transfers;
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
