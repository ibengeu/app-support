import itertools
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum


class TicketStatus(Enum):
    OPEN = "OPEN"
    CLOSED = "CLOSED"
    REJECTED = "REJECTED"


counter = itertools.count(1)


@dataclass
class Ticket:
    """A support ticket raised by a user."""
    # Unique number that identifies this ticket
    id: int = field(init=False)
    # Short summary of the problem
    title: str
    # Full details of the problem
    description: str
    # Masked customer identifier linked to the issue, when known.
    customer_id: str | None = None
    # Payment reference linked to the issue, when known.
    payment_ref: str | None = None
    updated_at: datetime = field(default_factory=datetime.now)
    # date the ticket was created
    created_at: datetime = field(default_factory=datetime.now)
    # Name of the person handling the ticket. None means nobody is assigned yet.
    assigned_to: str | None = None
    status: TicketStatus = TicketStatus.OPEN

    def __post_init__(self):
        self.id = next(counter)
