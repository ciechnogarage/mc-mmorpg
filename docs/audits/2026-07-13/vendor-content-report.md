# Vendor Content Report

## Source Boundary

Source: `OUTPUT — kopia (2).zip`  
Status: externally supplied, not tracked in Git, not active runtime content

Reported archive size is approximately 147 MB with 8,066 entries.

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
