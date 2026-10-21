from __future__ import annotations

import re
import subprocess
import sys
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ASSET_ID = "mcmmo_elite_crypt_warden"
EVIDENCE_PATH = Path("qa/evidence/mcmmo_elite_crypt_warden/blockbench_validation.json")


REQUIRED_FILES = [
    Path("assets/blockbench/source/mcmmo_elite_crypt_warden/mcmmo_elite_crypt_warden.bbmodel"),
    Path("assets/blockbench/source/mcmmo_elite_crypt_warden/textures/mcmmo_elite_crypt_warden.png"),
    Path("assets/blockbench/source/mcmmo_elite_crypt_warden/README.md"),
    Path("docs/assets/reviews/mcmmo_elite_crypt_warden_review.md"),
    Path("assets/modelengine/mcmmo_elite_crypt_warden/README.md"),
    Path("assets/modelengine/mcmmo_elite_crypt_warden/model_manifest.yml"),
    Path("assets/modelengine/mcmmo_elite_crypt_warden/animation_mapping.yml"),
    Path("assets/mythicmobs/mcmmo_elite_crypt_warden/mobs/mcmmo_elite_crypt_warden.yml"),
    Path("assets/mythicmobs/mcmmo_elite_crypt_warden/skills/mcmmo_elite_crypt_warden_skills.yml"),
    Path("assets/nexo/mcmmo_elite_crypt_warden/NOTES.md"),
    Path("assets/nexo/mcmmo_elite_crypt_warden/items/mcmmo_elite_crypt_shard.yml"),
    Path("qa/mcmmo_elite_crypt_warden_staging_smoke.md"),
]


def rel(path: Path) -> str:
    return path.as_posix()


def check_git_ignore(path: Path) -> tuple[bool, str]:
    cmd = [
        "git",
        "-c",
        f"safe.directory={ROOT.as_posix()}",
        "check-ignore",
        "-v",
        rel(path),
    ]
    proc = subprocess.run(cmd, cwd=ROOT, text=True, capture_output=True)
    if proc.returncode == 0:
        detail = proc.stdout.strip()
        # `git check-ignore -v` also reports negated allowlist rules. The PNG is
        # acceptable when the matching pattern is an explicit `!path` exception.
        parts = detail.split("\t", 1)
        rule = parts[0] if parts else detail
        if re.search(r":!", rule):
            return True, detail
        return False, detail
    if proc.returncode == 1:
        return True, ""
    return False, (proc.stderr or proc.stdout).strip()


def main() -> int:
    errors: list[str] = []
    blocked: list[str] = []

    for path in REQUIRED_FILES:
        full_path = ROOT / path
        if not full_path.exists():
            errors.append(f"missing required file: {rel(path)}")
        elif full_path.is_file() and full_path.stat().st_size == 0:
            errors.append(f"empty required file: {rel(path)}")

    review_path = ROOT / "docs/assets/reviews/mcmmo_elite_crypt_warden_review.md"
    readme_path = ROOT / "assets/blockbench/source/mcmmo_elite_crypt_warden/README.md"
    for path in [review_path, readme_path]:
        if path.exists():
            text = path.read_text(encoding="utf-8")
            if re.search(r"(?im)^\s*(status|decision)\s*:\s*approved\s*$", text):
                errors.append(f"approved status is forbidden before staging: {path.relative_to(ROOT).as_posix()}")

    png_path = Path("assets/blockbench/source/mcmmo_elite_crypt_warden/textures/mcmmo_elite_crypt_warden.png")
    ok, detail = check_git_ignore(png_path)
    if not ok:
        errors.append(f"PNG is ignored by git or git check failed: {detail}")

    evidence_full_path = ROOT / EVIDENCE_PATH
    if not evidence_full_path.exists():
        blocked.append(f"missing Blockbench evidence file: {rel(EVIDENCE_PATH)}")
    else:
        try:
            evidence = json.loads(evidence_full_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            errors.append(f"invalid Blockbench evidence JSON: {exc}")
            evidence = {}

        if evidence:
            result = str(evidence.get("validation_result", "")).upper()
            if result == "FAIL":
                errors.append("Blockbench evidence result is FAIL")
            elif result == "BLOCKED":
                blocked.append(str(evidence.get("blocker", "Blockbench evidence result is BLOCKED")))
            elif result != "PASS":
                blocked.append("Blockbench evidence validation_result must be PASS, FAIL, or BLOCKED")

            required_truthy = [
                "blockbench_executable_found",
                "opened_in_blockbench",
                "visible_model_confirmed",
                "animations_panel_confirmed",
            ]
            for key in required_truthy:
                value = evidence.get(key)
                if result == "PASS" and value is not True:
                    errors.append(f"PASS evidence requires {key}=true")
                elif value == "unknown":
                    blocked.append(f"Blockbench evidence has unknown value for {key}")

            if result == "PASS":
                if not evidence.get("screenshot_path") and not evidence.get("export_log_path"):
                    errors.append("PASS evidence requires screenshot_path or export_log_path")
                visible = evidence.get("visible_animations", [])
                for animation in ["idle", "walk", "attack_01", "death"]:
                    if animation not in visible:
                        errors.append(f"PASS evidence missing visible animation: {animation}")

    mob_text_path = ROOT / "assets/mythicmobs/mcmmo_elite_crypt_warden/mobs/mcmmo_elite_crypt_warden.yml"
    if mob_text_path.exists():
        mob_text = mob_text_path.read_text(encoding="utf-8")
        for expected in ["MCMMOEliteCryptWarden", "mcmmo_elite_crypt_warden", "BossBar:", "MCMMOEliteCryptWardenDeath"]:
            if expected not in mob_text:
                errors.append(f"mob file missing expected marker: {expected}")

    skill_text_path = ROOT / "assets/mythicmobs/mcmmo_elite_crypt_warden/skills/mcmmo_elite_crypt_warden_skills.yml"
    if skill_text_path.exists():
        skill_text = skill_text_path.read_text(encoding="utf-8")
        for expected in [
            "MCMMOEliteCryptWardenSpawn",
            "MCMMOEliteCryptWardenAttack01",
            "MCMMOEliteCryptWardenAttack02",
            "MCMMOEliteCryptWardenVoidCast",
            "MCMMOEliteCryptWardenSpecial01",
            "MCMMOEliteCryptWardenDeath",
        ]:
            if expected not in skill_text:
                errors.append(f"skill file missing expected skill: {expected}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1

    if blocked:
        for item in blocked:
            print(f"BLOCKED: {item}")
        print("MCMMO_ELITE_CRYPT_WARDEN_PACKET_BLOCKED_BLOCKBENCH_EVIDENCE_REQUIRED")
        return 1

    print("MCMMO_ELITE_CRYPT_WARDEN_PACKET_OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
