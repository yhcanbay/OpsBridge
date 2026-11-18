import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/ApiError.js'
import { githubConnectionService } from '../../services/githubConnectionService.js'
import ProjectForm from './ProjectForm.jsx'

vi.mock('../../services/githubConnectionService.js', () => ({
    githubConnectionService: {
        getAllConnections: vi.fn(),
        saveConnection: vi.fn(),
        updateConnection: vi.fn(),
        deleteConnection: vi.fn()
    }
}))

const savedConnection = { id: 'connection-1', name: 'TaskApp okuma', accountLogin: 'example' }

beforeAll(() => {
    HTMLDialogElement.prototype.showModal = function () {
        this.open = true
    }
    HTMLDialogElement.prototype.close = function () {
        this.open = false
    }
})

beforeEach(() => {
    vi.resetAllMocks()
    githubConnectionService.getAllConnections.mockResolvedValue([savedConnection])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
})

afterEach(() => {
    cleanup()
    vi.restoreAllMocks()
})

async function waitForConnections() {
    await screen.findByRole('option', { name: 'example — TaskApp okuma' })
}

function fillProjectFields() {
    fireEvent.change(screen.getByLabelText(/Proje adı/), { target: { value: 'Test proje' } })
    fireEvent.change(screen.getByLabelText(/Repository URL/), {
        target: { value: 'https://github.com/example/task-app' }
    })
}

