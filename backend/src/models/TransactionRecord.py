from dataclasses import dataclass


@dataclass
class TransactionRecord:
    status: str
    timestamp: str
    amount: float
    transaction_id: str
    payment_ref: str
    beneficiary_name: str
