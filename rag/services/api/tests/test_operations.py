from __future__ import annotations

import pytest
from pydantic import ValidationError

from api.app.main import EventRequest
from ragcore.events import EVENT_TYPES


@pytest.mark.parametrize("event_type", EVENT_TYPES)
def test_event_request_accepts_exact_supported_event_types(event_type):
    paths = [] if event_type == "commit" else ["docs/example.md"]
    assert EventRequest(event_type=event_type, paths=paths).event_type == event_type


def test_event_request_rejects_unknown_event_type():
    with pytest.raises(ValidationError):
        EventRequest(event_type="delete_everything", paths=[])


def test_event_request_requires_paths_except_for_commit():
    with pytest.raises(ValidationError):
        EventRequest(event_type="runtime_report", paths=[])
    assert EventRequest(event_type="commit", paths=[]).paths == []


def test_event_request_caps_path_count_and_path_length():
    with pytest.raises(ValidationError):
        EventRequest(event_type="documentation", paths=["docs/a.md"] * 101)
    with pytest.raises(ValidationError):
        EventRequest(event_type="documentation", paths=["x" * 4097])
