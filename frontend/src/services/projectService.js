import { httpClient } from '../api/httpClient.js'

async function getAllProjects({ signal } = {}) {
    const response = await httpClient.get('/projects', { signal })
    return response.data
}

async function saveProject(request) {
    const response = await httpClient.post('/projects', request, { timeout: 20_000 })
    return response.data
}

async function updateProject(projectId, request) {
    const response = await httpClient.put(`/projects/${projectId}`, request, { timeout: 20_000 })
    return response.data
}

async function deleteProject(projectId) {
    await httpClient.delete(`/projects/${projectId}`)
}

async function getIntegrationStatuses(projectId, { signal } = {}) {
    const response = await httpClient.get(`/projects/${projectId}/integrations`, { signal })
    return response.data
}

async function getGitHistory(projectId, { branch, page, branchPage }, { signal } = {}) {
    const response = await httpClient.get(`/projects/${projectId}/git`, {
        params: { branch, page, branchPage },
        signal,
        // Backend, branch listesi ve commit geçmişi için iki ayrı GitHub isteği yapar.
        timeout: 35_000
    })
    return response.data
}

export const projectService = {
    getAllProjects,
    saveProject,
    updateProject,
    deleteProject,
    getIntegrationStatuses,
    getGitHistory
}
