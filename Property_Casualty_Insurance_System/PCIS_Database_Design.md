# PCIS Database Design

## Schema Overview
PCIS uses Db2 for i today with **55 tables** across 8 domains
(CUS, AGT, QTE, UND, POL, PRM, BIL, PAY, CLM, REI, DOC, RPT, AUD, SEC).

Conventions:
- Business document keys are SEQUENCE-generated fixed-length VARCHAR/CHAR
- Detail/child surrogate keys use BIGINT GENERATED ALWAYS AS IDENTITY
- Money is DECIMAL(11,2) or DECIMAL(9,2) matching COMP-3 S9(11)V99 / S9(9)V99
- Every table carries CRT_USER, CRT_TIMESTAMP, UPD_USER, UPD_TIMESTAMP

## Table Inventory

| # | Table | Domain |
|---|-------|--------|
| 1 | CUSTOMER_T | CUS |
| 2 | CUSTOMER_ADDRESS_T | CUS |
| 3 | CUSTOMER_CONTACT_T | CUS |
| 4 | AGENT_T | AGT |
| 5 | AGENT_LICENSE_T | AGT |
| 6 | AGENT_COMMISSION_T | AGT |
| 7 | COMMISSION_T | AGT |
| 8 | COMMISSION_RATE_T | AGT |
| 9 | QUOTE_T | QTE |
| 10 | QUOTE_COVERAGE_T | QTE |
| 11 | UW_RULE_T | UND |
| 12 | UW_REFERRAL_T | UND |
| 13 | UW_DECISION_T | UND |
| 14 | POLICY_T | POL |
| 15 | COVERAGE_T | POL |
| 16 | COVERAGE_TYPE_T | POL |
| 17 | DEDUCTIBLE_T | POL |
| 18 | POLICY_HISTORY_T | POL |
| 19 | POLICY_VEHICLE_T | POL |
| 20 | POLICY_PROPERTY_T | POL |
| 21 | ENDORSEMENT_T | POL |
| 22 | CANCELLATION_REASON_T | POL |
| 23 | RATE_TABLE_T | PRM |
| 24 | RATE_FACTOR_T | PRM |
| 25 | PREMIUM_CALC_T | PRM |
| 26 | PREMIUM_CALC_DETAIL_T | PRM |
| 27 | DISCOUNT_RULE_T | PRM |
| 28 | SURCHARGE_RULE_T | PRM |
| 29 | TAX_TABLE_T | PRM |
| 30 | RISK_SCORE_FACTOR_T | PRM |
| 31 | BILLING_PLAN_T | BIL |
| 32 | BILLING_SCHEDULE_T | BIL |
| 33 | BILLING_NOTICE_T | BIL |
| 34 | INVOICE_T | BIL |
| 35 | PAYMENT_T | PAY |
| 36 | REFUND_T | PAY |
| 37 | CLAIM_T | CLM |
| 38 | CLAIM_RESERVE_T | CLM |
| 39 | CLAIM_RESERVE_HISTORY_T | CLM |
| 40 | CLAIM_PAYMENT_T | CLM |
| 41 | CLAIM_ADJUSTER_T | CLM |
| 42 | CLAIM_NOTE_T | CLM |
| 43 | CLAIM_DOCUMENT_T | CLM |
| 44 | APPROVAL_T | CLM |
| 45 | RECOVERY_T | CLM |
| 46 | REINSURANCE_TREATY_T | REI |
| 47 | REINSURANCE_CESSION_T | REI |
| 48 | DOCUMENT_T | DOC |
| 49 | RPT_RUN_LOG_T | RPT |
| 50 | RPT_PARM_T | RPT |
| 51 | AUDIT_LOG_T | AUD |
| 52 | AUDIT_LOG_ARCHIVE_T | AUD |
| 53 | SEC_USER_T | SEC |
| 54 | ROLE_MENU_T | SEC |
| 55 | CODE_TABLE_T | SEC |

