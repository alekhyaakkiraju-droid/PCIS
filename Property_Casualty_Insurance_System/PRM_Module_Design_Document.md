# PRM Module Design Document

## Overview
The Premium module handles delinquency aging and premium calculations.
PRM005B has shipped COBOL source.

## Programs
| Program | Status  | DDS       | Description                  |
|---------|---------|-----------|------------------------------|
| PRM005B | Shipped | PRMINQD1  | Premium Delinquency Batch    |

## Key Notes (ADR-002)
PRM005B is named premiumDelinquencyJob. Its prologue claims PRMCLC01 is called
but the PROCEDURE DIVISION contains zero CALL 'PRMCLC01' statements. The prologue
is incorrect. PRMCLC01 has no source in the repository (missing callee).
