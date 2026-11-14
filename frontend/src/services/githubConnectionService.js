import { httpClient } from '../api/httpClient.js'

async function getAllConnections({ signal } = {}) {
    const response = await httpClient.get('/github-connections', { signal })
    return response.data
}

async function saveConnection(request) {
    const response = await httpClient.post('/github-connections', request, { timeout: 20_000 })
    return response.data
}

async function updateConnection(connectionId, request) {
    const response = await httpClient.put(`/github-connections/${connectionId}`, request, {
        timeout: 20_000
    })
    return response.data
}

async function deleteConnection(connectionId) {
    await httpClient.delete(`/github-connections/${connectionId}`)
}

export const githubConnectionService = {
    getAllConnections,
    saveConnection,
    updateConnection,
    deleteConnection
}