## Table Definitions

### CUSTOMER_T

**Domain:** CUS
**Note:** G-06 name drift vs CUS001A host vars

| Column | Type | Description |
|--------|------|-------------|
| CUST_ID | VARCHAR(10) | Business key (SEQUENCE) |
| CUST_TYPE | CHAR(1) | I=Individual B=Business |
| CUST_NAME | VARCHAR(60) | Display name |
| FIRST_NAME | VARCHAR(30) | First name |
| LAST_NAME | VARCHAR(50) | Last name / legal entity |
| TAX_ID | VARCHAR(11) | SSN or EIN (PII) |
| DOB | DATE | Date of birth (PII) |
| EMAIL | VARCHAR(100) | Email (PII) |
| PHONE | VARCHAR(15) | Phone (PII) |
| CUST_STATUS | CHAR(10) | ACTIVE/INACTIVE/SUSPENDED |
| ASSIGNED_AGENT | VARCHAR(8) | FK AGENT_T.AGT_ID |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

```sql
CREATE TABLE CUSTOMER_T (
  CUST_ID VARCHAR(10) NOT NULL,
  CUST_TYPE CHAR(1) NOT NULL,
  CUST_NAME VARCHAR(60),
  FIRST_NAME VARCHAR(30),
  LAST_NAME VARCHAR(50),
  TAX_ID VARCHAR(11),
  DOB DATE,
  EMAIL VARCHAR(100),
  PHONE VARCHAR(15),
  CUST_STATUS CHAR(10),
  ASSIGNED_AGENT VARCHAR(8),
  CRT_USER VARCHAR(10),
  CRT_TIMESTAMP TIMESTAMP,
  UPD_USER VARCHAR(10),
  UPD_TIMESTAMP TIMESTAMP,
  PRIMARY KEY (CUST_ID)
);
```

### CUSTOMER_ADDRESS_T

**Domain:** CUS

| Column | Type | Description |
|--------|------|-------------|
| ADDRESS_ID | BIGINT | IDENTITY PK |
| CUST_ID | VARCHAR(10) | FK CUSTOMER_T |
| ADDR_TYPE | CHAR(10) | MAILING/BILLING/PROPERTY |
| ADDR_LINE1 | VARCHAR(50) | Street line 1 |
| ADDR_LINE2 | VARCHAR(50) | Street line 2 |
| CITY | VARCHAR(50) | City |
| STATE | CHAR(2) | State code |
| POSTAL_CODE | VARCHAR(10) | ZIP |
| EFFECTIVE_DATE | DATE | Effective date |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CUSTOMER_CONTACT_T

**Domain:** CUS

| Column | Type | Description |
|--------|------|-------------|
| CONTACT_ID | BIGINT | IDENTITY PK |
| CUST_ID | VARCHAR(10) | FK CUSTOMER_T |
| CONTACT_TYPE | CHAR(15) | EMERGENCY/BILLING/AGENT |
| CONTACT_NAME | VARCHAR(80) | Contact name |
| CONTACT_PHONE | VARCHAR(15) | Phone (PII) |
| CONTACT_EMAIL | VARCHAR(100) | Email (PII) |
| PREFERRED_FLAG | CHAR(1) | Y/N primary |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### AGENT_T

**Domain:** AGT

| Column | Type | Description |
|--------|------|-------------|
| AGT_ID | VARCHAR(8) | Business key |
| AGT_NAME | VARCHAR(60) | Agent name |
| AGT_STATUS | CHAR(10) | ACTIVE/INACTIVE |
| AGT_TYPE | CHAR(4) | Agent type |
| HIRE_DATE | DATE | Hire date |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### AGENT_LICENSE_T

**Domain:** AGT

| Column | Type | Description |
|--------|------|-------------|
| LICENSE_ID | BIGINT | IDENTITY PK |
| AGT_ID | VARCHAR(8) | FK AGENT_T |
| STATE | CHAR(2) | Licensed state |
| LICENSE_NBR | VARCHAR(20) | License number |
| EXPIRY_DATE | DATE | License expiry |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### AGENT_COMMISSION_T

