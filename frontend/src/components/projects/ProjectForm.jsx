import { useEffect, useRef, useState } from 'react';
import { CAPABILITY_LABELS, EMPTY_PROJECT } from '../../constants/projectProfile.js';

export default function ProjectForm({ project, onSave, onClose }) {
  const dialog = useRef(null);
  const [values, setValues] = useState(() => ({ ...EMPTY_PROJECT, ...project }));
  const [error, setError] = useState('');
  const [fields, setFields] = useState({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const element = dialog.current;
    element.showModal();
    return () => element.close();
  }, []);

  function field(
    name,
    label,
    { required = false, type = 'text', maxLength, placeholder, hint, pattern } = {},
  ) {
    return (
      <label className="field" key={name}>
        <span>
          {label}
          {required && <span className="required"> *</span>}
        </span>
        <input
          name={name}
          value={values[name] ?? ''}
          type={type}
          required={required}
          maxLength={maxLength}
          placeholder={placeholder}
          pattern={pattern}
          aria-invalid={Boolean(fields[name])}
          onChange={(event) => setValues({ ...values, [name]: event.target.value })}
        />
        {(fields[name] || hint) && (
          <small className={fields[name] ? 'field-error' : 'muted'}>{fields[name] ?? hint}</small>
        )}
      </label>
    );
  }

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    setFields({});
    try {
      const body = {
        name: values.name,
        repositoryUrl: values.repositoryUrl,
        environment: values.environment,
        jenkinsBaseUrlRef: values.jenkinsBaseUrlRef,
        jenkinsJobPath: values.jenkinsJobPath,
        containerLabelSelector: values.containerLabelSelector,
        healthUrl: values.healthUrl,
        capabilities: values.capabilities,
        enabled: values.enabled,
      };
      await onSave(body);
    } catch (failure) {
      setError(failure.message);
      setFields(failure.fields ?? {});
      setBusy(false);
    }
  }

  return (
    <dialog
      ref={dialog}
      className="project-dialog"
      aria-labelledby="form-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
    >
      <form onSubmit={submit}>
        <header className="dialog-header">
          <div>
            <p className="eyebrow">PROJE PROFİLİ</p>
            <h2 id="form-title">{project ? 'Profili düzenle' : 'Yeni proje ekle'}</h2>
          </div>
          <button
            type="button"
            className="icon-button"
            aria-label="Formu kapat"
            disabled={busy}
            onClick={onClose}
          >
            ×
          </button>
        </header>
        <div className="dialog-content">
          <p className="muted">
            Bir profil, projenin nerede olduğunu ve hangi bağlantıları desteklediğini tanımlar.
          </p>
          <div className="form-grid">
            {field('name', 'Proje adı', {
              required: true,
              maxLength: 80,
              placeholder: 'Örn. Personal_Task_App',
            })}
            {field('environment', 'Ortam', {
              required: true,
              maxLength: 30,
              pattern: '[a-z][a-z0-9_-]{0,29}',
              hint: 'Örn. local, staging, production',
            })}
            <div className="span-2">
              {field('repositoryUrl', 'Repository URL', {
                required: true,
                type: 'url',
                maxLength: 500,
                placeholder: 'https://github.com/…',
              })}
            </div>
          </div>
          <h3>
            Bağlantı bilgileri <span className="muted optional">İsteğe bağlı</span>
          </h3>
          <div className="form-grid">
            {field('jenkinsBaseUrlRef', 'Jenkins bağlantı referansı', {
              maxLength: 100,
              placeholder: 'local-jenkins',
              hint: 'Bağlantıya verilen ad; token veya parola yazmayın.',
            })}
            {field('jenkinsJobPath', 'Jenkins job yolu', {
              maxLength: 200,
              placeholder: 'folder/job-name',
            })}
            <div className="span-2">
              {field('containerLabelSelector', 'Container label seçicisi', {
                maxLength: 250,
                placeholder: 'com.docker.compose.project=proje-adi',
              })}
            </div>
            <div className="span-2">
              {field('healthUrl', 'HTTP health URL', {
                type: 'url',
                maxLength: 500,
                placeholder: 'http://localhost:8080/actuator/health',
              })}
            </div>
          </div>
          <fieldset>
            <legend>Desteklenen yetenekler</legend>
            <p className="muted small">
              Yetenek seçimi, bağlantının çalıştığını göstermez. Gerçek entegrasyonlar sonraki
              adımlarda eklenecek.
            </p>
            <div className="capability-options">
              {Object.entries(CAPABILITY_LABELS).map(([key, label]) => (
                <label className="check-label" key={key}>
                  <input
                    type="checkbox"
                    checked={values.capabilities.includes(key)}
                    onChange={(event) =>
                      setValues({
                        ...values,
                        capabilities: event.target.checked
                          ? [...values.capabilities, key]
                          : values.capabilities.filter((value) => value !== key),
                      })
                    }
                  />
                  {label}
                </label>
              ))}
            </div>
          </fieldset>
          <label className="check-label enabled-check">
            <input
              type="checkbox"
              checked={values.enabled}
              onChange={(event) => setValues({ ...values, enabled: event.target.checked })}
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
          <button type="button" className="button secondary" disabled={busy} onClick={onClose}>
            Vazgeç
          </button>
          <button className="button primary" disabled={busy}>
            {busy ? 'Kaydediliyor…' : 'Profili kaydet'}
          </button>
        </footer>
      </form>
    </dialog>
  );
}
