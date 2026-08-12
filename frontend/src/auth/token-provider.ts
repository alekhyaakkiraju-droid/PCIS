let accessTokenProvider: (() => Promise<string | null>) | null = null

export function setAccessTokenProvider(provider: (() => Promise<string | null>) | null): void {
  accessTokenProvider = provider
}

export async function getAccessToken(): Promise<string | null> {
  if (!accessTokenProvider) {
    return null
  }
  return accessTokenProvider()
}
