import json
from pathlib import Path

from faker import Faker
from langchain_core.tools import tool

from models.ticket import Ticket

LOG_DIR = Path("logs").resolve()
ARTICLES_DIR = Path("articles").resolve()
MAX_LOG_CHARS = 5000
CREATED_TICKETS = []
faker = Faker()

def make_fake_transactions():
    """Create database records that can be compared with the sample logs."""
    return [
        {
            "customer_id": "CUST-***3812",
            "payment_ref": "PAY-10041",
            "transaction_id": "TXN-50041",
            "timestamp": "2026-11-03T08:14:04Z",
            "amount": 25000.00,
            "currency": "NGN",
            "status": "SUCCESS",
            "error_code": None,
        },
        {
            "customer_id": "CUST-***6140",
            "payment_ref": "PAY-10042",
            "transaction_id": "TXN-50042",
            "timestamp": "2026-11-03T08:15:28Z",
            "amount": 150000.00,
            "currency": "NGN",
            "status": "FAILURE",
            "error_code": "GW_TIMEOUT",
        },
        {
            "customer_id": "CUST-***6140",
            "payment_ref": "PAY-10043",
            "transaction_id": "TXN-50043",
            "timestamp": "2026-11-03T09:02:11Z",
            "amount": 5000.00,
            "currency": "NGN",
            "status": "FAILURE",
            "error_code": "INSUFFICIENT_FUNDS",
        },
        {
            # Deliberate mismatch: logs report FAILURE for this payment.
            "customer_id": "CUST-***9275",
            "payment_ref": "PAY-10044",
            "transaction_id": "TXN-50044",
            "timestamp": "2026-11-03T09:46:01Z",
            "amount": 75000.00,
            "currency": "NGN",
            "status": "SUCCESS",
            "error_code": None,
        },
        {
            # No corresponding entry exists in the supplied logs.
            "customer_id": "CUST-***5521",
            "payment_ref": "PAY-10045",
            "transaction_id": "TXN-50045",
            "timestamp": "2026-11-03T10:12:00Z",
            "amount": 12500.00,
            "currency": "NGN",
            "status": "SUCCESS",
            "error_code": None,
        },
        {
            "customer_id": "CUST-***3812",
            "payment_ref": "PAY-10046",
            "transaction_id": "TXN-50046",
            "timestamp": "2026-11-03T10:18:00Z",
            "amount": 4200.00,
            "currency": "NGN",
            "status": "FAILURE",
            "error_code": "INSUFFICIENT_FUNDS",
        }
    ]


transactions = make_fake_transactions()


@tool
def check_logs(file_name: str = "payment-log.txt") -> str:
    """Read the end of one application log file by file name."""
    path = (LOG_DIR / file_name).resolve()

    if not path.is_relative_to(LOG_DIR):
        return "Access denied: the file is outside the logs directory."
    if not path.is_file():
        return f"Log file not found: {file_name}"

    log_text = path.read_text(encoding="utf-8", errors="replace")
    return log_text[-MAX_LOG_CHARS:]


@tool
def check_database(payment_ref: str = "") -> str:
    """Look up a fake payment record, or return all records if no reference is given."""
    if payment_ref:
        matching_transactions = [
            transaction
            for transaction in transactions
            if transaction["payment_ref"] == payment_ref
        ]
    else:
        matching_transactions = transactions

    return json.dumps(matching_transactions)


@tool
def check_services() -> str:
    """Check the status of all practice services."""
    service_names = ("transfer", "account", "customer")
    all_services = [
        {
            service_name: {
                "status": faker.random_element(["healthy", "not healthy"]),
            }
            for service_name in service_names
        }
    ]

    return json.dumps(all_services)


@tool
def search_docs() -> str:
    """Read the help articles about payments and self-service."""
    text = Path(ARTICLES_DIR / "HELP_ARTICLES.md").read_text(
        encoding="utf-8", errors="replace"
    )
    return text


@tool
def create_support_ticket(
    title: str,
    description: str,
    customer_id: str = "",
    payment_ref: str = "",
) -> str:
    """Create a support ticket with issue details and optional customer references."""
    ticket = Ticket(
        title=title,
        description=description,
        customer_id=customer_id or None,
        payment_ref=payment_ref or None,
    )
    CREATED_TICKETS.append(ticket)

    return f"Ticket {ticket.id} created successfully."

def escalate_to_human(ticket: Ticket) -> str:
    """Escalate a ticket to human."""
    # simulate email call or websocket.
    return (
        f"Ticket {ticket.id} escalated successfully. "
        "please expect feedback in 24 hours."
    )
