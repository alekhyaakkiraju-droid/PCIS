# Sample DDL (WO-128 fixtures)

### SAMPLE_CUSTOMER_T

**Domain:** CUS

| Column | Type | Description |
|--------|------|-------------|
| CUST_ID | VARCHAR(10) | Business key |
| DOB | DATE | Date of birth |
| TAX_ID | VARCHAR(11) | Tax identifier |
| EMAIL | VARCHAR(100) | Email |
| FIRST_NAME | VARCHAR(30) | First name |
| LAST_NAME | VARCHAR(50) | Last name |

```sql
CREATE TABLE SAMPLE_CUSTOMER_T (
  CUST_ID VARCHAR(10) NOT NULL,
  DOB DATE,
  TAX_ID VARCHAR(11),
  EMAIL VARCHAR(100),
  FIRST_NAME VARCHAR(30),
  LAST_NAME VARCHAR(50),
  PRIMARY KEY (CUST_ID)
);
```
