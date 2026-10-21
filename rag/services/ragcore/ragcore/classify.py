"""Deterministic source classification and safe exclusions.

Given a repository-relative path, :func:`classify` returns exactly one
:class:`Classification` (collection + evidence kind + a coarse source type) or
``None`` when the path must not be indexed. The mapping is pure and total: the
same path always yields the same result, with no dependence on filesystem
state, ordering, or wall-clock time.

Exclusions come first and are conservative by design — secrets, player/world
data, jars, model binaries and generated caches are never indexed. Everything
that is not positively recognised as indexable text is excluded, so unknown or
binary content is dropped rather than guessed at.
"""
from __future__ import annotations

import posixpath
import re
from dataclasses import dataclass
from typing import Optional, Tuple

from . import contract


@dataclass(frozen=True)
class Classification:
    collection: str
    evidence_kind: str
    source_type: str


# --- normalisation ----------------------------------------------------------


def normalize(path: str) -> str:
    """Return a safe repository-relative POSIX path.

    Absolute paths, drive-qualified paths and any parent traversal are rejected
    before normalisation.  Silently stripping those prefixes would let webhook
    input escape the read-only repository mount.
    """
    p = path.replace("\\", "/").strip()
    if not p or p.startswith("/") or re.match(r"^[A-Za-z]:", p):
        raise ValueError("path must be repository-relative")
    if ".." in p.split("/"):
        raise ValueError("parent traversal is not allowed")
    p = posixpath.normpath(p)
    if p.startswith("./"):
        p = p[2:]
    if p in ("", ".") or p.startswith("../"):
        raise ValueError("path must name a repository file")
    return p


def _segments(path: str) -> Tuple[str, ...]:
    return tuple(seg.lower() for seg in path.split("/") if seg)


def _name(path: str) -> str:
    return posixpath.basename(path).lower()


def _ext(path: str) -> str:
    name = _name(path)
    dot = name.rfind(".")
    return name[dot:] if dot > 0 else ""


# --- exclusion rules --------------------------------------------------------

# Any path segment equal to one of these is excluded (directories we never
# index: VCS internals, dependency trees, generated data, world/player state).
_EXCLUDED_DIR_SEGMENTS = frozenset(
    {
        ".git",
        ".hg",
        ".svn",
        "node_modules",
        ".venv",
        "venv",
        "__pycache__",
        ".pytest_cache",
        ".mypy_cache",
        "target",
        "build",
        "dist",
        "cache",
        ".cache",
        ".data",
        "world",
        "world_nether",
        "world_the_end",
        "worlds",
        "playerdata",
        "globalplayerdata",
        "userdata",
        "userlimbs",
        "players",
        "playervaults",
        "sessions",
        "stats",
        "advancements",
        "secrets",
        "backups",
        "databasebackups",
    }
)

# Basenames that are always secrets/credentials.
_EXCLUDED_NAMES = frozenset(
    {
        ".env",
        "credentials.json",
        "credentials.yml",
        "credentials.yaml",
        "id_rsa",
        "id_ed25519",
        "ops.json",
        "whitelist.json",
        "banned-players.json",
        "banned-ips.json",
        "usercache.json",
        "bukkit-ops.json",
        "operators.json",
        "auth.json",
        "sessions.json",
    }
)

# Extensions that are binaries, archives, model weights, or opaque data.
_EXCLUDED_EXTS = frozenset(
    {
        # archives / binaries / jars
        ".jar",
        ".zip",
        ".gz",
        ".tar",
        ".tgz",
        ".7z",
        ".rar",
        ".class",
        ".so",
        ".dll",
        ".dylib",
        ".exe",
        ".bin",
        # model weights / opaque ML artifacts
        ".safetensors",
        ".pt",
        ".pth",
        ".onnx",
        ".gguf",
        ".ckpt",
        ".npz",
        ".npy",
        ".bbmodel",
        # images / media
        ".png",
        ".jpg",
        ".jpeg",
        ".gif",
        ".webp",
        ".ico",
        ".mp3",
        ".ogg",
        ".wav",
        ".mp4",
        # minecraft world / player data
        ".dat",
        ".dat_old",
        ".mca",
        ".mcr",
        ".nbt",
        ".mcstructure",
        # secrets material
        ".pem",
        ".key",
        ".p12",
        ".pfx",
        ".keystore",
        ".jks",
        ".crt",
        ".db",
        ".sqlite",
        ".sqlite3",
        ".log",
    }
)

# Substrings in a basename that indicate secrets even without a known ext.
_SECRET_NAME_HINTS = ("secret", "password", "credential", "token", "apikey", "api_key")


