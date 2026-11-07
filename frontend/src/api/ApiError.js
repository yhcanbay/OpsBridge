export class ApiError extends Error {
    constructor(status, message, fields = {}) {
        super(message)
        this.name = 'ApiError'
        this.status = status
        this.fields = fields
    }
}