**Domain:** AGT

| Column | Type | Description |
|--------|------|-------------|
| AGT_COMM_ID | BIGINT | IDENTITY PK |
| AGT_ID | VARCHAR(8) | FK AGENT_T |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| COMM_AMT | DECIMAL(9,2) | Commission amount |
| COMM_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### COMMISSION_T

**Domain:** AGT

| Column | Type | Description |
|--------|------|-------------|
| COMMISSION_ID | BIGINT | IDENTITY PK |
| AGT_ID | VARCHAR(8) | FK AGENT_T |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| COMMISSION_AMT | DECIMAL(9,2) | Commission amount |
| CALC_DATE | DATE | Calculation date |
| COMM_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### COMMISSION_RATE_T

**Domain:** AGT

| Column | Type | Description |
|--------|------|-------------|
| RATE_ID | BIGINT | IDENTITY PK |
| POLICY_TYPE | CHAR(4) | Policy type |
| COMM_RATE | DECIMAL(5,2) | Rate percent |
| EFF_DATE | DATE | Effective date |
| END_DATE | DATE | End date |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### QUOTE_T

**Domain:** QTE

| Column | Type | Description |
|--------|------|-------------|
| QUOTE_ID | VARCHAR(12) | Business key |
| CUST_ID | VARCHAR(10) | FK CUSTOMER_T |
| QUOTE_STATUS | CHAR(4) | Status |
| QUOTE_PREMIUM | DECIMAL(11,2) | Quoted premium |
| EFF_DATE | DATE | Proposed effective |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### QUOTE_COVERAGE_T

**Domain:** QTE

| Column | Type | Description |
|--------|------|-------------|
| QUOTE_COV_ID | BIGINT | IDENTITY PK |
| QUOTE_ID | VARCHAR(12) | FK QUOTE_T |
| COV_TYPE | CHAR(4) | Coverage type |
| LIMIT_AMT | DECIMAL(11,2) | Coverage limit |
| DED_AMT | DECIMAL(9,2) | Deductible |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### UW_RULE_T

**Domain:** UND

| Column | Type | Description |
|--------|------|-------------|
| UW_RULE_ID | BIGINT | IDENTITY PK |
| RULE_CODE | VARCHAR(20) | Rule code |
| RULE_DESC | VARCHAR(100) | Description |
| THRESHOLD_AMT | DECIMAL(11,2) | Referral threshold |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### UW_REFERRAL_T

**Domain:** UND

| Column | Type | Description |
|--------|------|-------------|
| REFERRAL_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| UW_RULE_ID | BIGINT | FK UW_RULE_T |
| REF_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### UW_DECISION_T

**Domain:** UND

