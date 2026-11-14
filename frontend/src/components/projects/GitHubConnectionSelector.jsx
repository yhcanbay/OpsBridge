import { useEffect, useRef, useState } from 'react'
import { isRequestCancelled } from '../../api/httpClient.js'
import { githubConnectionService } from '../../services/githubConnectionService.js'

function GitHubConnectionSelector({
    connectionId,
    onChange,
    onBusyChange,
    onEditingChange,
    disabled
}) {
    const [connections, setConnections] = useState([])
    const [isLoading, setIsLoading] = useState(true)
    const [isSaving, setIsSaving] = useState(false)
    const [mode, setMode] = useState('list')
    const [name, setName] = useState('')
    const [token, setToken] = useState('')
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')
    const activeRequest = useRef(null)
    const isMounted = useRef(false)
    const mutationPending = useRef(false)
    const selectedConnection = connections.find((connection) => connection.id === connectionId)

    useEffect(() => {
        isMounted.current = true
        handleReload()
        return () => {
            isMounted.current = false
            activeRequest.current?.abort()
        }
    }, [])

    async function handleReload() {
        activeRequest.current?.abort()
        const controller = new AbortController()
        activeRequest.current = controller
        setIsLoading(true)
        setError('')
        try {
            const result = await githubConnectionService.getAllConnections({
                signal: controller.signal
            })
            if (!controller.signal.aborted && isMounted.current) {
                setConnections(result)
            }
        } catch (failure) {
            if (!controller.signal.aborted && isMounted.current && !isRequestCancelled(failure)) {
                setError(failure.message)
            }
        } finally {
            if (!controller.signal.aborted && isMounted.current) {
                setIsLoading(false)
            }
        }
    }

    function handleSelectionChange(event) {
        onChange(event.target.value || null)
        setError('')
        setNotice('')
    }

    function handleAdd() {
        setMode('create')
        setName('')
        setToken('')
        setError('')
        setNotice('')
        onEditingChange(true)
    }

    function handleEdit() {
        setMode('edit')
        setName(selectedConnection.name)
        setToken('')
        setError('')
        setNotice('')
        onEditingChange(true)
    }

    function handleCancelEdit() {
        setMode('list')
        setToken('')
        setError('')
        onEditingChange(false)
    }

    function handleNameChange(event) {
        setName(event.target.value)
    }

    function handleTokenChange(event) {
        setToken(event.target.value)
    }

    function startMutation() {
        mutationPending.current = true
        setIsSaving(true)
        onBusyChange(true)
        setError('')
        setNotice('')
    }

    function finishMutation() {
        mutationPending.current = false
        if (isMounted.current) {
            setIsSaving(false)
            onBusyChange(false)
        }
    }

    async function handleSaveConnection() {
        if (mutationPending.current || disabled) {
            return
        }
        if (!name.trim() || (mode === 'create' && !token.trim())) {
            setError('Bağlantı adını ve yeni bağlantı için GitHub tokenını girin.')
            return
        }
        if (mode === 'edit' && token.trim()) {
            const confirmed = window.confirm(
                'Token yenilendiğinde bu bağlantıyı kullanan bütün projeler yeni tokenı kullanacak. Devam edilsin mi?'
            )
            if (!confirmed) {
                return
            }
        }

        startMutation()
        try {
            const request = { name: name.trim(), token: token.trim() || null }
            let savedConnection
            if (mode === 'create') {
                savedConnection = await githubConnectionService.saveConnection(request)
            } else {
                savedConnection = await githubConnectionService.updateConnection(
                    connectionId,
                    request
                )
            }
            if (!isMounted.current) {
                return
            }
            setConnections((currentConnections) => {
                const remainingConnections = currentConnections.filter(
                    (connection) => connection.id !== savedConnection.id
                )
                return [...remainingConnections, savedConnection].sort((first, second) =>
                    `${first.accountLogin} ${first.name}`.localeCompare(
                        `${second.accountLogin} ${second.name}`
                    )
                )
            })
            onChange(savedConnection.id)
            setToken('')
            setMode('list')
            onEditingChange(false)
            setNotice(
                'GitHub bağlantısı kaydedildi. Proje formunu iptal etseniz de bağlantı listede kalır.'
            )
        } catch (failure) {
            if (isMounted.current) {
                setError(failure.message)
            }
        } finally {
            finishMutation()
        }
    }

    async function handleDeleteConnection() {
        if (mutationPending.current || disabled) {
            return
        }
        const confirmed = window.confirm(
            `“${selectedConnection.name}” GitHub bağlantısını silmek istiyor musunuz?`
        )
        if (!confirmed) {
            return
        }

        startMutation()
        try {
            await githubConnectionService.deleteConnection(connectionId)
            if (isMounted.current) {
                setConnections((currentConnections) =>
                    currentConnections.filter((connection) => connection.id !== connectionId)
                )
                onChange(null)
                setNotice('GitHub bağlantısı silindi.')
            }
        } catch (failure) {
            if (isMounted.current) {
                setError(failure.message)
            }
        } finally {
            finishMutation()
        }
    }

    return (
        <section aria-labelledby="github-connection-title" className="github-connection-selector">
            <h3 id="github-connection-title">GitHub bağlantısı</h3>
            <p className="muted">
                Sistemde kayıtlı hesaplardan bir bağlantı seçin veya yeni token ekleyin. Açık
                depolar için tokensız devam edebilirsiniz.
            </p>
            {isLoading && <p role="status">GitHub bağlantıları yükleniyor…</p>}
            <fieldset disabled={disabled || isSaving}>
                <label className="field">
                    <span>Kayıtlı GitHub bağlantıları</span>
                    <select
                        value={connectionId ?? ''}
                        onChange={handleSelectionChange}
                        disabled={isLoading || mode !== 'list'}
                    >
                        <option value="">Tokensız devam et (açık depolar)</option>
                        {connectionId && !selectedConnection && (
                            <option value={connectionId}>
                                Mevcut bağlantı — listeyi yenileyin
                            </option>
                        )}
                        {connections.map((connection) => (
                            <option key={connection.id} value={connection.id}>
                                {connection.accountLogin} — {connection.name}
                            </option>
                        ))}
                    </select>
                </label>
                {!isLoading && connections.length === 0 && !error && (
                    <p>Kayıtlı GitHub bağlantısı yok.</p>
                )}
                {mode === 'list' ? (
                    <div className="connection-actions">
                        <button
                            type="button"
                            className="button secondary"
                            onClick={handleAdd}
                            disabled={isLoading}
                        >
                            Yeni bağlantı ekle
                        </button>
                        <button
                            type="button"
                            className="text-button"
                            onClick={handleEdit}
                            disabled={!selectedConnection || isLoading}
                        >
                            Bağlantıyı düzenle
                        </button>
                        <button
                            type="button"
                            className="text-button"
                            onClick={handleDeleteConnection}
                            disabled={!selectedConnection || isLoading}
                        >
                            Bağlantıyı sil
                        </button>
                        <button
                            type="button"
                            className="text-button"
                            onClick={handleReload}
                            disabled={isLoading}
                        >
                            Listeyi yenile
                        </button>
                    </div>
                ) : (
                    <div className="connection-editor">
                        <label className="field">
                            <span>Bağlantı adı</span>
                            <input
                                value={name}
                                onChange={handleNameChange}
                                maxLength={80}
                                placeholder="Örn. TaskApp okuma"
                            />
                        </label>
                        <label className="field">
                            <span>
                                {mode === 'create' ? 'GitHub tokenı' : 'Yeni GitHub tokenı'}
                            </span>
                            <input
                                type="password"
                                value={token}
                                onChange={handleTokenChange}
                                maxLength={2048}
                                autoComplete="new-password"
                                spellCheck={false}
                            />
                        </label>
                        <p className="muted small">
                            {mode === 'create'
                                ? 'Hesap adı GitHub üzerinden doğrulanır. Özel depolar için ilgili depoya Contents: read izni verin.'
                                : 'Tokenı boş bırakırsanız mevcut token korunur. Yeni token aynı hesaba ait olmalıdır ve bu bağlantıyı kullanan bütün projelere uygulanır.'}
                        </p>
                        <div className="connection-actions">
                            <button
                                type="button"
                                className="button secondary"
                                onClick={handleSaveConnection}
                            >
                                {isSaving ? 'Kaydediliyor…' : 'Bağlantıyı kaydet'}
                            </button>
                            <button
                                type="button"
                                className="text-button"
                                onClick={handleCancelEdit}
                            >
                                Düzenlemeyi iptal et
                            </button>
                        </div>
                    </div>
                )}
            </fieldset>
            {notice && (
                <p className="notice" role="status">
                    {notice}
                </p>
            )}
            {error && (
                <p className="notice error" role="alert">
                    {error}
                </p>
            )}
        </section>
    )
}

export default GitHubConnectionSelector
