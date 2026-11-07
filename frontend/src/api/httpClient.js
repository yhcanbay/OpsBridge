import axios from 'axios'
import { ApiError } from './ApiError.js'

export const AUTH_EXPIRED_EVENT = 'opsbridge:unauthorized'

export const httpClient = axios.create({
    baseURL: '/api',
    timeout: 10_000,
    withCredentials: true,
    headers: { Accept: 'application/json' }
})

let csrfTokenPromise = null

export function resetCsrfToken() {
    csrfTokenPromise = null
}

export async function loadCsrfToken() {
    if (csrfTokenPromise === null) {
        csrfTokenPromise = httpClient.get('/auth/csrf').then((response) => response.data)
    }

    try {
        return await csrfTokenPromise
    } catch (error) {
        resetCsrfToken()
        throw error
    }
}

export function isRequestCancelled(error) {
    return axios.isCancel(error)
}

function toApiError(error) {
    const status = error.response?.status ?? 0
    const problem = error.response?.data ?? {}

    const messages = {
        0: 'API bağlantısı kurulamadı. Backend uygulamasının çalıştığını kontrol edin.',
        401: 'Oturum bulunamadı veya kullanıcı adı/parola hatalı.',
        403: 'İstek doğrulanamadı. Oturumunuzu yenileyip tekrar deneyin.',
        404: 'Proje bulunamadı. Listeyi yenileyin.'
    }

    let message = messages[status] ?? problem.detail ?? 'İşlem tamamlanamadı.'
    if (error.code === 'ECONNABORTED') {
        message = 'API isteği zaman aşımına uğradı. Bağlantıyı kontrol edip tekrar deneyin.'
    }

    return new ApiError(status, message, problem.fields)
}

httpClient.interceptors.request.use(async (config) => {
    const method = config.method?.toUpperCase() ?? 'GET'
    const safeMethod = ['GET', 'HEAD', 'OPTIONS'].includes(method)

    if (!safeMethod) {
        const csrfToken = await loadCsrfToken()
        config.headers.set(csrfToken.headerName, csrfToken.token)
    }

    return config
})

httpClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (isRequestCancelled(error) || error instanceof ApiError) {
            return Promise.reject(error)
        }

        const apiError = toApiError(error)
        if (apiError.status === 401 || apiError.status === 403) {
            resetCsrfToken()
        }

        const loginAttempt = error.config?.url === '/auth/login'
        if (apiError.status === 401 && !loginAttempt) {
            window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
        }

        // Başarısız mutation otomatik tekrarlanmaz; işlem sunucuya ulaşmış olabilir.
        return Promise.reject(apiError)
    }
)
