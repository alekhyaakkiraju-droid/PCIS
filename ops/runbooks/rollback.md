# Deployment rollback runbook (WO-139)

**Target execution time:** 15 minutes or less for Path A (no schema change).

**Owner:** Platform / SRE on-call  
**Runbook key:** `rollback-deployment`  
**Related alerts:** `pcis-prod-alerts`, Argo CD `on-sync-failed`, `on-health-degraded`

## Trigger

- Failed production or test deployment (sync error, CrashLoopBackOff, failed probes)
- Deliberate rollback after bad release promotion
- Change-advisory rollback request within the 15-minute RTO window

## Prerequisites

- Argo CD CLI authenticated (`argocd login`) or access to Argo CD UI
- Membership in `pcis-admins` for **prd** sync/rollback actions
- Prior five revisions retained (`revisionHistoryLimit: 5` on all PCIS Applications)
- Known-good Git SHA / image digest documented in the change ticket

## Decision tree: Path A vs Path B

```
Bad release detected
        │
        ▼
Did this release run a Flyway migration?
        │
   No ──┴── Yes
   │         │
   ▼         ▼
Path A    Path B
(Helm/    (Down-migration OR
 GitOps    point-in-time DB restore)
 rollback)
```

**Path A (normal case):** Coexistence migrations are additive-only (expand-then-contract). Roll back the GitOps revision to the prior signed digest — no schema change required.

**Path B (schema change):** A Flyway migration ran. Either apply the paired down-migration script or restore Aurora to a point-in-time snapshot taken before the migration. Document measured recovery time in the post-incident notes.

## Path A — GitOps revision rollback (target ≤ 15 min)

### 1. Identify the failing application

```bash
argocd app list -l pcis.environment=prd
argocd app get pcis-<service>-prd
```

Record: current revision, previous healthy revision from `argocd app history pcis-<service>-prd`.

### 2. Roll back via Argo CD history

```bash
# List retained revisions (max 5)
argocd app history pcis-<service>-prd

# Roll back to revision N (prior known-good)
argocd app rollback pcis-<service>-prd <history-id>
```

Or pin to a known Git SHA:

```bash
argocd app sync pcis-<service>-prd --revision <good-git-sha> \
  --annotation pcis.governance/change-ticket=ROLLBACK-<TICKET>
```

### 3. Batch workloads

```bash
argocd app rollback pcis-batch-prd <history-id>
```

### 4. Verify (within 15 minutes)

```bash
argocd app wait pcis-<service>-prd --health --timeout 600
kubectl -n pcis-prd rollout status deploy/<service>
curl -sf https://<gateway>/actuator/health
```

Checklist:

- [ ] Application status **Synced** and **Healthy**
- [ ] Pod image digest matches the rollback target
- [ ] Smoke test endpoint returns 200
- [ ] No elevated error rate in dashboards
- [ ] Elapsed time from rollback start ≤ 15 minutes (record in ticket)

## Path B — Schema-accompanied rollback

Use when Path A would leave the database schema ahead of application code.

1. **Stop traffic** — scale affected deployment to zero or route traffic away via Istio VirtualService.
2. **Assess migration** — locate Flyway script version in the bad release tag.
3. **Down-migration** (preferred when paired script exists):

   ```bash
   flyway -url=$JDBC_URL -user=$DB_USER -password=$DB_PASS migrate -target=<prior-version>
   ```

4. **Point-in-time restore** (when no down-migration):

   ```bash
   aws rds restore-db-cluster-to-point-in-time \
     --source-db-cluster-identifier pcis-prd-aurora \
     --db-cluster-identifier pcis-prd-aurora-rollback-<timestamp> \
     --restore-to-time <ISO8601-before-migration>
   ```

5. Execute Path A GitOps rollback after database is consistent.
6. Re-enable traffic and run extended verification (may exceed 15-minute target — document actual time).

## Production approval gate

Production rollbacks still require `pcis-admins` approval annotations. Reference `argocd/docs/production-approval-gate.md` for RBAC and change-ticket requirements.

## Post-rollback

1. Create incident / problem ticket with timeline and elapsed recovery time.
2. Freeze further prod syncs until root-cause analysis completes.
3. Update golden rollback digest in the service Helm values if the rolled-back SHA becomes the new baseline.

## References

- ApplicationSets: `argocd/applicationsets/pcis-services.yaml`, `argocd/applicationsets/pcis-batch.yaml`
- Production approval gate: `argocd/docs/production-approval-gate.md`
- Helm charts: `helm/charts/<service>/`
- Flyway migrations: service `db/migration/` directories
