# Vendor Content Report

## Source Boundary

Source: `OUTPUT — kopia (2).zip`  
Status: externally supplied, not tracked in Git, not active runtime content

Reported archive size is 146,719,792 bytes, approximately 146.72 MB decimal, with 8,066 entries.
SHA-256: `c661a807ad54f53de23752658c2aa840024347470aa713d6d3b12b6e8e7c0ffa`

The checksum is recorded from the supplied external archive. The archive is not
stored in this repository, so the checksum cannot be independently recomputed
from the Git checkout alone. The evidence validator verifies the recorded
checksum format and validates it against a local file only when an external
archive path is supplied.

| Type | Count |
| --- | ---: |
| `.yml` | 542 |
| `.bbmodel` | 542 |
| `.png` | 914 |
| `.ogg` | 2,818 |
| `.json` | 1,835 |

## Static Findings

- 27 duplicate YAML keys in 17 files.
- 2 invalid `sounds.json` files.
- 8 duplicate MythicMobs item IDs.
- 10 duplicate MythicMobs mob IDs.
- 44 duplicate MythicMobs skill IDs.
- 572 `~onTimer:1` occurrences in 113 files.

## Import Decision

Do not copy the archive directly into `MCMMORPG/plugins`. Keep it in quarantine and process it through vendor intake, namespace rewriting, static validation, model/reference validation, isolated spawn testing, profiling, and gameplay review. Only selected authored content may enter the active runtime tree.
