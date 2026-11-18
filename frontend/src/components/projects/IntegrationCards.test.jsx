import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { httpClient } from '../../api/httpClient.js'
import { useIntegrations } from '../../hooks/useIntegrations.js'
import IntegrationCards from './IntegrationCards.jsx'

vi.mock('../../hooks/useIntegrations.js', () => ({ useIntegrations: vi.fn() }))
afterEach(cleanup)

describe('Jenkins bağlantı kartı', () => {
    it('bağlantı doğrulandığında ayrıntı sayfasına bağlantı gösterir', () => {
        useIntegrations.mockReturnValue({
            items: [
                { integration: 'Jenkins', state: 'CONNECTED', message: 'Bağlantı doğrulandı.' }
            ],
            isLoading: false,
            error: ''
        })
        render(
            <MemoryRouter>
                <IntegrationCards project={{ id: 'project-1' }} />
            </MemoryRouter>
        )
        expect(
            screen.getByRole('link', { name: 'Jenkins ayrıntıları ↗' }).getAttribute('href')
        ).toBe('/projects/project-1/jenkins')
    })

    it.each(['NOT_CONNECTED', 'UNREACHABLE', 'JOB_NOT_FOUND', 'ACCESS_DENIED', 'DISABLED'])(
        '%s durumunda Jenkins ayrıntısına geçişe izin vermez',
        (state) => {
            useIntegrations.mockReturnValue({
                items: [
                    { integration: 'Jenkins', state, message: 'URL ve job yolunu kontrol edin.' }
                ],
                isLoading: false,
                error: ''
            })
            render(
                <MemoryRouter>
                    <IntegrationCards project={{ id: 'project-1' }} />
                </MemoryRouter>
            )
            expect(screen.queryByRole('link')).toBeNull()
            expect(screen.getByText('Ayrıntılar için Jenkins bağlantısı gerekli.')).toBeTruthy()
        }
    )
})

it('Docker bağlantısı hazır olduğunda projenin Docker sayfasını açar', () => {
    useIntegrations.mockReturnValue({
        items: [
            {
                integration: 'Docker',
                state: 'CONNECTED',
                message: 'Docker Engine bağlantısı hazır.'
            }
        ],
        isLoading: false,
        error: ''
    })
    render(
        <MemoryRouter>
            <IntegrationCards project={{ id: 'project-1' }} />
        </MemoryRouter>
    )
    expect(screen.getByRole('link', { name: 'Docker ayrıntıları ↗' }).getAttribute('href')).toBe(
        '/projects/project-1/docker'
    )
})

describe('HTTP Health kartı', () => {
    const originalAdapter = httpClient.defaults.adapter

    afterEach(() => {
        httpClient.defaults.adapter = originalAdapter
    })

    it.each([
        [200, 'UP', 'Sağlıklı'],
        [503, 'DOWN', 'Sağlıksız'],
        [503, 'OUT_OF_SERVICE', 'Hizmet dışı']
    ])('HTTP %i ve %s yanıtını kartta gösterir', async (httpStatus, status, label) => {
        useIntegrations.mockReturnValue({
            items: [{ integration: 'HTTP Health', state: 'NOT_CONNECTED' }],
            isLoading: false,
            error: ''
        })
        const adapter = vi.fn(async (config) => {
            return { data: { status }, status: httpStatus, headers: {}, config }
        })
        httpClient.defaults.adapter = adapter

        render(<IntegrationCards project={{ id: 'project-1' }} />)

        expect(await screen.findByText(label)).toBeTruthy()
        expect(screen.getByText(`OpsBridge backend · HTTP ${httpStatus} · ${status}`)).toBeTruthy()
        expect(adapter).toHaveBeenCalledWith(
            expect.objectContaining({ baseURL: '', method: 'get', url: '/actuator/health' })
        )
        expect(adapter.mock.calls[0][0].validateStatus(httpStatus)).toBe(true)
        expect(screen.queryByRole('link')).toBeNull()
    })

    it('proxy hata sayfasını Actuator sağlık durumu olarak göstermez', async () => {
        useIntegrations.mockReturnValue({
            items: [{ integration: 'HTTP Health', state: 'NOT_CONNECTED' }],
            isLoading: false,
            error: ''
        })
        httpClient.defaults.adapter = async (config) => ({
            data: '<html>Service Unavailable</html>',
            status: 503,
            headers: {},
            config
        })

        render(<IntegrationCards project={{ id: 'project-1' }} />)

        expect((await screen.findByRole('alert')).textContent).toBe(
            'Actuator sağlık yanıtı okunamadı.'
        )
    })
})
