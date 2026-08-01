# Harness L4 audit

Run a deterministic harness maturity scan and fix gaps until L4.

1. Run `npx harness-score` at the repo root.
2. If below L4, address failed checks in order: CTX → SKL/AGT → HKS → CI-04 → HYG.
3. Re-run `npx harness-score --min-level 4`.
4. Summarize score, level, and remaining gaps for the user.
