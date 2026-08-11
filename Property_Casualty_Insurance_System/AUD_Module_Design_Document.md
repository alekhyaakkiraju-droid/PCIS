# AUD Module Design Document

## Overview
The Audit module archives audit log records. AUD002B (archive batch) has shipped
COBOL source. AUDLOG01 (audit writer shared service) has no source.

## Programs
| Program  | Status          | Description                    |
|----------|-----------------|--------------------------------|
| AUD002B  | Shipped         | Audit Archive Batch            |
| AUDLOG01 | Missing-Callee  | Audit Log Writer Shared Service|

## Key Gaps (G-03, G-04, G-05)
- AUDLOG01 no source: 7 programs call it but no implementation exists
- Parameter shape drift: batch X(3)/X(30) vs interactive X(1)/X(100)/X(40)
- Audit failure does not roll back mutation (documented defect in 5 batch programs)
