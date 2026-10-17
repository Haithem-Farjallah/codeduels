import { useQuery } from '@tanstack/react-query';
import { getDuelState } from '../api/duelApi';
import { syncServerClock } from './serverClock';
import { useDuelSocket } from './useDuelSocket';
import { duelKeys } from './duelKeys';

const LIVE = new Set(['WAITING_FOR_OPPONENT', 'SCHEDULED', 'ACTIVE']);

/** Snapshot once on mount, then live deltas over the socket into the same cache entry */
export const useDuel = (duelId: string) => {
  const query = useQuery({
    queryKey: duelKeys.detail(duelId),
    queryFn: async () => {
      const state = await getDuelState(duelId);
      syncServerClock(state);
      return state;
    },
    // Safety net for events missed while the socket was reconnecting
    refetchInterval: (q) => (q.state.data && LIVE.has(q.state.data.status) ? 30_000 : false),
    staleTime: 0,
  });

  const status = query.data?.status;
  // Only subscribe once the GET proved we're a participant — a rejected SUBSCRIBE kills the connection
  useDuelSocket(duelId, status !== undefined && LIVE.has(status));

  return query;
};
