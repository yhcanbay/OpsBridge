import { httpClient } from '../api/httpClient.js'

async function getAllProjects({ signal } = {}) {
    const response = await httpClient.get('/projects', { signal })
    return response.data
}

async function getProjectById(projectId, { signal } = {}) {
    const response = await httpClient.get(`/projects/${projectId}`, { signal })
    return response.data
}

async function getJenkinsJobInfo(projectId, build, { signal } = {}) {
    const response = await httpClient.get(`/projects/${projectId}/jenkins`, {
        params: { build },
        signal,
        timeout: 25_000
    })
    return response.data
}

async function saveProject(request) {
    const response = await httpClient.post('/projects', request, { timeout: 20_000 })
    return response.data
}

async function getDockerContainers(projectId, { signal } = {}) {
    const response = await httpClient.get(`/projects/${projectId}/docker/containers`, {
        signal,
        timeout: 25_000
    })
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
    const response = await httpClient.get(`/projects/${projectId}/integrations`, {
        signal,
        timeout: 15_000
    })
    return response.data
}

async function getHttpHealth({ signal } = {}) {
    const response = await httpClient.get('/actuator/health', {
        baseURL: '',
        signal,
        // Actuator, DOWN ve OUT_OF_SERVICE durumlarını HTTP 503 ile döndürür.
        validateStatus: () => true
    })
    const { data, status: httpStatus } = response

    if (httpStatus !== 200 && httpStatus !== 503) {
        throw new Error(`Sağlık kontrolü HTTP ${httpStatus} yanıtı verdi.`)
    }
    if (typeof data?.status !== 'string' || !data.status.trim()) {
        throw new Error('Actuator sağlık yanıtı okunamadı.')
    }

    return { status: data.status, httpStatus }
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
    getProjectById,
    getJenkinsJobInfo,
    getDockerContainers,
    saveProject,
    updateProject,
    deleteProject,
    getIntegrationStatuses,
    getHttpHealth,
    getGitHistory
}