describe('GitHub bağlantılı proje formu', () => {
    it('bağlantıyı otomatik seçmez, seçimi gönderir ve adımlar arasında bilgileri korur', async () => {
        const onSave = vi.fn().mockResolvedValue({})
        render(<ProjectForm onSave={onSave} onClose={vi.fn()} />)
        await waitForConnections()
        const select = screen.getByLabelText('Kayıtlı GitHub bağlantıları')
        expect(select.value).toBe('')
        fireEvent.change(select, { target: { value: savedConnection.id } })
        fireEvent.click(screen.getByRole('button', { name: 'Devam et' }))
        fillProjectFields()
        fireEvent.click(screen.getByRole('button', { name: 'GitHub bağlantısına dön' }))
        expect(select.value).toBe(savedConnection.id)
        fireEvent.click(screen.getByRole('button', { name: 'Devam et' }))
        expect(screen.getByLabelText(/Proje adı/).value).toBe('Test proje')
        fireEvent.click(screen.getByRole('button', { name: 'Profili kaydet' }))
        await waitFor(() => expect(onSave).toHaveBeenCalledTimes(1))
        expect(onSave.mock.calls[0][0].githubConnectionId).toBe(savedConnection.id)
        expect(onSave.mock.calls[0][0]).not.toHaveProperty('token')
    })

    it('yeni tokenı yalnız bağlantı servisine gönderir ve kaydedilen bağlantıyı seçer', async () => {
        githubConnectionService.saveConnection.mockResolvedValue({
            id: 'new-connection',
            name: 'Yeni bağlantı',
            accountLogin: 'another'
        })
        render(<ProjectForm onSave={vi.fn()} onClose={vi.fn()} />)
        await waitForConnections()
        fireEvent.click(screen.getByRole('button', { name: 'Yeni bağlantı ekle' }))
        expect(screen.getByRole('button', { name: 'Devam et' }).disabled).toBe(true)
        fireEvent.change(screen.getByLabelText('Bağlantı adı'), {
            target: { value: 'Yeni bağlantı' }
        })
        const tokenInput = screen.getByLabelText('GitHub tokenı')
        expect(tokenInput.type).toBe('password')
        fireEvent.change(tokenInput, { target: { value: 'private-test-token' } })
        fireEvent.click(screen.getByRole('button', { name: 'Bağlantıyı kaydet' }))
        await screen.findByRole('option', { name: 'another — Yeni bağlantı' })
        expect(githubConnectionService.saveConnection).toHaveBeenCalledWith({
            name: 'Yeni bağlantı',
            token: 'private-test-token'
        })
        expect(screen.getByLabelText('Kayıtlı GitHub bağlantıları').value).toBe('new-connection')
        expect(screen.queryByLabelText('GitHub tokenı')).toBeNull()
        expect(document.body.textContent).not.toContain('private-test-token')
    })

    it('token yenilemesi için etkiyi açıklar ve hata olursa seçili bağlantıyı korur', async () => {
        githubConnectionService.updateConnection.mockRejectedValue(
            new ApiError(400, 'Yeni token aynı hesaba ait olmalıdır.')
        )
        render(
            <ProjectForm
                project={{ githubConnectionId: savedConnection.id }}
                onSave={vi.fn()}
                onClose={vi.fn()}
            />
        )
        await waitForConnections()
        expect(screen.getByLabelText('Kayıtlı GitHub bağlantıları').value).toBe(savedConnection.id)
        fireEvent.click(screen.getByRole('button', { name: 'Bağlantıyı düzenle' }))
        fireEvent.change(screen.getByLabelText('Yeni GitHub tokenı'), {
            target: { value: 'new-private-token' }
        })
        fireEvent.click(screen.getByRole('button', { name: 'Bağlantıyı kaydet' }))
        expect(await screen.findByRole('alert')).toHaveProperty(
            'textContent',
            'Yeni token aynı hesaba ait olmalıdır.'
        )
        expect(window.confirm).toHaveBeenCalledWith(expect.stringContaining('bütün projeler'))
        expect(screen.getByLabelText('Kayıtlı GitHub bağlantıları').value).toBe(savedConnection.id)
        expect(screen.getByRole('button', { name: 'Devam et' }).disabled).toBe(true)
        fireEvent.click(screen.getByRole('button', { name: 'Düzenlemeyi iptal et' }))
        fireEvent.click(screen.getByRole('button', { name: 'Bağlantıyı düzenle' }))
        expect(screen.getByLabelText('Yeni GitHub tokenı').value).toBe('')
    })

    it('kullanılan bağlantının silinme hatasını gösterir ve kullanılmayanı silebilir', async () => {
        githubConnectionService.deleteConnection
            .mockRejectedValueOnce(new ApiError(409, 'Bağlantı projelerde kullanılıyor.'))
            .mockResolvedValueOnce()
        render(
            <ProjectForm
                project={{ githubConnectionId: savedConnection.id }}
                onSave={vi.fn()}
                onClose={vi.fn()}
            />
        )
        await waitForConnections()
        fireEvent.click(screen.getByRole('button', { name: 'Bağlantıyı sil' }))
        expect(await screen.findByRole('alert')).toHaveProperty(
            'textContent',
            'Bağlantı projelerde kullanılıyor.'
        )
        expect(screen.getByLabelText('Kayıtlı GitHub bağlantıları').value).toBe(savedConnection.id)
        fireEvent.click(screen.getByRole('button', { name: 'Bağlantıyı sil' }))
        await waitFor(() =>
            expect(screen.getByLabelText('Kayıtlı GitHub bağlantıları').value).toBe('')
        )
        expect(screen.queryByRole('option', { name: 'example — TaskApp okuma' })).toBeNull()
    })

    it('bağlantı kaydı sürerken çift gönderimi ve formun kapanmasını engeller', async () => {
        let resolveSave
        githubConnectionService.saveConnection.mockImplementation(
            () =>
                new Promise((resolve) => {
                    resolveSave = resolve
                })
        )
        const onClose = vi.fn()
        render(<ProjectForm onSave={vi.fn()} onClose={onClose} />)
        await waitForConnections()
        fireEvent.click(screen.getByRole('button', { name: 'Yeni bağlantı ekle' }))
        fireEvent.change(screen.getByLabelText('Bağlantı adı'), { target: { value: 'Test' } })
        fireEvent.change(screen.getByLabelText('GitHub tokenı'), {
            target: { value: 'private-token' }
        })
        fireEvent.click(screen.getByRole('button', { name: 'Bağlantıyı kaydet' }))
        const savingButton = screen.getByRole('button', { name: 'Kaydediliyor…' })
        fireEvent.click(savingButton)
        fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }))
        expect(githubConnectionService.saveConnection).toHaveBeenCalledTimes(1)
        expect(onClose).not.toHaveBeenCalled()
        expect(screen.getByRole('button', { name: 'Formu kapat' }).disabled).toBe(true)
        await act(async () => resolveSave(savedConnection))
        expect(screen.getByRole('button', { name: 'Devam et' }).disabled).toBe(false)
    })

    it('liste yüklenemese bile tokensız devam edilebilir ve liste yeniden istenebilir', async () => {
        githubConnectionService.getAllConnections
            .mockRejectedValueOnce(new ApiError(0, 'Bağlantı kurulamadı.'))
            .mockResolvedValueOnce([savedConnection])
        render(<ProjectForm onSave={vi.fn()} onClose={vi.fn()} />)
        await screen.findByRole('alert')
        expect(screen.getByRole('button', { name: 'Devam et' }).disabled).toBe(false)
        fireEvent.click(screen.getByRole('button', { name: 'Listeyi yenile' }))
        await waitForConnections()
        expect(screen.queryByRole('alert')).toBeNull()
    })

    it('kapanan formun gecikmiş bağlantı listesini yeni forma taşımaz', async () => {
        let resolveOld
        githubConnectionService.getAllConnections.mockImplementationOnce(
            () =>
                new Promise((resolve) => {
                    resolveOld = resolve
                })
        )
        const oldForm = render(<ProjectForm onSave={vi.fn()} onClose={vi.fn()} />)
        const oldSignal = githubConnectionService.getAllConnections.mock.calls[0][0].signal
        oldForm.unmount()
        expect(oldSignal.aborted).toBe(true)
        render(<ProjectForm onSave={vi.fn()} onClose={vi.fn()} />)
        await waitForConnections()
        await act(async () =>
            resolveOld([{ id: 'old', name: 'Eski bağlantı', accountLogin: 'old' }])
        )
        expect(screen.queryByRole('option', { name: 'old — Eski bağlantı' })).toBeNull()
    })
})
