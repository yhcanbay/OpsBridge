export const CAPABILITY_LABELS = {
    BUILD_VIEW: 'Build görüntüleme',
    BUILD_TRIGGER: 'Build tetikleme',
    CONTAINER_VIEW: 'Container görüntüleme',
    HEALTH_VIEW: 'HTTP health',
    SECURITY_VIEW: 'Güvenlik raporları'
}

export const EMPTY_PROJECT = {
    name: '',
    repositoryUrl: '',
    githubConnectionId: null,
    environment: 'local',
    jenkinsBaseUrlRef: '',
    jenkinsJobPath: '',
    containerLabelSelector: '',
    healthUrl: '',
    capabilities: [],
    enabled: true
}

export const INTEGRATION_STATE_LABELS = {
    CONNECTED: 'Bağlı',
    NOT_CONNECTED: 'Henüz bağlanmadı',
    NOT_SUPPORTED: 'Desteklenmiyor',
    DISABLED: 'Proje pasif',
    UNREACHABLE: 'Ulaşılamıyor',
    JOB_NOT_FOUND: 'Job bulunamadı',
    ACCESS_DENIED: 'Erişim reddedildi',
    INVALID_RESPONSE: 'Yanıt okunamadı'
}