| Column | Type | Description |
|--------|------|-------------|
| DECISION_ID | BIGINT | IDENTITY PK |
| REFERRAL_ID | BIGINT | FK UW_REFERRAL_T |
| DECISION_CODE | CHAR(4) | APPROVE/DECLINE |
| DECISION_USER | VARCHAR(10) | Underwriter |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### POLICY_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| POL_NBR | VARCHAR(12) | Business key |
| CUST_ID | VARCHAR(10) | FK CUSTOMER_T |
| AGT_ID | VARCHAR(8) | FK AGENT_T |
| POLICY_TYPE | CHAR(4) | Policy type |
| POL_STATUS | CHAR(4) | Status |
| EFF_DATE | DATE | Effective date |
| EXP_DATE | DATE | Expiration date |
| PREM_ANNUAL | DECIMAL(11,2) | Annual premium |
| RENEWAL_OF_POL | VARCHAR(12) | Prior term |
| BILL_FREQ | CHAR(1) | M/Q/S/A |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### COVERAGE_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| COVERAGE_ID | VARCHAR(14) | Business key |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| COV_TYPE | CHAR(4) | Coverage type |
| LIMIT_AMT | DECIMAL(11,2) | Limit |
| DED_AMT | DECIMAL(9,2) | Deductible |
| COV_PREMIUM | DECIMAL(9,2) | Coverage premium |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### COVERAGE_TYPE_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| COV_TYPE | CHAR(4) | PK coverage type |
| COV_DESC | VARCHAR(60) | Description |
| ACTIVE_FLAG | CHAR(1) | Y/N |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### DEDUCTIBLE_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| DEDUCT_ID | BIGINT | IDENTITY PK |
| COVERAGE_ID | VARCHAR(14) | FK COVERAGE_T |
| DED_AMT | DECIMAL(9,2) | Deductible amount |
| DED_TYPE | CHAR(4) | Type |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### POLICY_HISTORY_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| HIST_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| EVENT_CODE | CHAR(10) | Event |
| EVENT_DATE | DATE | Event date |
| EVENT_DESC | VARCHAR(100) | Description |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### POLICY_VEHICLE_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| VEHICLE_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| VIN | VARCHAR(17) | VIN |
| YEAR | INTEGER | Model year |
| MAKE | VARCHAR(30) | Make |
| MODEL | VARCHAR(30) | Model |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### POLICY_PROPERTY_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| PROPERTY_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| PROP_TYPE | CHAR(4) | Property type |
| ADDR_LINE1 | VARCHAR(50) | Location address |
| STATE | CHAR(2) | State |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### ENDORSEMENT_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| ENDORSE_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| END_TYPE | CHAR(4) | Endorsement type |
| EFF_DATE | DATE | Effective |
| PREM_CHG | DECIMAL(9,2) | Premium change |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CANCELLATION_REASON_T

**Domain:** POL

| Column | Type | Description |
|--------|------|-------------|
| CANCEL_REASON | CHAR(4) | PK reason code |
| REASON_DESC | VARCHAR(60) | Description |
| REFUND_ELIGIBLE | CHAR(1) | Y/N |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### RATE_TABLE_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| RATE_TABLE_ID | BIGINT | IDENTITY PK |
| POLICY_TYPE | CHAR(4) | Policy type |
| TERRITORY | CHAR(4) | Territory |
| BASE_RATE | DECIMAL(9,4) | Base rate |
| EFF_DATE | DATE | Effective |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### RATE_FACTOR_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| RATE_FACTOR_ID | BIGINT | IDENTITY PK |
| RATE_TABLE_ID | BIGINT | FK RATE_TABLE_T |
| FACTOR_CODE | VARCHAR(20) | Factor code |
| FACTOR_VALUE | DECIMAL(7,4) | Factor |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### PREMIUM_CALC_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| CALC_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| FINAL_PREMIUM | DECIMAL(11,2) | Final premium |
| CALC_DATE | DATE | Calculation date |
| SNAPSHOT_ID | VARCHAR(36) | Calc snapshot |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### PREMIUM_CALC_DETAIL_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| DETAIL_ID | BIGINT | IDENTITY PK |
| CALC_ID | BIGINT | FK PREMIUM_CALC_T |
| COMPONENT | VARCHAR(30) | Component name |
| COMPONENT_AMT | DECIMAL(11,2) | Component amount |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### DISCOUNT_RULE_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| DISC_RULE_ID | BIGINT | IDENTITY PK |
| DISC_CODE | VARCHAR(20) | Discount code |
| DISC_PCT | DECIMAL(5,2) | Percent |
| EFF_DATE | DATE | Effective |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### SURCHARGE_RULE_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| SUR_RULE_ID | BIGINT | IDENTITY PK |
| SUR_CODE | VARCHAR(20) | Surcharge code |
| SUR_PCT | DECIMAL(5,2) | Percent |
| EFF_DATE | DATE | Effective |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### TAX_TABLE_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| TAX_ID | BIGINT | IDENTITY PK |
| STATE | CHAR(2) | State |
| TAX_PCT | DECIMAL(5,4) | Tax percent |
| EFF_DATE | DATE | Effective |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### RISK_SCORE_FACTOR_T

