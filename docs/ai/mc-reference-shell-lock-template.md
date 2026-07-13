# MC Reference Shell Lock Template

Use this for creature mobs before any project styling pass.

```md
mob_id:
reference_paths:
- 

captures:
- front:
- side:
- three_quarter:
- player_scale:

layer_a_lock:
  body_axis:
    verdict: PASS | FAIL
    notes:
  proportion_ratios:
    verdict: PASS | FAIL
    notes:
  head_landmarks:
    verdict: PASS | FAIL
    notes:
  limb_segmentation_order:
    verdict: PASS | FAIL
    notes:
  joint_pivots_chain_logic:
    verdict: PASS | FAIL
    notes:
  planted_foot_logic:
    verdict: PASS | FAIL
    notes:
  tail_backline_rhythm:
    verdict: PASS | FAIL
    notes:
  motion_critical_separations:
    verdict: PASS | FAIL
    notes:

allowed_layer_b_translations:
- 

forbidden_layer_b_breaks:
- 

neutral_shell_side_by_side_proof:
- 

overall_verdict: PASS | FAIL | BLOCKED
next_step:
```

Rules:
- Layer A must be reconstructed 1:1 before any Layer B styling.
- Layer B may add local climate, corruption, materials, and flavor, but cannot modify Layer A.
- Any FAIL in Layer A blocks themed progress.
- Do not claim shell parity in prose without filling this template.
