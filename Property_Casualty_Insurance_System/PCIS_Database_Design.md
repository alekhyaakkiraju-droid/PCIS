# PCIS Database Design

## Schema Overview
PCIS uses Db2 for i today with 55+ tables across 8 domains.

## Core Tables
- CUSTOMER_T: Customer master
- POLICY_T: Policy records
- COVERAGE_T: Coverage details
- CLAIM_T: Claim records
- CLAIM_RESERVE_T: Reserve amounts
- CLAIM_PAYMENT_T: Payment records
- CLAIM_ADJUSTER_T: Adjuster assignments
- BILLING_PLAN_T: Billing plans
- BILLING_SCHEDULE_T: Billing schedule
- COMMISSION_T: Commission records
- COMMISSION_RATE_T: Commission rate table
- AUDIT_LOG_T: Audit records
- AUDIT_LOG_ARCHIVE_T: Archived audit records
- APPROVAL_T: Approval workflow (X-07: three-way conflict resolved via ADR-001)

## Migration Notes
All 55 tables require classification for PII masking (BR-16).
