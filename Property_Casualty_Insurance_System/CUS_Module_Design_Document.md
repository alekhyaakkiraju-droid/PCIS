# CUS Module Design Document

## Overview

The Customer (CUS) module manages the lifecycle of customer records within PCIS.
It provides customer creation, maintenance, inquiry, and deletion functions via
5250 green-screen panels (CUS001A through CUS005A).

## Module Scope

| Program  | Type        | Status      | DDS Panel  | Description                        |
|----------|-------------|-------------|------------|------------------------------------|
| CUS001A  | Interactive | Shipped     | CUSMNTD1   | Customer Maintenance               |
| CUS002A  | Interactive | Design-Only | CUSINQD1   | Customer Inquiry                   |
| CUS003A  | Interactive | Design-Only | CUSLSTD1   | Customer List                      |
| CUS004A  | Interactive | Design-Only | CUSDELD1   | Customer Delete                    |
| CUS005A  | Interactive | Design-Only | CUSCNTD1   | Customer Contacts                  |

## Database Tables

### CUSTOMER_T

Primary customer master table.

| Column          | Type          | Description                      |
|-----------------|---------------|----------------------------------|
| CUSTOMER_ID     | INTEGER       | Primary key (sequence)           |
| TAX_ID          | VARCHAR(11)   | SSN or EIN (PII — masked)        |
| CUSTOMER_TYPE   | CHAR(1)       | I=Individual, B=Business         |
| FIRST_NAME      | VARCHAR(30)   | First name                       |
| LAST_NAME       | VARCHAR(50)   | Last name or business name       |
| EMAIL           | VARCHAR(100)  | Email address (PII)              |
| PHONE           | VARCHAR(15)   | Phone number (PII)               |
| STATUS          | CHAR(10)      | ACTIVE / INACTIVE / SUSPENDED    |
| ASSIGNED_AGENT  | INTEGER       | FK to AGENT_T                    |
| CREATED_DATE    | DATE          | Record creation date             |
| LAST_UPDATED    | TIMESTAMP     | Last modification timestamp      |

### ADDRESS_T

| Column          | Type          | Description                      |
|-----------------|---------------|----------------------------------|
| ADDRESS_ID      | INTEGER       | Primary key                      |
| CUSTOMER_ID     | INTEGER       | FK to CUSTOMER_T                 |
| ADDRESS_TYPE    | CHAR(10)      | MAILING / BILLING / PROPERTY     |
| STREET_1        | VARCHAR(50)   | Street line 1                    |
| STREET_2        | VARCHAR(50)   | Street line 2 (optional)         |
| CITY            | VARCHAR(50)   | City                             |
| STATE           | CHAR(2)       | State code                       |
| ZIP_CODE        | CHAR(10)      | ZIP+4 code                       |
| EFFECTIVE_DATE  | DATE          | Address effective date           |

### CONTACT_T

| Column          | Type          | Description                      |
|-----------------|---------------|----------------------------------|
| CONTACT_ID      | INTEGER       | Primary key                      |
| CUSTOMER_ID     | INTEGER       | FK to CUSTOMER_T                 |
| CONTACT_TYPE    | CHAR(15)      | EMERGENCY / BILLING / AGENT      |
| CONTACT_NAME    | VARCHAR(80)   | Full name of contact             |
| CONTACT_PHONE   | VARCHAR(15)   | Phone (PII)                      |
| CONTACT_EMAIL   | VARCHAR(100)  | Email (PII)                      |
| IS_PRIMARY      | CHAR(1)       | Y=Primary contact                |

## Business Rules

### BR-01: Unique Tax Identifier

Each customer must have a unique TAX_ID within CUSTOMER_T. Duplicate tax IDs
trigger a hard stop (message CUS0015) in the legacy interactive path.

**Edge Case A-P1-1 (IMPROVE):** The modernized target allows an authorized
supervisor to override the duplicate detection with a signed justification.
This is an intentional improvement over legacy behaviour.

### BR-02: Customer Type Validation

CUSTOMER_TYPE must be 'I' (Individual) or 'B' (Business). Individual customers
require FIRST_NAME + LAST_NAME. Business customers require LAST_NAME as the
legal entity name.

### BR-03: Address Requirement

**Legacy:** Address and contact information are optional in the CUS001A panel.
**Target (BR-19):** Both mailing address and at least one contact record are
required for new customer creation. This is an intentional IMPROVE disposition.

Edge Case A-P1-2: Address/contact optional-as-group in legacy vs both required
in PRD BR-19. Signed waiver required before Phase 1 gate.

### BR-04: Customer Status Transitions

Valid status transitions:
- ACTIVE → INACTIVE (customer request or inactivity)
- ACTIVE → SUSPENDED (non-payment or compliance hold)
- INACTIVE → ACTIVE (reactivation with supervisor approval)
- SUSPENDED → ACTIVE (compliance clearance)
- INACTIVE → SUSPENDED (compliance triggered)

### BR-05: PII Classification

The following fields are classified as RESTRICTED tier:
- TAX_ID — masked to last-4 digits in display
- EMAIL — masked to domain-only in display
- PHONE — masked to last-4 digits in display
- CONTACT_PHONE — masked to last-4 digits
- CONTACT_EMAIL — masked to domain-only

Unmask requires UNMASK_PII permission plus recorded justification in AUDIT_LOG_T.

