import { AxiosError } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './ApiError.js'
import { AUTH_EXPIRED_EVENT, httpClient, resetCsrfToken } from './httpClient.js'
import { authService } from '../services/authService.js'
import { projectService } from '../services/projectService.js'

function response(config, data, status = 200) {
    return { config, data, status, statusText: String(status), headers: {} }
}

const originalAdapter = httpClient.defaults.adapter

beforeEach(() => {
    resetCsrfToken()
})

afterEach(() => {
    httpClient.defaults.adapter = originalAdapter
    resetCsrfToken()
})

describe('Axios session and CSRF protocol', () => {
    it('shares a CSRF request across simultaneous mutations and supplies its header', async () => {
        const requests = []
        httpClient.defaults.adapter = async (config) => {
            requests.push(config)
            if (config.url === '/auth/csrf') {
                return response(config, { headerName: 'X-CSRF-TOKEN', token: 'csrf-before-login' })
            }
            return response(config, { id: 'project-id' }, 201)
        }

        await Promise.all([
            projectService.saveProject({ name: 'First' }),
            projectService.saveProject({ name: 'Second' })
        ])

        const csrfRequests = requests.filter((request) => request.url === '/auth/csrf')
        const mutations = requests.filter((request) => request.method === 'post')
        expect(csrfRequests).toHaveLength(1)
        expect(mutations).toHaveLength(2)
        expect(
            mutations.every(
                (request) => request.headers.get('X-CSRF-TOKEN') === 'csrf-before-login'
            )
        ).toBe(true)
    })

    it('refreshes the token after login before the next write', async () => {
        let tokenRequests = 0
        const mutationTokens = []
        httpClient.defaults.adapter = async (config) => {
            if (config.url === '/auth/csrf') {
                tokenRequests += 1
                return response(config, {
                    headerName: 'X-CSRF-TOKEN',
                    token: `csrf-${tokenRequests}`
                })
            }
            if (config.url === '/auth/me') {
                return response(config, { username: 'admin', role: 'ADMIN' })
            }
            mutationTokens.push(config.headers.get('X-CSRF-TOKEN'))
            return response(config, null, 204)
        }

        await authService.login({ username: 'admin', password: 'test-only' })
        await projectService.saveProject({ name: 'AfterLogin' })

        expect(mutationTokens).toEqual(['csrf-1', 'csrf-2'])
        expect(tokenRequests).toBe(2)
    })

    it('expires the session on 401 without retrying a mutation', async () => {
        const expired = vi.fn()
        window.addEventListener(AUTH_EXPIRED_EVENT, expired)
        let writes = 0

        httpClient.defaults.adapter = async (config) => {
            if (config.url === '/auth/csrf') {
                return response(config, { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' })
            }
            writes += 1
            throw new AxiosError(
                'Unauthorized',
                'ERR_BAD_REQUEST',
                config,
                null,
                response(config, {}, 401)
            )
        }

        try {
            await expect(projectService.saveProject({ name: 'Expired' })).rejects.toMatchObject({
                status: 401
            })
            expect(expired).toHaveBeenCalledTimes(1)
            expect(writes).toBe(1)
        } finally {
            window.removeEventListener(AUTH_EXPIRED_EVENT, expired)
        }
    })

    it('keeps API field errors available to forms', async () => {
        httpClient.defaults.adapter = async (config) => {
            if (config.url === '/auth/csrf') {
                return response(config, { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' })
            }
            const problem = {
                detail: 'Proje alanlarını kontrol edin.',
                fields: { name: 'Boş olamaz.' }
            }
            throw new AxiosError(
                'Bad Request',
                'ERR_BAD_REQUEST',
                config,
                null,
                response(config, problem, 400)
            )
        }

        await expect(projectService.saveProject({ name: '' })).rejects.toBeInstanceOf(ApiError)
        await expect(projectService.saveProject({ name: '' })).rejects.toMatchObject({
            fields: { name: 'Boş olamaz.' }
        })
    })
})
