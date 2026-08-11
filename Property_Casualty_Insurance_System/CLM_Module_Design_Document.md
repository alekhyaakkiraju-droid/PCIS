# CLM Module Design Document

## Overview
The Claims module manages FNOL intake, reserve management, adjuster assignment,
and payment processing. Only CLM006B (batch payment) has shipped COBOL source.
CLM001A through CLM005A are design-only.

## Programs
| Program | Status      | DDS       | Description                   |
|---------|-------------|-----------|-------------------------------|
| CLM001A | Design-Only | CLMFNLD1  | FNOL Intake Interactive       |
| CLM002A | Design-Only | CLMRSVD1  | Reserve Management            |
| CLM003A | Design-Only | CLMADJD1  | Adjuster Assignment           |
| CLM004A | Design-Only | CLMINQD1  | Claim Inquiry                 |
| CLM005A | Design-Only | CLMCLSD1  | Claim Closure                 |
| CLM006B | Shipped     | CLMPAYD1  | Payment Batch Processing      |

## Key Gaps
- CLM006B has no SECCHK01 call (G-02): authority check missing
- APPROVAL_T ignored in CLM006B (ADR-001)
- AUDLOG01 called but no source (G-03)
