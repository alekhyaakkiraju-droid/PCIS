# PCIS Intent Document

## Vision
Modernize the PCIS IBM i application to a cloud-native microservices platform
using Java 21, Spring Boot 3.5, Aurora PostgreSQL, and React 19.

## Core Features
1. Six domain microservices with versioned REST APIs
2. Shared kernel: authorization service (deny-by-default)
3. Shared kernel: audit logging with transactional outbox
4. Spring Batch 5.x for all batch workloads
5. React 19 SPA replacing 22 DDS 5250 panels

## Constraints
- Phased delivery with parallel run gates
- Cent-level financial parity required before cutover
- IBM i must remain buildable until Phase 5
