# Runtime Startup Report

## Evidence

- `MCMMORPG/_validation/_server_boot.log` contains startup warnings, including MCPets fallback behavior and a DeluxeMenus NMS-hook warning.
- `MCMMORPG/logs/latest.log` contains a failed port bind and an unexpected exception.

## Status

`PARTIALLY_VERIFIED`.

These logs prove that runtime probes and warning/error evidence exist. They do not prove a clean release startup because the current `latest.log` includes a failed bind attempt. A fresh, isolated staging boot with classified warnings is required before release approval.
