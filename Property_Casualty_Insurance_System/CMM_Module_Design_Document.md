# CMM Module Design Document

## Overview
The Commission module calculates and records agent commissions.
CMM001B (commission calculation batch) has shipped COBOL source.

## Programs
| Program | Status  | DDS       | Description                  |
|---------|---------|-----------|------------------------------|
| CMM001B | Shipped | COMRPTD1  | Commission Calculation Batch |

## Commission Rate Logic
CMM001B reads active policies, looks up rates in COMMISSION_RATE_T,
calculates commission amounts, and inserts records into COMMISSION_T.
Calls AUDLOG01 for each commission record created.
