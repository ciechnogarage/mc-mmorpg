import { batchFail } from './errors.mjs';

const HOST = /^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])$/;

export function validateModrinthSearchPolicy(policy) {
  if (!policy || typeof policy !== 'object' || Array.isArray(policy)
      || policy.adapter !== 'modrinth-search-v2'
      || !Number.isInteger(policy.max_results) || policy.max_results < 1 || policy.max_results > 20) {
    batchFail('batch-search-policy-malformed', 'Modrinth search policy must use the bounded v2 adapter');
  }
  const search = endpoint(policy.search, 'search');
  const project = endpoint(policy.project, 'project');
  if (search.host !== 'api.modrinth.com' || search.path_prefix !== '/v2/search'
      || project.host !== 'api.modrinth.com' || project.path_prefix !== '/v2/project/') {
    batchFail('batch-search-policy-malformed', 'Modrinth API hosts and operation paths must be pinned');
  }
  if (!policy.artifact || typeof policy.artifact !== 'object'
      || policy.artifact.host !== 'cdn.modrinth.com'
      || !Number.isInteger(policy.artifact.max_bytes)
      || policy.artifact.max_bytes < 1 || policy.artifact.max_bytes > 128 * 1024 * 1024) {
    batchFail('batch-search-policy-malformed', 'Modrinth artifact host and byte limit must be pinned');
  }
  if (!policy.redirects || policy.redirects.max !== 0 || policy.redirects.same_host_only !== true) {
    batchFail('batch-search-policy-malformed', 'Modrinth search policy forbids redirects');
  }
  return Object.freeze({
    adapter: policy.adapter,
    max_results: policy.max_results,
    search,
    project,
    artifact: Object.freeze({ ...policy.artifact }),
    redirects: Object.freeze({ ...policy.redirects }),
  });
}

function endpoint(value, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)
      || typeof value.host !== 'string' || !HOST.test(value.host) || value.host !== value.host.toLowerCase()
      || typeof value.project_path !== 'string' || !value.project_path.startsWith('/')
      || typeof value.path_prefix !== 'string' || !value.path_prefix.startsWith(`${value.project_path}/`)
      || value.path_prefix.includes('..') || value.path_prefix.includes('\\')
      || !Number.isInteger(value.max_bytes) || value.max_bytes < 1 || value.max_bytes > 16 * 1024 * 1024) {
    batchFail('batch-search-policy-malformed', `${label} endpoint is not safely pinned`);
  }
  return Object.freeze({ ...value });
}
