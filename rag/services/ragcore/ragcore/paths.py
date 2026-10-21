"""Filesystem safety helpers for repository-backed ingestion."""
from __future__ import annotations

import os
import stat

from .classify import normalize


def resolve_repo_file(repo_root: str, relative_path: str) -> tuple[str, str]:
    """Resolve a repo-relative regular path while rejecting every symlink."""
    normalized = normalize(relative_path)
    root = os.path.abspath(repo_root)
    if os.path.islink(root):
        raise ValueError("repository root must not be a symlink")
    current = root
    for component in normalized.split("/"):
        current = os.path.join(current, component)
        try:
            mode = os.lstat(current).st_mode
        except OSError:
            # Preserve the normal file-not-found error at the final open/stat.
            continue
        if stat.S_ISLNK(mode):
            raise ValueError("symlinks are not indexable")
    resolved = os.path.abspath(current)
    if os.path.commonpath((root, resolved)) != root:
        raise ValueError("path escapes repository root")
    return normalized, resolved
