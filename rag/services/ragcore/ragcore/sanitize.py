"""Conservative secret redaction before text leaves the indexer."""
from __future__ import annotations

import re

_ASSIGNMENT = re.compile(
    r"(?im)^(?P<prefix>\s*(?:export\s+)?(?P<quote>[\"']?)"
    r"(?P<key>[A-Za-z_][A-Za-z0-9_.-]*)(?P=quote)\s*[:=]\s*)"
    r"(?P<value>[^\r\n]*)$"
)
_INLINE_JSON = re.compile(
    r"(?P<prefix>[\"'](?P<key>[A-Za-z_][A-Za-z0-9_.-]*)[\"']\s*:\s*)"
    r"(?P<quote>[\"'])(?P<value>.*?)(?P=quote)",
    re.I,
)
_CREDENTIAL_URI = re.compile(r"(?P<scheme>\b[a-z][a-z0-9+.-]*://)[^\s/@:]+:[^\s/@]+@", re.I)

_SECRET_TOKENS = {
    "password",
    "passwd",
    "secret",
    "token",
    "apikey",
    "privatekey",
    "accesskey",
    "clientsecret",
    "authorization",
    "dsn",
}


def _is_secret_key(key: str) -> bool:
    expanded = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", key)
    lowered = expanded.lower()
    tokens = [token for token in re.split(r"[._-]+", lowered) if token]
    collapsed = "".join(tokens)
    return bool(
        _SECRET_TOKENS.intersection(tokens)
        or collapsed in _SECRET_TOKENS
        or ("api" in tokens and "key" in tokens)
        or ("private" in tokens and "key" in tokens)
        or ("access" in tokens and "key" in tokens)
        or ("client" in tokens and "secret" in tokens)
    )


def _redacted_value(value: str) -> str:
    stripped = value.rstrip()
    suffix = value[len(stripped) :]
    comma = "," if stripped.endswith(",") else ""
    if comma:
        stripped = stripped[:-1].rstrip()
    quoted = len(stripped) >= 2 and stripped[0] in "\"'" and stripped[-1] == stripped[0]
    replacement = '"<redacted>"' if quoted else "<redacted>"
    return replacement + comma + suffix


def _replace_assignment(match: re.Match[str]) -> str:
    if not _is_secret_key(match.group("key")):
        return match.group(0)
    return match.group("prefix") + _redacted_value(match.group("value"))


def _replace_inline_json(match: re.Match[str]) -> str:
    if not _is_secret_key(match.group("key")):
        return match.group(0)
    return match.group("prefix") + '"<redacted>"'


def redact_secrets(text: str) -> str:
    """Replace common credential values while preserving line provenance."""
    output: list[str] = []
    block_indent: int | None = None
    continuation = False
    for line in text.splitlines(keepends=True):
        content = line.rstrip("\r\n")
        ending = line[len(content) :]
        indentation = len(content) - len(content.lstrip(" \t"))

        if block_indent is not None:
            if not content.strip():
                output.append(line)
                continue
            if indentation > block_indent:
                output.append(content[:indentation] + "<redacted>" + ending)
                continue
            block_indent = None

        if continuation:
            output.append(content[:indentation] + "<redacted>" + ending)
            continuation = content.rstrip().endswith("\\")
            continue

        match = _ASSIGNMENT.match(content)
        if match is not None and _is_secret_key(match.group("key")):
            value = match.group("value")
            output.append(match.group("prefix") + _redacted_value(value) + ending)
            stripped = value.strip()
            if re.fullmatch(r"[|>][+-]?\d*", stripped):
                block_indent = len(match.group("prefix")) - len(match.group("prefix").lstrip(" \t"))
            elif value.rstrip().endswith("\\"):
                continuation = True
            continue

        output.append(line)

    redacted = "".join(output)
    redacted = _INLINE_JSON.sub(_replace_inline_json, redacted)
    return _CREDENTIAL_URI.sub(
        lambda match: match.group("scheme") + "<redacted>@", redacted
    )