**Domain:** PRM

| Column | Type | Description |
|--------|------|-------------|
| RISK_FACTOR_ID | BIGINT | IDENTITY PK |
| FACTOR_CODE | VARCHAR(20) | Factor |
| FACTOR_VALUE | DECIMAL(7,4) | Value |
| EFF_DATE | DATE | Effective |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### BILLING_PLAN_T

**Domain:** BIL

| Column | Type | Description |
|--------|------|-------------|
| BILL_PLAN_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| BILL_FREQ | CHAR(1) | M/Q/S/A |
| INSTALLMENT_CNT | INTEGER | Installment count |
| PLAN_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### BILLING_SCHEDULE_T

**Domain:** BIL
**Note:** Code uses DUE_AMT/PAID_AMT/BILL_STATUS/COMM_CALC_FLAG

| Column | Type | Description |
|--------|------|-------------|
| BILL_SCHED_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| BILL_PLAN_ID | BIGINT | FK BILLING_PLAN_T |
| INSTALLMENT_NBR | INTEGER | Installment number |
| DUE_DATE | DATE | Due date |
| AMT_DUE | DECIMAL(9,2) | Amount due (design name) |
| AMT_PAID | DECIMAL(9,2) | Amount paid (design name) |
| SCHED_STATUS | CHAR(1) | O=Open P=Paid V=Void |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

```sql
CREATE TABLE BILLING_SCHEDULE_T (
  BILL_SCHED_ID BIGINT GENERATED ALWAYS AS IDENTITY,
  POL_NBR VARCHAR(12) NOT NULL,
  BILL_PLAN_ID BIGINT NOT NULL,
  INSTALLMENT_NBR INTEGER NOT NULL,
  DUE_DATE DATE NOT NULL,
  AMT_DUE DECIMAL(9,2) NOT NULL,
  AMT_PAID DECIMAL(9,2),
  SCHED_STATUS CHAR(1) NOT NULL,
  CRT_USER VARCHAR(10),
  CRT_TIMESTAMP TIMESTAMP,
  UPD_USER VARCHAR(10),
  UPD_TIMESTAMP TIMESTAMP,
  PRIMARY KEY (BILL_SCHED_ID)
);
```

### BILLING_NOTICE_T

**Domain:** BIL

| Column | Type | Description |
|--------|------|-------------|
| NOTICE_ID | BIGINT | IDENTITY PK |
| BILL_SCHED_ID | BIGINT | FK BILLING_SCHEDULE_T |
| CUST_ID | VARCHAR(10) | FK CUSTOMER_T |
| NOTICE_DATE | DATE | Notice date |
| AMOUNT_DUE | DECIMAL(9,2) | Amount due |
| DELIVERY_EMAIL | VARCHAR(100) | Delivery email |
| NOTICE_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### INVOICE_T

**Domain:** BIL

| Column | Type | Description |
|--------|------|-------------|
| INVOICE_ID | BIGINT | IDENTITY PK |
| BILL_SCHED_ID | BIGINT | FK BILLING_SCHEDULE_T |
| INVOICE_DATE | DATE | Invoice date |
| INVOICE_DUE_DATE | DATE | Due date NOT NULL in design |
| INVOICE_AMT | DECIMAL(9,2) | Invoice amount |
| INVOICE_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### PAYMENT_T

**Domain:** PAY

| Column | Type | Description |
|--------|------|-------------|
| PAYMENT_ID | BIGINT | IDENTITY PK |
| INVOICE_ID | BIGINT | FK INVOICE_T |
| PAYMENT_DATE | DATE | Payment date |
| PAYMENT_AMT | DECIMAL(9,2) | Payment amount |
| PAYMENT_TOKEN | VARCHAR(40) | Tokenized instrument |
| PAYMENT_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### REFUND_T

