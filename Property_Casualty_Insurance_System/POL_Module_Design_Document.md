# POL Module Design Document

## Overview
The Policy module manages policy issuance, inquiry, endorsement, and renewal.
POL001A (issuance) and POL006B (renewal) have shipped COBOL source.

## Programs
| Program | Status  | DDS       | Description               |
|---------|---------|-----------|---------------------------|
| POL001A | Shipped | POLMNTD1  | Policy Issuance           |
| POL006B | Shipped | POLREND1  | Policy Renewal Batch      |

## Key Gaps
- POL001A creates billing record without BILLING_PLAN_T row (I-02/P-06)
- POL006B date arithmetic on +1 MONTH (A-P4-1: leap year edge case)
