"""Deterministic chunking with line and heading provenance.

:func:`chunk_text` splits a source file into chunks with stable boundaries and
exact 1-based ``line_start``/``line_end`` ranges. For Markdown, a heading line
starts a new chunk and the heading text is carried on every chunk beneath it,
so a citation can point at both the line range and the section it came from.

The algorithm depends only on the input text and the (size) parameters — never
on wall-clock time or filesystem state — so re-chunking identical content
produces byte-identical chunks and, via :func:`ragcore.contract.point_id`,
identical point identities.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import List, Optional

from . import contract

_HEADING_RE = re.compile(r"^(#{1,6})\s+(.*\S)\s*$")
_MARKDOWN_EXTS = (".md", ".mdx", ".markdown")

DEFAULT_MAX_LINES = 80
DEFAULT_MAX_CHARS = 2000


@dataclass(frozen=True)
class Chunk:
    text: str
    heading: Optional[str]
    line_start: int
    line_end: int
    chunk_index: int

    @property
    def content_hash(self) -> str:
        return contract.content_hash(self.text)


def _is_markdown(path: str) -> bool:
    return path.lower().endswith(_MARKDOWN_EXTS)


def chunk_text(
    text: str,
    path: str,
    *,
    max_lines: int = DEFAULT_MAX_LINES,
    max_chars: int = DEFAULT_MAX_CHARS,
) -> List[Chunk]:
    """Split ``text`` into deterministic chunks with line/heading provenance.

    A chunk is flushed when it reaches ``max_lines`` or ``max_chars``, and — for
    Markdown — whenever a new heading begins. Empty / whitespace-only chunks are
    dropped. ``chunk_index`` is a dense 0-based counter over the emitted chunks.
    """
    if max_lines < 1:
        raise ValueError("max_lines must be >= 1")
    if max_chars < 1:
        raise ValueError("max_chars must be >= 1")

    lines = text.split("\n")
    is_md = _is_markdown(path)

    chunks: List[Chunk] = []
    buf: List[str] = []
    buf_start = 1
    heading_now: Optional[str] = None
    chunk_heading: Optional[str] = None

    def flush(end_line: int) -> None:
        nonlocal buf
        if buf and "\n".join(buf).strip():
            chunks.append(
                Chunk(
                    text="\n".join(buf),
                    heading=chunk_heading,
                    line_start=buf_start,
                    line_end=end_line,
                    chunk_index=len(chunks),
                )
            )
        buf = []

    for lineno, line in enumerate(lines, start=1):
        match = _HEADING_RE.match(line) if is_md else None

        # A heading always starts a fresh chunk (unless the buffer is empty).
        if match and buf:
            flush(lineno - 1)

        if not buf:
            buf_start = lineno
            chunk_heading = match.group(2).strip() if match else heading_now

        buf.append(line)

        if match:
            heading_now = match.group(2).strip()
            chunk_heading = heading_now

        over_lines = len(buf) >= max_lines
        over_chars = sum(len(x) + 1 for x in buf) >= max_chars
        if over_lines or over_chars:
            flush(lineno)

    flush(len(lines))
    return chunks
