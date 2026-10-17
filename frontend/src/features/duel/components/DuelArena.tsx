import { Clock } from 'lucide-react';
import { useServerNow } from '../hooks/useServerNow';
import { formatClock } from './format';
import { ProblemList } from './ProblemList';
import { Scoreboard } from './Scoreboard';
import { SubmissionFeed } from './SubmissionFeed';
import type { DuelState } from '../types';

interface DuelArenaProps {
  state: DuelState;
  myUserId?: string;
}

/** ACTIVE: players solve on codeforces.com, this page is the live scoreboard */
export const DuelArena = ({ state, myUserId }: DuelArenaProps) => {
  const now = useServerNow();
  const remaining = (state.endsAtEpoch ?? 0) - now;
  const timeUp = remaining <= 0;

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-white/10 bg-white/3 px-6 py-4">
        <div>
          <p className="text-xs font-semibold tracking-wide text-brand-light">CODEFORCES DUEL · LIVE</p>
          <h1 className="mt-1 text-xl font-bold text-white">
            {state.playerOne?.handle} <span className="text-white/30">vs</span> {state.playerTwo?.handle}
          </h1>
        </div>
        <div className="text-right">
          <p className="flex items-center justify-end gap-2 font-mono text-3xl font-bold text-white tabular-nums">
            <Clock size={22} className="text-brand" />
            {formatClock(remaining)}
          </p>
          <p className="text-xs text-white/50">
            {timeUp ? 'Time’s up — waiting for the last verdicts…' : 'Most solved wins · ties broken by penalty'}
          </p>
        </div>
      </header>

      <Scoreboard state={state} myUserId={myUserId} />

      <div className="grid gap-6 lg:grid-cols-[2fr_3fr]">
        <ProblemList problems={state.problems} />
        <SubmissionFeed submissions={state.submissions} problems={state.problems} startedAtEpoch={state.startedAtEpoch} />
      </div>
    </div>
  );
};
