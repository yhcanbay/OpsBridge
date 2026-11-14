import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/ApiError.js'
import ProjectForm from './ProjectForm.jsx'
import { githubConnectionService } from '../../services/githubConnectionService.js'

vi.mock('../../services/githubConnectionService.js', () => ({
    githubConnectionService: {
        getAllConnections: vi.fn(),
        saveConnection: vi.fn(),
        updateConnection: vi.fn(),
        deleteConnection: vi.fn()
    }
}))

beforeEach(() => {
    vi.resetAllMocks()
    githubConnectionService.getAllConnections.mockResolvedValue([])
})

beforeAll(() => {
    // jsdom, tarayıcının dialog açma ve kapatma metotlarını çalıştırmaz.
    HTMLDialogElement.prototype.showModal = function () {
        this.open = true
    }
    HTMLDialogElement.prototype.close = function () {
        this.open = false
    }
})

afterEach(cleanup)

async function fillRequiredFields() {
    await waitFor(() =>
        expect(screen.getByRole('button', { name: 'Yeni bağlantı ekle' }).disabled).toBe(false)
    )
    fireEvent.click(screen.getByRole('button', { name: 'Devam et' }))
    fireEvent.change(screen.getByLabelText(/Proje adı/), { target: { value: 'Yeni proje' } })
    fireEvent.change(screen.getByLabelText(/Repository URL/), {
        target: { value: 'https://example.com/project' }
    })
}

describe('proje profili formu', () => {
    it('alanları, yetenek seçimlerini ve aktif durumunu kaydeder', async () => {
        const onSave = vi.fn().mockResolvedValue({})
        render(<ProjectForm onSave={onSave} onClose={vi.fn()} />)
        await fillRequiredFields()

        fireEvent.click(screen.getByLabelText('Build görüntüleme'))
        fireEvent.click(screen.getByLabelText('HTTP health'))
        fireEvent.click(screen.getByLabelText('Build görüntüleme'))
        fireEvent.click(screen.getByLabelText('Proje aktif'))
        fireEvent.click(screen.getByRole('button', { name: 'Profili kaydet' }))

        await waitFor(() => expect(onSave).toHaveBeenCalledTimes(1))
        expect(onSave).toHaveBeenCalledWith({
            name: 'Yeni proje',
            repositoryUrl: 'https://example.com/project',
            githubConnectionId: null,
            environment: 'local',
            jenkinsBaseUrlRef: '',
            jenkinsJobPath: '',
            containerLabelSelector: '',
            healthUrl: '',
            capabilities: ['HEALTH_VIEW'],
            enabled: false
        })
    })

    it('mevcut profili düzenlerken kimlik ve tarih alanlarını isteğe eklemez', async () => {
        const onSave = vi.fn().mockResolvedValue({})
        const project = {
            id: 'existing-project',
            name: 'Mevcut proje',
            repositoryUrl: 'https://example.com/project',
            environment: 'staging',
            jenkinsBaseUrlRef: null,
            jenkinsJobPath: null,
            containerLabelSelector: null,
            healthUrl: null,
            capabilities: ['BUILD_VIEW'],
            enabled: true,
            createdAt: '2026-11-01T10:00:00Z',
            updatedAt: '2026-11-07T10:00:00Z'
        }
        render(<ProjectForm project={project} onSave={onSave} onClose={vi.fn()} />)

        fireEvent.click(screen.getByRole('button', { name: 'Devam et' }))
        expect(screen.getByLabelText(/Proje adı/).value).toBe('Mevcut proje')
        expect(screen.getByLabelText('Jenkins job yolu').value).toBe('')
        expect(screen.getByLabelText('Build görüntüleme').checked).toBe(true)
        fireEvent.change(screen.getByLabelText(/Proje adı/), { target: { value: 'Yeni ad' } })
        fireEvent.click(screen.getByRole('button', { name: 'Profili kaydet' }))

        await waitFor(() => expect(onSave).toHaveBeenCalledTimes(1))
        const request = onSave.mock.calls[0][0]
        expect(request.name).toBe('Yeni ad')
        expect(request.environment).toBe('staging')
        expect(request.capabilities).toEqual(['BUILD_VIEW'])
        expect(request).not.toHaveProperty('id')
        expect(request).not.toHaveProperty('createdAt')
        expect(request).not.toHaveProperty('updatedAt')
    })

    it('sunucunun alan hatasını gösterir ve girilen veriyi koruyarak yeniden denemeye izin verir', async () => {
        const onSave = vi
            .fn()
            .mockRejectedValue(
                new ApiError(400, 'Proje alanlarını kontrol edin.', { name: 'Bu ad kullanılamaz.' })
            )
        const onClose = vi.fn()
        render(<ProjectForm onSave={onSave} onClose={onClose} />)
        await fillRequiredFields()
        fireEvent.click(screen.getByRole('button', { name: 'Profili kaydet' }))

        expect(await screen.findByRole('alert')).toHaveProperty(
            'textContent',
            'Proje alanlarını kontrol edin.'
        )
        expect(screen.getByText('Bu ad kullanılamaz.')).toBeTruthy()
        expect(screen.getByLabelText(/Proje adı/).value).toBe('Yeni proje')
        expect(screen.getByLabelText(/Proje adı/).getAttribute('aria-invalid')).toBe('true')
        expect(screen.getByRole('button', { name: 'Profili kaydet' }).disabled).toBe(false)
        expect(onClose).not.toHaveBeenCalled()
    })

    it('kayıt sürerken ikinci isteği ve Escape ile kapatmayı engeller', async () => {
        let finishSave
        const onSave = vi.fn().mockImplementation(
            () =>
                new Promise((resolve) => {
                    finishSave = resolve
                })
        )
        const onClose = vi.fn()
        render(<ProjectForm onSave={onSave} onClose={onClose} />)
        await fillRequiredFields()
        fireEvent.click(screen.getByRole('button', { name: 'Profili kaydet' }))

        const dialog = screen.getByRole('dialog')
        fireEvent.submit(dialog.querySelector('form'))
        fireEvent(dialog, new Event('cancel', { cancelable: true }))

        expect(onSave).toHaveBeenCalledTimes(1)
        expect(onClose).not.toHaveBeenCalled()
        expect(screen.getByRole('button', { name: 'Formu kapat' }).disabled).toBe(true)
        expect(screen.getByRole('button', { name: 'Vazgeç' }).disabled).toBe(true)

        await act(async () => finishSave())
        expect(screen.getByRole('button', { name: 'Profili kaydet' }).disabled).toBe(false)
    })
})
