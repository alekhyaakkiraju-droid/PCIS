# BIL Module Design Document

## Overview
The Billing module generates and tracks installment billing schedules.
BIL003B (installments batch) has shipped COBOL source.

## Programs
| Program | Status  | DDS       | Description               |
|---------|---------|-----------|---------------------------|
| BIL003B | Shipped | BILSTSD1  | Billing Installments Batch|

## Key Gaps
- U-B1: Billing schedule ID type mismatch (string 'BS'+seq vs numeric expected)
- I-02: POL001A does not create BILLING_PLAN_T row on policy issuance
