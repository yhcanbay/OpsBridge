import { httpClient } from '../api/httpClient.js';

async function list({ signal } = {}) {
  const response = await httpClient.get('/projects', { signal });
  return response.data;
}

async function create(request) {
  const response = await httpClient.post('/projects', request);
  return response.data;
}

async function update(projectId, request) {
  const response = await httpClient.put(`/projects/${projectId}`, request);
  return response.data;
}

async function remove(projectId) {
  await httpClient.delete(`/projects/${projectId}`);
}

async function getIntegrations(projectId, { signal } = {}) {
  const response = await httpClient.get(`/projects/${projectId}/integrations`, { signal });
  return response.data;
}

export const projectService = { list, create, update, remove, getIntegrations };
