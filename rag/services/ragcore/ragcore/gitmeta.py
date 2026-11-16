"""Git snapshot inspection: exact commit SHA and clean/dirty working tree.

The indexer stamps every point with the snapshot it was read from. That
snapshot is ``(commit_sha, worktree_state)``:

* ``commit_sha`` — the full 40-char HEAD commit,
* ``worktree_state`` — ``"clean"`` when the working tree matches the index and
  HEAD, ``"dirty"`` otherwise, ``"unknown"`` when it cannot be determined.

:class:`SubprocessGitInspector` shells out to ``git`` (read-only; it never takes
locks) and falls back to parsing ``.git`` plumbing when the binary is absent.
:class:`StaticGitInspector` returns fixed values and is what the tests use, so
none of the indexing logic depends on a real repository or the ``git`` binary.
"""
from __future__ import annotations

import os
import subprocess
from typing import Protocol

from . import contract


class GitInspector(Protocol):
    def commit_sha(self) -> str:
        ...

    def worktree_state(self) -> str:
        ...


class StaticGitInspector:
    """Deterministic inspector for tests and fixtures."""

    def __init__(self, commit_sha: str, worktree_state: str = contract.WORKTREE_CLEAN) -> None:
        self._sha = commit_sha
        self._state = worktree_state

    def commit_sha(self) -> str:
        return self._sha

    def worktree_state(self) -> str:
        return self._state


class SubprocessGitInspector:
    """Reads the snapshot from a real git repository, read-only."""

    def __init__(self, repo_path: str) -> None:
        self.repo_path = repo_path

    # -- git binary path (preferred) ----------------------------------------

    def _git(self, *args: str) -> str:
        env = dict(os.environ)
        # Never take index/ref locks — the repo may be mounted read-only.
        env["GIT_OPTIONAL_LOCKS"] = "0"
        out = subprocess.run(
            ["git", "-C", self.repo_path, *args],
            capture_output=True,
            text=True,
            env=env,
            timeout=30,
            check=True,
        )
        return out.stdout

    def commit_sha(self) -> str:
        try:
            return self._git("rev-parse", "HEAD").strip()
        except Exception:
            return self._commit_sha_from_plumbing()

    def worktree_state(self) -> str:
        try:
            porcelain = self._git("status", "--porcelain")
        except Exception:
            return contract.WORKTREE_UNKNOWN
        return contract.WORKTREE_DIRTY if porcelain.strip() else contract.WORKTREE_CLEAN

    # -- plumbing fallback (no git binary) ----------------------------------

    def _commit_sha_from_plumbing(self) -> str:
        git_dir = os.path.join(self.repo_path, ".git")
        try:
            head = _read(os.path.join(git_dir, "HEAD")).strip()
        except OSError:
            return "unknown"
        if head.startswith("ref:"):
            ref = head[4:].strip()
            ref_path = os.path.join(git_dir, ref)
            try:
                return _read(ref_path).strip()
            except OSError:
                return self._sha_from_packed_refs(git_dir, ref)
        return head  # detached HEAD stores the SHA directly

    @staticmethod
    def _sha_from_packed_refs(git_dir: str, ref: str) -> str:
        try:
            packed = _read(os.path.join(git_dir, "packed-refs"))
        except OSError:
            return "unknown"
        for line in packed.splitlines():
            if line and not line.startswith(("#", "^")):
                sha, _, name = line.partition(" ")
                if name.strip() == ref:
                    return sha.strip()
        return "unknown"


def _read(path: str) -> str:
    with open(path, "r", encoding="utf-8") as handle:
        return handle.read()
