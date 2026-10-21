'use strict';

// Klient HTTP dla CompanionBridge (Poziom 2, patrz index.js). Cienki
// wrapper nad fetch: token w nagłówku, timeout przez AbortController,
// bez retry — retry/fallback to rola companion-provider.js.

const DEFAULT_TIMEOUT_MS = 2000;

function createCompanionClient({
  baseUrl = process.env.MC_COMPANION_BASE_URL || 'http://127.0.0.1:25580',
  token = process.env.MC_COMPANION_TOKEN || '',
  timeoutMs = DEFAULT_TIMEOUT_MS
} = {}) {
  async function request(path) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const res = await fetch(`${baseUrl}${path}`, {
        headers: { Authorization: `Bearer ${token}` },
        signal: controller.signal
      });
      if (!res.ok) {
        throw new Error(`companion-bridge ${path}: HTTP ${res.status}`);
      }
      return await res.json();
    } finally {
      clearTimeout(timer);
    }
  }

  return {
    health: () => request('/v1/health'),
    capabilities: () => request('/v1/capabilities'),
    performance: () => request('/v1/server/performance'),
    playerSnapshot: uuid => request(`/v1/players/${uuid}/snapshot`)
  };
}

module.exports = { createCompanionClient, DEFAULT_TIMEOUT_MS };
