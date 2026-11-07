import { useEffect, useRef, useState } from 'react'
import { CAPABILITY_LABELS, EMPTY_PROJECT } from '../../constants/projectProfile.js'

function ProjectForm({ project, onSave, onClose }) {
    const dialog = useRef(null)
    const [values, setValues] = useState(() => ({ ...EMPTY_PROJECT, ...project }))
    const [error, setError] = useState('')
    const [fieldErrors, setFieldErrors] = useState({})
    const [isSaving, setIsSaving] = useState(false)

    useEffect(() => {
        const element = dialog.current
        element.showModal()
        return () => {
            element.close()
        }
    }, [])

    function handleChange(event) {
        const { name, value, type, checked } = event.target
        let fieldValue = value

        if (type === 'checkbox') {
            fieldValue = checked
        }

        setValues((currentValues) => ({ ...currentValues, [name]: fieldValue }))
    }

    function handleCapabilityChange(event) {
        const { value, checked } = event.target

        setValues((currentValues) => {
            let capabilities

            if (checked) {
                capabilities = [...currentValues.capabilities, value]
            } else {
                capabilities = currentValues.capabilities.filter(
                    (capability) => capability !== value
                )
            }

            return { ...currentValues, capabilities }
        })
    }

    function handleCancel(event) {
        event.preventDefault()

        if (!isSaving) {
            onClose()
        }
    }

    async function handleSubmit(event) {
        event.preventDefault()

        if (isSaving) {
            return
        }

        setIsSaving(true)
        setError('')
        setFieldErrors({})

        try {
            const projectRequest = {
                name: values.name,
                repositoryUrl: values.repositoryUrl,
                environment: values.environment,
                jenkinsBaseUrlRef: values.jenkinsBaseUrlRef,
                jenkinsJobPath: values.jenkinsJobPath,
                containerLabelSelector: values.containerLabelSelector,
                healthUrl: values.healthUrl,
                capabilities: values.capabilities,
                enabled: values.enabled
            }
            await onSave(projectRequest)
        } catch (failure) {
            setError(failure.message)
            setFieldErrors(failure.fields ?? {})
        } finally {
            setIsSaving(false)
        }
    }

    return (
        <dialog
            ref={dialog}
            className="project-dialog"
            aria-labelledby="form-title"
            onCancel={handleCancel}
        >
            <form onSubmit={handleSubmit}>
                <header className="dialog-header">
                    <div>
                        <p className="eyebrow">PROJE PROFİLİ</p>
                        <h2 id="form-title">{project ? 'Profili düzenle' : 'Yeni proje ekle'}</h2>
                    </div>
                    <button
                        type="button"
                        className="icon-button"
                        aria-label="Formu kapat"
                        disabled={isSaving}
                        onClick={onClose}
                    >
                        ×
                    </button>
                </header>
                <div className="dialog-content">
                    <p className="muted">
                        Bir profil, projenin nerede olduğunu ve hangi bağlantıları desteklediğini
                        tanımlar.
                    </p>
                    <div className="form-grid">
                        <label className="field">
                            <span>
                                Proje adı<span className="required"> *</span>
                            </span>
                            <input
                                name="name"
                                value={values.name ?? ''}
                                onChange={handleChange}
                                required
                                maxLength={80}
                                placeholder="Örn. Personal_Task_App"
                                aria-invalid={Boolean(fieldErrors.name)}
                                aria-describedby="name-hint"
                            />
                            {fieldErrors.name && (
                                <small
                                    id="name-hint"
                                    className={fieldErrors.name ? 'field-error' : 'muted'}
                                >
                                    {fieldErrors.name}
                                </small>
                            )}
                        </label>
                        <label className="field">
                            <span>
                                Ortam<span className="required"> *</span>
                            </span>
                            <input
                                name="environment"
                                value={values.environment ?? ''}
                                onChange={handleChange}
                                required
                                maxLength={30}
                                pattern="[a-z][a-z0-9_-]{0,29}"
                                aria-invalid={Boolean(fieldErrors.environment)}
                                aria-describedby="environment-hint"
                            />
                            <small
                                id="environment-hint"
                                className={fieldErrors.environment ? 'field-error' : 'muted'}
                            >
                                {fieldErrors.environment || 'Örn. local, staging, production'}
                            </small>
                        </label>
                        <div className="span-2">
                            <label className="field">
                                <span>
                                    Repository URL<span className="required"> *</span>
                                </span>
                                <input
                                    name="repositoryUrl"
                                    value={values.repositoryUrl ?? ''}
                                    onChange={handleChange}
                                    type="url"
                                    required
                                    maxLength={500}
                                    placeholder="https://github.com/…"
                                    aria-invalid={Boolean(fieldErrors.repositoryUrl)}
                                    aria-describedby="repositoryUrl-hint"
                                />
                                {fieldErrors.repositoryUrl && (
                                    <small
                                        id="repositoryUrl-hint"
                                        className={
                                            fieldErrors.repositoryUrl ? 'field-error' : 'muted'
                                        }
                                    >
                                        {fieldErrors.repositoryUrl}
                                    </small>
                                )}
                            </label>
                        </div>
                    </div>
                    <h3>
                        Bağlantı bilgileri <span className="muted optional">İsteğe bağlı</span>
                    </h3>
                    <div className="form-grid">
                        <label className="field">
                            <span>Jenkins bağlantı referansı</span>
                            <input
                                name="jenkinsBaseUrlRef"
                                value={values.jenkinsBaseUrlRef ?? ''}
                                onChange={handleChange}
                                maxLength={100}
                                placeholder="local-jenkins"
                                aria-invalid={Boolean(fieldErrors.jenkinsBaseUrlRef)}
                                aria-describedby="jenkinsBaseUrlRef-hint"
                            />
                            <small
                                id="jenkinsBaseUrlRef-hint"
                                className={fieldErrors.jenkinsBaseUrlRef ? 'field-error' : 'muted'}
                            >
                                {fieldErrors.jenkinsBaseUrlRef ||
                                    'Bağlantıya verilen ad; token veya parola yazmayın.'}
                            </small>
                        </label>
                        <label className="field">
                            <span>Jenkins job yolu</span>
                            <input
                                name="jenkinsJobPath"
                                value={values.jenkinsJobPath ?? ''}
                                onChange={handleChange}
                                maxLength={200}
                                placeholder="folder/job-name"
                                aria-invalid={Boolean(fieldErrors.jenkinsJobPath)}
                                aria-describedby="jenkinsJobPath-hint"
                            />
                            {fieldErrors.jenkinsJobPath && (
                                <small
                                    id="jenkinsJobPath-hint"
                                    className={fieldErrors.jenkinsJobPath ? 'field-error' : 'muted'}
                                >
                                    {fieldErrors.jenkinsJobPath}
                                </small>
                            )}
                        </label>
                        <div className="span-2">
                            <label className="field">
                                <span>Container label seçicisi</span>
                                <input
                                    name="containerLabelSelector"
                                    value={values.containerLabelSelector ?? ''}
                                    onChange={handleChange}
                                    maxLength={250}
                                    placeholder="com.docker.compose.project=proje-adi"
                                    aria-invalid={Boolean(fieldErrors.containerLabelSelector)}
                                    aria-describedby="containerLabelSelector-hint"
                                />
                                {fieldErrors.containerLabelSelector && (
                                    <small
                                        id="containerLabelSelector-hint"
                                        className={
                                            fieldErrors.containerLabelSelector
                                                ? 'field-error'
                                                : 'muted'
                                        }
                                    >
                                        {fieldErrors.containerLabelSelector}
                                    </small>
                                )}
                            </label>
                        </div>
                        <div className="span-2">
                            <label className="field">
                                <span>HTTP health URL</span>
                                <input
                                    name="healthUrl"
                                    value={values.healthUrl ?? ''}
                                    onChange={handleChange}
                                    type="url"
                                    maxLength={500}
                                    placeholder="http://localhost:8080/actuator/health"
                                    aria-invalid={Boolean(fieldErrors.healthUrl)}
                                    aria-describedby="healthUrl-hint"
                                />
                                {fieldErrors.healthUrl && (
                                    <small
                                        id="healthUrl-hint"
                                        className={fieldErrors.healthUrl ? 'field-error' : 'muted'}
                                    >
                                        {fieldErrors.healthUrl}
                                    </small>
                                )}
                            </label>
                        </div>
                    </div>
                    <fieldset>
                        <legend>Desteklenen yetenekler</legend>
                        <p className="muted small">
                            Yetenek seçimi, bağlantının çalıştığını göstermez. Gerçek entegrasyonlar
                            sonraki adımlarda eklenecek.
                        </p>
                        <div className="capability-options">
                            {Object.entries(CAPABILITY_LABELS).map(([key, label]) => (
                                <label className="check-label" key={key}>
                                    <input
                                        type="checkbox"
                                        checked={values.capabilities.includes(key)}
                                        value={key}
                                        onChange={handleCapabilityChange}
                                    />
                                    {label}
                                </label>
                            ))}
                        </div>
                    </fieldset>
                    <label className="check-label enabled-check">
                        <input
                            type="checkbox"
                            name="enabled"
                            checked={values.enabled}
                            onChange={handleChange}
                        />
                        Proje aktif
                    </label>
                    {error && (
                        <p className="notice error" role="alert">
                            {error}
                        </p>
                    )}
                </div>
                <footer className="dialog-footer">
                    <button
                        type="button"
                        className="button secondary"
                        disabled={isSaving}
                        onClick={onClose}
                    >
                        Vazgeç
                    </button>
                    <button className="button primary" disabled={isSaving}>
                        {isSaving ? 'Kaydediliyor…' : 'Profili kaydet'}
                    </button>
                </footer>
            </form>
        </dialog>
    )
}

export default ProjectForm
