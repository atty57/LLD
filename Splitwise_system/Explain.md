
# Splitwise (Expense Sharing) – Low-Level Design

## Overview

A system for groups to record shared expenses and settle up. Each expense is
split among participants by a chosen rule (equal, exact amounts, or percentages),
the split feeds a running balance sheet of who-owes-whom, and a debt simplifier
collapses the tangle of IOUs into the fewest payments.

---

## Key Design Patterns

- **Strategy Pattern:** `SplitStrategy` (`EqualSplit`, `ExactSplit`, `PercentSplit`)
  isolates each division rule. New rules (shares/weights) implement the interface.
- **Ledger model:** `BalanceSheet` nets debts pairwise so a reverse debt cancels
  instead of accumulating.
- **Greedy settlement:** `DebtSimplifier` uses max-creditor/max-debtor matching to
  minimise the number of transfers.

---

## Class Relationships

### Association
- `Expense` is split using a `SplitStrategy` and references the payer `User`
- `BalanceSheet` consumes `Expense`s and tracks balances between `User` pairs
- `DebtSimplifier` turns net balances into `Transfer`s

---

## Core Workflows

1. **Record expense:** build an `Expense` with a `SplitStrategy`; it computes each
   participant's share (strategies validate that shares sum to the total).
2. **Update balances:** `BalanceSheet.addExpense` turns each share into a debt to
   the payer, netting against any existing reverse debt.
3. **Settle up:** feed net balances to `DebtSimplifier.simplify` to get the minimal
   set of "A pays B" transfers.

---

## Textual Class Diagram

```
Expense
├── paidBy → User
└── split using → SplitStrategy
                  ├── EqualSplit
                  ├── ExactSplit
                  └── PercentSplit

BalanceSheet
├── consumes → Expense
└── balances: owedTo → (owedBy → amount)

DebtSimplifier → Transfer[] (from, to, amount)
```

---

## Scalability & Extensibility

- **New split types:** implement `SplitStrategy` (by shares, adjustment splits).
- **Groups:** wrap a `BalanceSheet` per group; the same model applies.
- **Settlement:** swap the greedy simplifier for min-cash-flow DP only if the
  transaction count genuinely matters at scale.
- **Money precision:** amounts are rounded to cents; a production system would use
  `BigDecimal` to avoid floating-point drift.

---