## CUS001A — Customer Maintenance Interactive Program

### Purpose

CUS001A is the only shipped interactive program in the CUS module. It provides
full CRUD operations on customer master records: create, read, update, and
soft-delete (status change). It uses the CUSMNTD1 display file.

### Program Flow

```
1. Display CUSMNTD1 main panel
2. Receive function key or action code
3. F3/F12 → exit
4. Action A (Add) → validate → INSERT CUSTOMER_T → audit
5. Action C (Change) → read → display → validate → UPDATE → audit
6. Action D (Delete) → confirm → UPDATE status=INACTIVE → audit
7. Action I (Inquiry) → read → display (read-only)
```

### Shared Services Called

| Service   | Purpose                              | Gap?        |
|-----------|--------------------------------------|-------------|
| SECCHK01  | Authorize the requested action       | No source   |
| CUSVAL01  | Validate customer data               | No source   |
| AUDLOG01  | Write audit record (PAY/UPD/DEL/ADD) | No source   |

**Gap G-02:** SECCHK01 has no source member. Authorization is convention-only.
The deny-by-default authz-svc must be built from scratch in the target.

**Gap G-03:** AUDLOG01 has no source member. The audit record format used by
CUS001A is the interactive shape: X(1)/X(100)/X(40).

## CUS002A through CUS005A — Design-Only Programs

These programs are fully specified in this design document and have corresponding
DDS display files but no COBOL source members in the repository.

### CUS002A — Customer Inquiry

Uses CUSINQD1. Read-only view of customer data with PII masking.
Allows search by customer ID, name, or tax ID (last-4 only).

### CUS003A — Customer List

Uses CUSLSTD1. Subfile display of customers matching a search criterion.
Supports scrolling through results with forward and backward paging.

### CUS004A — Customer Delete

Uses CUSDELD1. Soft-delete confirmation panel. Changes STATUS to INACTIVE.
Requires supervisor authority (SECCHK01 with DELETE_CUSTOMER permission).
Validates no active policies exist before allowing delete.

### CUS005A — Customer Contacts

Uses CUSCNTD1. Manages the CONTACT_T records for a customer.
Add, change, and delete contact records with primary contact designation.

## Migration Edge Cases

### P-C1: TAX_ID Type Mismatch

Legacy CUS001A stores TAX_ID as CHAR(11) with hyphens (XXX-XX-XXXX).
The target schema normalises to VARCHAR(11) without hyphens.
Flyway migration must strip hyphens and convert existing records.

### P-C2: Phone Format

Legacy stores phone as CHAR(15) in format (NNN) NNN-NNNN.
Target normalises to E.164 format: +1XXXXXXXXXX.
Migration must apply format normalisation with validation.

### P-C3: CUSTOMER_ID Sequence Gap

Legacy CUSTOMER_ID is generated by a COBOL program using an internal counter.
Gaps exist in the sequence from test records and deletions.
Target uses PostgreSQL SERIAL/IDENTITY. Migration must preserve existing IDs
and set the sequence start above the max existing ID.

## Acceptance Tests

### UT-CUS-001: Create Customer — Happy Path (PARITY)
- Input: valid Individual customer with TAX_ID, name, address, contact
- Expected: CUSTOMER_T INSERT with ACTIVE status, audit record written
- Golden output: verify customer_id, status, audit log entry

### UT-CUS-002: Duplicate Tax ID (IMPROVE)
- Input: customer with TAX_ID matching existing record
- Legacy expected: CUS0015 hard stop, no insert
- Target expected: warning dialog with supervisor override capability
- Disposition: IMPROVE — signed waiver required before Phase 1 gate

### UT-CUS-003: PII Masking (NEW)
- Input: customer with TAX_ID and EMAIL
- Expected: display shows XXX-XX-####, @domain.com
- Unmask requires UNMASK_PII permission in SECCHK01/authz-svc

### UT-CUS-004: Status Transition Guard (PARITY)
- Input: attempt to delete customer with active POLICY_T records
- Expected: rejection with message CUS0031
- Golden output: verify status unchanged, audit record for failed attempt

### UT-CUS-005: Audit on Update (PARITY)
- Input: update customer email
- Expected: AUDIT_LOG_T record with action=UPD, old_value, new_value
- Masking: old_value and new_value must be masked before audit write

## Open Questions

- **OQ-CUS-01:** Should CONTACT_T records be transferred when customer is merged?
  (CUS004A merge is out of scope for Phase 1 but design must accommodate it.)
- **OQ-CUS-02:** What is the retention period for INACTIVE customer records?
  (BR-16 requires classification; 55-table registry must include CUSTOMER_T.)
- **OQ-CUS-03:** Is address validation against USPS API required in target?
  (Legacy does no external address validation.)

## Revision History

| Version | Date       | Author          | Change                            |
|---------|------------|-----------------|-----------------------------------|
| 1.0     | 2023-06-01 | PCIS Dev Team   | Initial module design             |
| 1.1     | 2023-09-15 | PCIS Dev Team   | Added PII classification section  |
| 1.2     | 2024-01-10 | PCIS Dev Team   | Edge cases and acceptance tests   |
| 1.3     | 2024-03-22 | PCIS Dev Team   | BR-19 address requirement added   |


---

*End of CUS Module Design Document*




















































































































































































































































