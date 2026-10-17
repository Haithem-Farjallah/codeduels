import { useState } from 'react';
import { Check, Copy, Hourglass, Link2, Swords } from 'lucide-react';
import { toast } from 'sonner';
import { useServerNow } from '../hooks/useServerNow';
import { useCancelDuel } from '../hooks/useDuelMutations';
import { codeforcesProfileUrl, formatClock } from './format';
import { ProblemList } from './ProblemList';
import type { DuelState, PlayerScore } from '../types';

interface DuelLobbyProps {
  state: DuelState;
  myUserId?: string;
}

/** WAITING_FOR_OPPONENT (share the code) and SCHEDULED (countdown to start) */
export const DuelLobby = ({ state, myUserId }: DuelLobbyProps) => {
  const waiting = state.status === 'WAITING_FOR_OPPONENT';
  const isCreator = state.playerOne?.userId === myUserId;
  const now = useServerNow(!waiting);
  const cancel = useCancelDuel(state.duelId);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="rounded-2xl border border-white/10 bg-white/3 p-8 text-center">
        {waiting ? (
          <>
            <Hourglass className="mx-auto text-brand" size={32} />
            <h1 className="mt-4 text-2xl font-bold text-white">Waiting for an opponent</h1>
            <p className="mt-2 text-white/50">Share the room code — the duel is scheduled as soon as they join.</p>
            {state.roomCode && <RoomCode code={state.roomCode} />}
          </>
        ) : (
          <>
            <Swords className="mx-auto text-brand" size={32} />
            <h1 className="mt-4 text-2xl font-bold text-white">Duel starts in</h1>
            <p className="mt-3 font-mono text-5xl font-bold text-white tabular-nums">
              {formatClock((state.scheduledAtEpoch ?? 0) - now)}
            </p>
            <p className="mt-3 text-sm text-white/50">
              {state.durationMinutes} minutes · {state.problems.length} problem{state.problems.length > 1 && 's'} ·
              ICPC scoring
            </p>
          </>
        )}

        <div className="mt-8 flex items-center justify-center gap-6">
          <PlayerChip player={state.playerOne} label="Creator" />
          <span className="text-sm font-bold text-white/30">VS</span>
          <PlayerChip player={state.playerTwo} label="Opponent" />
        </div>

        {waiting && isCreator && (
          <button
            onClick={() => cancel.mutate()}
            disabled={cancel.isPending}
            className="mt-8 text-sm text-white/50 underline-offset-4 hover:text-white/80 hover:underline disabled:opacity-50"
          >
            {cancel.isPending ? 'Canceling…' : 'Cancel duel'}
          </button>
        )}
      </div>

      {/* Problems are visible up front: they were picked by the creator, nothing to hide */}
      <ProblemList problems={state.problems} />
    </div>
  );
};

const RoomCode = ({ code }: { code: string }) => {
  const [copied, setCopied] = useState<'code' | 'link' | null>(null);

  const copy = async (what: 'code' | 'link') => {
    const text = what === 'code' ? code : `${window.location.origin}/duels/join?code=${code}`;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(what);
      window.setTimeout(() => setCopied(null), 1500);
    } catch {
      toast.error('Could not copy to clipboard');
    }
  };

  return (
    <div className="mt-6">
      <p className="font-mono text-5xl font-bold tracking-[0.3em] text-white">{code}</p>
      <div className="mt-5 flex justify-center gap-3">
        <button
          onClick={() => copy('code')}
          className="inline-flex items-center gap-2 rounded-lg bg-white/10 px-4 py-2 text-sm text-white hover:bg-white/15"
        >
          {copied === 'code' ? <Check size={16} /> : <Copy size={16} />} Copy code
        </button>
        <button
          onClick={() => copy('link')}
          className="inline-flex items-center gap-2 rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white hover:bg-brand-light"
        >
          {copied === 'link' ? <Check size={16} /> : <Link2 size={16} />} Copy invite link
        </button>
      </div>
    </div>
  );
};

const PlayerChip = ({ player, label }: { player: PlayerScore | null; label: string }) => (
  <div className="w-40 rounded-xl border border-white/10 bg-white/5 px-4 py-3">
    <p className="text-xs text-white/50">{label}</p>
    {player ? (
      <a
        href={codeforcesProfileUrl(player.handle)}
        target="_blank"
        rel="noreferrer"
        className="block truncate font-semibold text-white hover:text-brand-light"
      >
        {player.handle}
      </a>
    ) : (
      <p className="font-semibold text-white/30">—</p>
    )}
  </div>
);
