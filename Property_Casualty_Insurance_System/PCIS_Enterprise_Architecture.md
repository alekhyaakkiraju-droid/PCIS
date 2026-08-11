# PCIS Enterprise Architecture

## Overview
PCIS (Property & Casualty Insurance System) is a full-lifecycle IBM i application
covering customer, policy, claims, billing, premium, commission, and audit domains.

## Current State
Single-partition IBM i application with ILE COBOL, embedded SQL, DDS 5250 panels.

## Target Architecture
Microservices on Amazon EKS with Spring Boot 3.5, Aurora PostgreSQL, React 19 SPA.

## Phase Plan
- Phase 0: CI/CD, IaC, shared kernels, golden harness, data dictionary
- Phase 1: Customer domain migration with parallel run
- Phase 2: Claims domain migration
- Phase 3: Billing and Premium migration
- Phase 4: Policy migration
- Phase 5: Reporting, purge automation, decommission

## Key ADRs
- ADR-001: APPROVAL_T resolution — PCIS_Database_Design.md definition adopted
- ADR-002: PRM005B renamed to premiumDelinquencyJob
- ADR-003: CLM006B batch pays full outstanding; interactive allows partial (IMPROVE)