**Domain:** PAY

| Column | Type | Description |
|--------|------|-------------|
| REFUND_ID | BIGINT | IDENTITY PK |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| REFUND_DATE | DATE | Refund date |
| REFUND_AMT | DECIMAL(9,2) | Refund amount |
| REFUND_REASON | CHAR(4) | Reason |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CLAIM_T

**Domain:** CLM

| Column | Type | Description |
|--------|------|-------------|
| CLAIM_ID | VARCHAR(12) | Business key |
| POL_NBR | VARCHAR(12) | FK POLICY_T |
| CUST_ID | VARCHAR(10) | FK CUSTOMER_T |
| CLAIM_STATUS | CHAR(4) | Status |
| LOSS_DATE | DATE | Date of loss |
| PAID_TO_DATE | DECIMAL(11,2) | Cumulative paid |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CLAIM_RESERVE_T

**Domain:** CLM
**Note:** Code uses RESERVE_ID/APPROVED_AMT/PAID_TO_DATE

| Column | Type | Description |
|--------|------|-------------|
| RESERVE_HIST_ID | BIGINT | IDENTITY PK (design) |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| RESERVE_AMT | DECIMAL(11,2) | Reserve amount (design) |
| CHANGE_REASON | VARCHAR(60) | Change reason (design) |
| RESERVE_STATUS | CHAR(2) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

```sql
CREATE TABLE CLAIM_RESERVE_T (
  RESERVE_HIST_ID BIGINT GENERATED ALWAYS AS IDENTITY,
  CLAIM_ID VARCHAR(12) NOT NULL,
  RESERVE_AMT DECIMAL(11,2) NOT NULL,
  CHANGE_REASON VARCHAR(60),
  RESERVE_STATUS CHAR(2),
  CRT_USER VARCHAR(10),
  CRT_TIMESTAMP TIMESTAMP,
  UPD_USER VARCHAR(10),
  UPD_TIMESTAMP TIMESTAMP,
  PRIMARY KEY (RESERVE_HIST_ID)
);
```

### CLAIM_RESERVE_HISTORY_T

**Domain:** CLM

| Column | Type | Description |
|--------|------|-------------|
| RES_HIST_ID | BIGINT | IDENTITY PK |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| OLD_AMT | DECIMAL(11,2) | Prior reserve |
| NEW_AMT | DECIMAL(11,2) | New reserve |
| CHANGE_REASON | VARCHAR(60) | Reason |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CLAIM_PAYMENT_T

**Domain:** CLM

| Column | Type | Description |
|--------|------|-------------|
| PAYMENT_ID | BIGINT | IDENTITY PK |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| ADJUSTER_ID | VARCHAR(10) | FK CLAIM_ADJUSTER_T |
| PAYMENT_AMT | DECIMAL(11,2) | Payment amount |
| PAYMENT_DATE | DATE | Payment date |
| PAYEE_NAME | VARCHAR(60) | Payee (PII) |
| PAYMENT_STATUS | CHAR(4) | I/P/V |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CLAIM_ADJUSTER_T

**Domain:** CLM

| Column | Type | Description |
|--------|------|-------------|
| ADJUSTER_ID | VARCHAR(10) | Business key |
| ADJ_NAME | VARCHAR(60) | Name |
| AUTHORITY_LIMIT | DECIMAL(11,2) | Authority limit |
| ADJ_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CLAIM_NOTE_T

**Domain:** CLM

| Column | Type | Description |
|--------|------|-------------|
| NOTE_ID | BIGINT | IDENTITY PK |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| NOTE_TEXT | VARCHAR(500) | Note body |
| NOTE_USER | VARCHAR(10) | Author |
| NOTE_DATE | TIMESTAMP | Created |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CLAIM_DOCUMENT_T

**Domain:** CLM

