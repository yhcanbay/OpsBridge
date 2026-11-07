import { httpClient } from '../api/httpClient.js'

async function getAllProjects({ signal } = {}) {
    const response = await httpClient.get('/projects', { signal })
    return response.data
}

async function saveProject(request) {
    const response = await httpClient.post('/projects', request)
    return response.data
}

async function updateProject(projectId, request) {
    const response = await httpClient.put(`/projects/${projectId}`, request)
    return response.data
}

async function deleteProject(projectId) {
    await httpClient.delete(`/projects/${projectId}`)
}

async function getIntegrationStatuses(projectId, { signal } = {}) {
    const response = await httpClient.get(`/projects/${projectId}/integrations`, { signal })
    return response.data
}

export const projectService = {
    getAllProjects,
    saveProject,
    updateProject,
    deleteProject,
    getIntegrationStatuses
}
