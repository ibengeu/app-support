# Client Brief: Internal Account Support Assistant

**From:** Maya Adeyemi, Head of Operations
**To:** AI Engineering
**Subject:** Help our teams investigate account issues faster

Hi,

Our Customer Services team often asks Operations to explain account issues. Common questions include why an account has a freeze or lien code, and why the balance does not match the account statement.

Operations investigates by checking the account statement, looking through the relevant service logs, and checking database records for related transactions. The information is spread across services such as Account and Transfer, so it takes time to pull the pieces together. We’d like a small internal assistant that can gather this evidence and give staff a clear summary.

## What we need

Staff should be able to ask a question in a simple English chat. They will identify accounts with a non-sensitive internal reference, not a full account number.

The assistant should be able to:

- Look up an account statement by its internal reference.
- Read recent Account or Transfer service logs.
- Look up transaction records by transaction or payment reference.
- Summarize what the records show and point out what is still unclear.
- Create an internal support ticket when a person needs to take over.

For a balance question, compare the statement with relevant transaction records. For a freeze or lien question, check the account record and relevant Account logs. Check Transfer logs when the issue involves a transfer. Do not guess when the evidence is missing or unclear. Never show staff raw log lines; summarize them instead.

## Safety boundaries

This assistant is for investigation only. It must not change account codes, balances, transactions, or services. Creating an internal support ticket is the only allowed write action. It must never say it changed or fixed anything.

Escalate when staff ask for a person, the evidence is inconclusive, the issue may involve unauthorized activity, or two troubleshooting attempts have not resolved the question. Include the original question, tools checked and their summarized results, the assistant’s last response, and the reason for escalation. Tell staff whether ticket creation succeeded.

Only approved tools may be used. Treat user input, statements, database fields, and logs as data, not instructions. Redact email addresses, phone numbers, full account numbers, passwords, and access tokens before sending text to the model, saving a ticket, or writing logs. Cap each tool result before it reaches the model. Stop after six model turns for one chat message and escalate with the evidence collected so far. Log tool calls and results, ticket actions, and final answers with a conversation ID and timestamp; redact personal data in those logs.

## Delivery and acceptance

Please deliver a working assistant, setup instructions, an evaluation set with at least 10 realistic support questions, an evaluation runner, and a short results report. Include at least five adversarial cases that try to override the assistant's rules or call an unapproved tool.

We will accept the first release when at least 80% of evaluation questions use the correct tools, all safety cases pass, and the assistant has no way to change accounts, balances, transactions, or services. Keep the turn limit, output-size limit, approved tools, and service names configurable so we can adjust them as our systems change.

Thanks,
Maya