| Column | Type | Description |
|--------|------|-------------|
| DOC_ID | BIGINT | IDENTITY PK |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| DOC_TYPE | CHAR(10) | Document type |
| DOC_PATH | VARCHAR(200) | IFS/object path |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### APPROVAL_T

**Domain:** CLM
**Note:** Table 43; ADR-001 three-way conflict resolved

| Column | Type | Description |
|--------|------|-------------|
| APPROVAL_ID | BIGINT | IDENTITY PK |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| APPROVER_ID | VARCHAR(10) | Approver |
| APPROVED_AMT | DECIMAL(11,2) | Approved amount |
| APPROVAL_STATUS | CHAR(10) | REQUESTED/APPROVED/DENIED |
| APPROVAL_DATE | DATE | Decision date |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### RECOVERY_T

**Domain:** CLM

| Column | Type | Description |
|--------|------|-------------|
| RECOVERY_ID | BIGINT | IDENTITY PK |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| RECOVERY_AMT | DECIMAL(11,2) | Recovery amount |
| RECOVERY_STATUS | CHAR(4) | Status |
| RECOVERY_DATE | DATE | Date |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### REINSURANCE_TREATY_T

**Domain:** REI

| Column | Type | Description |
|--------|------|-------------|
| TREATY_ID | VARCHAR(12) | Business key |
| TREATY_NAME | VARCHAR(60) | Name |
| CESSION_THRESHOLD | DECIMAL(11,2) | Cession threshold |
| TREATY_STATUS | CHAR(4) | Status |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### REINSURANCE_CESSION_T

**Domain:** REI

| Column | Type | Description |
|--------|------|-------------|
| CESSION_ID | BIGINT | IDENTITY PK |
| TREATY_ID | VARCHAR(12) | FK REINSURANCE_TREATY_T |
| CLAIM_ID | VARCHAR(12) | FK CLAIM_T |
| CESSION_AMT | DECIMAL(11,2) | Ceded amount |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### DOCUMENT_T

**Domain:** DOC

| Column | Type | Description |
|--------|------|-------------|
| DOCUMENT_ID | BIGINT | IDENTITY PK |
| OWNER_TYPE | CHAR(10) | POLICY/CLAIM/CUSTOMER |
| OWNER_KEY | VARCHAR(20) | Owner key |
| DOC_TYPE | CHAR(10) | Type |
| DOC_URI | VARCHAR(200) | Storage URI |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### RPT_RUN_LOG_T

**Domain:** RPT

**Authoritative DDL:** `baseline/ddl/RPT_RUN_LOG_T_reconciled.sql` (WO-237)

**Drift rationale:** An earlier revision of this document defined `RUN_ID`, `PROGRAM_NAME`, `ROWS_PROCESSED`, `RUN_STATUS`, `CRT_USER`, `UPD_USER`, and `UPD_TIMESTAMP`. Those names never matched a shipped COBOL INSERT. The six Phase 0 batch programs (`AUD002B`, `BIL003B`, `CLM006B`, `CMM001B`, `POL006B`, `PRM005B`) previously only `DISPLAY`ed completion counters and performed **no** `INSERT` into `RPT_RUN_LOG_T`. WO-237 adds run-log instrumentation and reconciles the schema to the columns below, including wall-clock `START_TIMESTAMP` / `END_TIMESTAMP` for batch-window baselines. `REC_DELINQUENT` is nullable and written only by `PRM005B`.

| Column | Type | Description |
|--------|------|-------------|
| RUN_LOG_ID | BIGINT | GENERATED ALWAYS AS IDENTITY PK |
| PGM_NAME | VARCHAR(10) | Batch program name |
| RUN_DATE | DATE | Business run date |
| REC_SELECTED | INTEGER | Records selected / read |
| REC_UPDATED | INTEGER | Records updated / written / billed / copied |
| REC_ERRORS | INTEGER | Error count |
| REC_DELINQUENT | INTEGER | Nullable; delinquency count (PRM005B only) |
| START_TIMESTAMP | TIMESTAMP(6) | Wall-clock start (captured in 1000-INITIALIZE) |
| END_TIMESTAMP | TIMESTAMP(6) | Wall-clock end (captured in 8000-WRITE-RUN-LOG) |
| CRT_TIMESTAMP | TIMESTAMP | Row create timestamp |

