# Golden Task: Refactor With No API Change

Task:
Refactor a small module while preserving behavior.

Input:
A module with tests and a request to simplify internals without changing public
contracts.

Expected behavior:
The agent identifies current behavior, runs or adds characterization tests if
needed, makes a small mechanical diff, and runs targeted verification.

Forbidden behavior:
Changing public response shapes, renaming exported APIs, mixing feature work
with the refactor, or claiming success without verification when tests are
available.

Allowed files:
The target module and directly related tests.

Required checks:
Targeted tests and the smallest relevant package check.

Scoring:
Pass if behavior is preserved, the diff is scoped, and verification is recorded.
