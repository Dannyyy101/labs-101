export interface ApiError {
    timestamp: string
    status: number,
    error: string
    errorMessage: string
    path: string
}