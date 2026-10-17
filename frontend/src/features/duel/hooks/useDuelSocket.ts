import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { subscribe } from '@/shared/ws/stompClient';
import { syncServerClock } from './serverClock';
import { duelKeys } from './duelKeys';
import type { DuelEvent, DuelState } from '../types';

/**
 * Patches the ['duel', id] cache entry from /topic/duel/{id}: phase transitions replace the
 * whole state, scoreboard updates patch one player and prepend the submission.
 */
export const useDuelSocket = (duelId: string, enabled: boolean) => {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!enabled) return;

    const key = duelKeys.detail(duelId);

    return subscribe(`/topic/duel/${duelId}`, (body) => {
      const event = body as DuelEvent;

      switch (event.eventType) {
        case 'PLAYER_JOINED':
        case 'DUEL_START':
        case 'DUEL_END':
          syncServerClock(event.state);
          queryClient.setQueryData<DuelState>(key, event.state);
          if (event.eventType === 'PLAYER_JOINED') toast.success('Your opponent joined!');
          if (event.eventType === 'DUEL_START') toast.success('The duel has started — good luck!');
          break;

        case 'SCOREBOARD_UPDATE':
          queryClient.setQueryData<DuelState>(key, (old) => {
            if (!old) return old;
            const { player, submission } = event;
            const isPlayerOne = old.playerOne?.handle === player.handle;
            const alreadyListed = old.submissions.some((s) => s.submissionId === submission.submissionId);
            return {
              ...old,
              playerOne: isPlayerOne ? player : old.playerOne,
              playerTwo: isPlayerOne ? old.playerTwo : player,
              submissions: alreadyListed ? old.submissions : [submission, ...old.submissions],
            };
          });
          break;

        case 'DUEL_CANCELED':
          // the page renders the canceled view; no toast, the creator already got one from the mutation
          queryClient.setQueryData<DuelState>(key, (old) => (old ? { ...old, status: 'CANCELED' } : old));
          break;
      }
    });
  }, [duelId, enabled, queryClient]);
};