def is_excluded(path: str) -> bool:
    """True if ``path`` must never be indexed."""
    try:
        norm = normalize(path)
    except (TypeError, ValueError):
        return True
    segs = _segments(norm)
    if any(seg in _EXCLUDED_DIR_SEGMENTS for seg in segs):
        return True
    if any(
        segment == ".iris" and index + 1 < len(segs) and segs[index + 1] == "schema"
        for index, segment in enumerate(segs)
    ):
        return True
    name = _name(norm)
    if name in _EXCLUDED_NAMES:
        return True
    if _ext(norm) in _EXCLUDED_EXTS:
        return True
    # Raw server logs can contain player names, chat and IP addresses. Only
    # deliberately named reports under a logs directory are eligible.
    if "logs" in segs and name != "logs" and not any(
        marker in name
        for marker in (
            "runtime-report",
            "runtime_report",
            "validation-report",
            "test-report",
            "test_result",
            "benchmark",
        )
    ):
        return True
    # .env.example is a template and safe; bare .env* variants are secrets.
    if name.startswith(".env") and name != ".env.example":
        return True
    if any(hint in name for hint in _SECRET_NAME_HINTS):
        return True
    return False


# --- positive classification rules -----------------------------------------

_DOC_EXTS = frozenset({".md", ".mdx", ".markdown", ".rst", ".adoc", ".txt"})
_CONFIG_EXTS = frozenset(
    {
        ".yml",
        ".yaml",
        ".toml",
        ".json",
        ".properties",
        ".conf",
        ".cfg",
        ".ini",
        ".xml",
        ".env.example",  # matched via name, kept for symmetry
    }
)
_SOURCE_EXTS = frozenset(
    {
        ".py",
        ".js",
        ".mjs",
        ".cjs",
        ".ts",
        ".tsx",
        ".jsx",
        ".java",
        ".kt",
        ".kts",
        ".go",
        ".rs",
        ".rb",
        ".sh",
        ".bash",
        ".sql",
    }
)

# Observed-evidence signals (checked before the expected fallbacks).
_LOG_EXTS = frozenset({".tap"})
_RUNTIME_REPORT_PARENTS = frozenset(
    {"reports", "report", "artifacts", "results", "output", "outputs", "logs"}
)
_RUNTIME_NAME_HINTS = ("validation-report", "runtime-report", "runtime_report")
_BENCH_DIR_HINTS = ("benchmarks", "benchmark", "bot-output", "bot_output", "test-results", "test_results")
_BENCH_NAME_HINTS = ("benchmark", "bench-report", "bot-benchmark", "bot_benchmark", "acceptance-result")
_TEST_ARTIFACT_NAME_HINTS = ("junit", "test-result", "test_result", "test-report", "test_report")


def _looks_observed_runtime(name: str, ext: str, segs: Tuple[str, ...]) -> bool:
    # Source files and durable prose merely discussing "runtime" remain
    # expected evidence. Observed routing is reserved for report-shaped files
    # in deliberate artifact locations or with an explicit report filename.
    if ext not in (_DOC_EXTS | _CONFIG_EXTS | _LOG_EXTS):
        return False
    name_tokens = set(filter(None, re.split(r"[^a-z0-9]+", name)))
    if any(hint in name for hint in _RUNTIME_NAME_HINTS) or {
        "runtime",
        "report",
    }.issubset(name_tokens):
        return True
    return any(
        segment == "runtime" and index > 0 and segs[index - 1] in _RUNTIME_REPORT_PARENTS
        for index, segment in enumerate(segs)
    )


def _looks_observed_benchmark(name: str, segs: Tuple[str, ...]) -> bool:
    if any(seg in _BENCH_DIR_HINTS for seg in segs):
        return True
    if any(hint in name for hint in _BENCH_NAME_HINTS):
        return True
    return any(hint in name for hint in _TEST_ARTIFACT_NAME_HINTS)


def classify(path: str) -> Optional[Classification]:
    """Classify ``path`` into one collection, or ``None`` if excluded.

    Order of decision (first match wins):

    1. exclusions (secrets, binaries, world/player data, caches, VCS internals)
    2. observed benchmarks (bot output, completed test artifacts, bench reports)
    3. observed runtime (deliberate runtime/validation reports; never raw logs)
    4. expected docs (markdown / prose documentation)
    5. expected config (config, manifests, source, test definitions)
    6. otherwise excluded
    """
    if is_excluded(path):
        return None

    try:
        norm = normalize(path)
    except (TypeError, ValueError):
        return None
    name = _name(norm)
    ext = _ext(norm)
    segs = _segments(norm)

    # 2. observed benchmarks — bot outputs and completed test artifacts.
    if _looks_observed_benchmark(name, segs):
        return Classification("observed_benchmarks", contract.EVIDENCE_OBSERVED, "benchmark")

    # 3. observed runtime — deliberate runtime/validation reports.
    if ext in _LOG_EXTS or _looks_observed_runtime(name, ext, segs):
        return Classification("observed_runtime", contract.EVIDENCE_OBSERVED, "runtime")

    # 4. expected docs — markdown / prose.
    if ext in _DOC_EXTS:
        return Classification("expected_docs", contract.EVIDENCE_EXPECTED, "doc")

    # 5. expected config — config, manifests, source, test definitions.
    if name == ".env.example":
        return Classification("expected_config", contract.EVIDENCE_EXPECTED, "config")
    if ext in _CONFIG_EXTS:
        return Classification("expected_config", contract.EVIDENCE_EXPECTED, "config")
    if ext in _SOURCE_EXTS:
        return Classification("expected_config", contract.EVIDENCE_EXPECTED, "source")

    # 6. anything else is not recognised text — do not index.
    return None
