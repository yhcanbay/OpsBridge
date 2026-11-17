import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
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
