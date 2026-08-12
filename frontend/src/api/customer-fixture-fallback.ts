import { ConnectionError, HttpError } from '@/api/types'

/** Use wireframe fixtures when the API is unreachable (local e2e without backend stack). */
export function shouldUseCustomerFixtureFallback(error: unknown): boolean {
  if (!import.meta.env.DEV) {
    return false
  }
  if (error instanceof ConnectionError) {
    return true
  }
  return error instanceof HttpError && (error.status === 404 || error.status >= 500)
}
