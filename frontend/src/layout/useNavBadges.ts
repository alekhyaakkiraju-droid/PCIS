import { useQuery } from '@tanstack/react-query'
import { claimsApi } from '@/api/claims-api'
import { shouldUseClaimsFixtureFallback } from '@/api/claims-fixture-fallback'
import { batchStatusApi } from '@/api/batch-status-api'
import inquiryFixture from '../../fixtures/claims/inquiry.json'

/** Live nav badge counts, keyed by route path. Falls back to nav-config's static badge on error/loading. */
export function useNavBadges(): Record<string, string> {
  const openClaims = useQuery({
    queryKey: ['nav-badge', 'claims-open'],
    queryFn: async () => {
      try {
        return (await claimsApi.list({ view: 'open' })).length
      } catch (error) {
        if (shouldUseClaimsFixtureFallback(error)) {
          return (inquiryFixture as Record<string, unknown[]>).open?.length ?? 0
        }
        throw error
      }
    },
    staleTime: 30_000,
  })

  const batchAttention = useQuery({
    queryKey: ['nav-badge', 'batch-attention'],
    queryFn: async () => {
      const runs = await batchStatusApi.listRuns()
      return runs.filter((r) => r.status === 'FAILED' || r.status === 'UNKNOWN').length
    },
    staleTime: 30_000,
  })

  const badges: Record<string, string> = {}
  if (openClaims.data != null) badges['/claims'] = String(openClaims.data)
  if (batchAttention.data != null) badges['/batch'] = String(batchAttention.data)
  return badges
}
