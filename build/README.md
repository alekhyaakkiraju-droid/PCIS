# PCIS Legacy IBM i Build (WO-005)

Dependency-ordered, idempotent build for the coexistence-period COBOL/DDS/CL inventory.

## Quick start

```bash
./build/scripts/build_legacy.sh dev
./build/scripts/build_legacy.sh tst --executor stub
./build/scripts/build_legacy.sh prd --executor real   # requires PCIS_IBMI_SSH
```

Outputs:

- `build/reports/compile-order.txt` — resolved compile order
- `build/reports/build-manifest.json` — per-member results

## Architecture

1. `build_legacy.sh` — thin entrypoint (validates env name)
2. `build_orchestrator.py` — loads `build.yaml`, runs compiler gate, resolves order, compiles
3. `dependency_resolver.py` — topological sort from `manifest/pcis-manifest.yaml` (ILE order: DDS → COBOL → CL)
4. `compiler_gate.py` — asserts Enterprise COBOL for i release is supported
5. `cl_executor.py` — `StubExecutor` (CI/local) or `RealExecutor` (SSH to IBM i)

## Configuration

All library names live in `build/build.yaml` under `environments.{dev,tst,prd}`:

| Key | Purpose |
|-----|---------|
| `pgm_lib` | Program / object library |
| `data_lib` | Data library |
| `shared_lib` | Shared (`INSCOM`) |
| `tools_lib` | Tooling (`INSTOOLS`) |

Do not hard-code library names in scripts.

## Adding members

1. Add the source under `Property_Casualty_Insurance_System/`
2. Regenerate `manifest/pcis-manifest.yaml` (WO-001)
3. Re-run `build_legacy.sh` — compile order is recomputed from the manifest

## Forge Shipping (WOREF-004)

The Shipping pipeline invokes `build/scripts/build_legacy.sh` as the only sanctioned legacy build path, in parallel with modern CI through Phase 5 decommission.

## Tests

```bash
python3 -m unittest discover -s build/tests -v
make validate-data-dictionary
make test-data-dictionary
make test-monetary-precision
```

`validate-data-dictionary` compares `docs/data-dictionary.yaml` against
`shared-libs/pcis-schema/db/migration/V1__baseline_schema.sql` (WO-150).

`test-monetary-precision` runs the WO-152 monetary column precision and
`@Entity` BigDecimal CI gates in `shared-libs/pcis-schema`.