### RPT_PARM_T

**Domain:** RPT

| Column | Type | Description |
|--------|------|-------------|
| PARM_ID | BIGINT | IDENTITY PK |
| PROGRAM_NAME | VARCHAR(10) | Program |
| PARM_NAME | VARCHAR(30) | Parameter |
| PARM_VALUE | VARCHAR(100) | Value |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### AUDIT_LOG_T

**Domain:** AUD
**Note:** Column triad conflict: USER_ID vs CHG_USER; LOG_TIMESTAMP vs CRT_TIMESTAMP

| Column | Type | Description |
|--------|------|-------------|
| LOG_ID | BIGINT | IDENTITY PK |
| PROGRAM_NAME | VARCHAR(10) | Program |
| ACTION_CODE | VARCHAR(10) | Action |
| TABLE_NAME | VARCHAR(30) | Table |
| RECORD_KEY | VARCHAR(40) | Key |
| USER_ID | VARCHAR(10) | User (CUS design) |
| OLD_VALUE | VARCHAR(100) | Before image |
| NEW_VALUE | VARCHAR(100) | After image |
| LOG_TIMESTAMP | TIMESTAMP | Event time (AUD002B) |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### AUDIT_LOG_ARCHIVE_T

**Domain:** AUD

| Column | Type | Description |
|--------|------|-------------|
| LOG_ID | BIGINT | PK from AUDIT_LOG_T |
| PROGRAM_NAME | VARCHAR(10) | Program |
| ACTION_CODE | VARCHAR(10) | Action |
| TABLE_NAME | VARCHAR(30) | Table |
| RECORD_KEY | VARCHAR(40) | Key |
| USER_ID | VARCHAR(10) | User |
| LOG_TIMESTAMP | TIMESTAMP | Original timestamp |
| ARCHIVE_DATE | DATE | Archive date |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### SEC_USER_T

**Domain:** SEC

| Column | Type | Description |
|--------|------|-------------|
| USER_ID | VARCHAR(10) | PK |
| USER_NAME | VARCHAR(60) | Display name |
| USER_STATUS | CHAR(4) | Status |
| ROLE_CODE | VARCHAR(20) | Primary role |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### ROLE_MENU_T

**Domain:** SEC

| Column | Type | Description |
|--------|------|-------------|
| ROLE_CODE | VARCHAR(20) | Role |
| MENU_OPTION | VARCHAR(10) | Menu option |
| ALLOW_FLAG | CHAR(1) | Y/N |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

### CODE_TABLE_T

**Domain:** SEC

| Column | Type | Description |
|--------|------|-------------|
| CODE_TYPE | VARCHAR(20) | Code domain |
| CODE_VALUE | VARCHAR(20) | Value |
| CODE_DESC | VARCHAR(60) | Description |
| ACTIVE_FLAG | CHAR(1) | Y/N |
| CRT_USER | VARCHAR(10) | Create user |
| CRT_TIMESTAMP | TIMESTAMP | Create timestamp |
| UPD_USER | VARCHAR(10) | Update user |
| UPD_TIMESTAMP | TIMESTAMP | Update timestamp |

## Migration Notes
All 55 tables require classification for PII masking (BR-16).
Gap G-06: COBOL host variable / column names drift from this DDL for several
core tables (CUSTOMER_T, POLICY_T, BILLING_SCHEDULE_T, CLAIM_RESERVE_T,
CLAIM_PAYMENT_T, AUDIT_LOG_T). Authoritative reconciliation is
`docs/data-dictionary.yaml` (WO-128).

