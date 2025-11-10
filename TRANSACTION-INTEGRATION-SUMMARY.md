# Transaction Integration with Blockchain

## Overview

Transactions are now integrated with the blockchain system (CashCached tokens). When operations occur on FD accounts, corresponding transaction records are created in the `account_transactions` table.

## Transaction Recording Flow

### 1. Account Creation

**When**: New FD account is created
**Action**: Automatic initial DEPOSIT transaction created
**Code**: `AccountService.createAccount()` calls `recordInitialDepositTransaction()`
**Transaction Type**: `DEPOSIT`
**Amount**: Principal amount
**Reference**: `ACCOUNT_CREATION`

**Both standard and V2 (custom values) account creation methods now create initial deposit transactions.**

### 2. Wallet Deposits

**Endpoint**: `POST /api/accounts/{accountNo}/wallet/deposit`
**Requires**: `CUSTOMER` role
**Action**: Moves tokens FROM customer's CashCached wallet INTO the FD account
**Transaction Type**: `DEPOSIT`
**Records**: Created via `TransactionService.recordSelfTransaction()`

### 3. Wallet Withdrawals

**Endpoint**: `POST /api/accounts/{accountNo}/wallet/withdraw`
**Requires**: `CUSTOMER` role
**Action**: Moves tokens FROM FD account BACK TO customer's CashCached wallet
**Transaction Type**: `WITHDRAWAL`
**Records**: Created via `TransactionService.recordSelfTransaction()`

### 4. Bank Officer Transactions

**Endpoint**: `POST /api/accounts/{accountNo}/transactions`
**Requires**: `BANKOFFICER` or `ADMIN` role
**Action**: Record deposit, withdrawal, interest credit, penalty, or other transaction types
**Transaction Types**:

- `DEPOSIT` - Direct deposit to account
- `WITHDRAWAL` - Direct withdrawal from account
- `INTEREST_CREDIT` - Interest credited to account
- `PREMATURE_CLOSURE` - Premature closure payout
- `MATURITY_PAYOUT` - Maturity payout
- `PENALTY_DEBIT` - Penalty charged to account
- `REVERSAL` - Reverse a previous transaction

## Transaction Schema

```
account_transactions
├── id (PK)
├── transaction_id (unique, indexed)
├── account_no (indexed, FK)
├── transaction_type (indexed, ENUM)
├── amount (decimal)
├── balance_after (decimal)
├── description (varchar)
├── reference_no (varchar)
├── transaction_date (datetime)
├── processed_by (varchar)
├── remarks (varchar)
└── created_at (datetime auto)
```

## Database Queries

### Check transactions for account

```sql
SELECT * FROM account_transactions
WHERE account_no = 'IN79SBINBR001000000000000100000033'
ORDER BY transaction_date DESC;
```

### Check transaction count

```sql
SELECT COUNT(*) FROM account_transactions;
```

### Check transactions by type

```sql
SELECT transaction_type, COUNT(*) as count
FROM account_transactions
GROUP BY transaction_type;
```

## Integration Points

### CashCached (Blockchain) Integration

- Account creation issues tokens to customer wallet
- Account creation transaction records principal deposit
- Wallet deposits/withdrawals sync with transaction records
- Ledger entries track blockchain operations
- Wallet entries maintain customer balances

### Transaction Tracking

- All account operations create audit trail
- Each transaction has unique ID and hash
- Processed by field tracks who made the transaction
- Reference numbers link to external systems

## Future Enhancements

1. **Interest Accrual**: Automatic INTEREST_CREDIT transactions on interest accrual dates
2. **Maturity Processing**: Automatic MATURITY_PAYOUT transaction when account matures
3. **Transfers Between Accounts**: Transfer transactions when moving funds between accounts
4. **Premature Closure**: PREMATURE_CLOSURE transaction when account closed early
5. **Blockchain Confirmations**: Log blockchain transaction hash with each account transaction

## Testing

### Test Account Created

```
Account No: IN79SBINBR001000000000000100000033
Customer: 1
Status: ACTIVE
Principal: Initial deposit transaction should be created
```

### To Create Manual Transaction

```
POST http://localhost:8080/api/accounts/IN79SBINBR001000000000000100000033/transactions

{
  "transactionType": "DEPOSIT",
  "amount": "5000",
  "description": "Manual deposit",
  "referenceNo": "REF-2025-001"
}
```

### To View Transactions

```
GET http://localhost:8080/api/accounts/IN79SBINBR001000000000000100000033/transactions
```
