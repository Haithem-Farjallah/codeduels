import { Link } from 'react-router';
import { Flag, Trophy } from 'lucide-react';
import { Scoreboard } from './Scoreboard';
import { SubmissionFeed } from './SubmissionFeed';
import type { DuelState } from '../types';

interface DuelResultsProps {
  state: DuelState;
  myUserId?: string;
}

export const DuelResults = ({ state, myUserId }: DuelResultsProps) => {
  const me = [state.playerOne, state.playerTwo].find((p) => p?.userId === myUserId);
  const draw = state.winnerHandle == null;
  const won = !draw && me?.handle === state.winnerHandle;

  const title = draw ? 'It’s a draw' : won ? 'You won!' : `${state.winnerHandle} wins`;
  const subtitle = draw
    ? 'Same number of problems solved and the same penalty.'
    : won
      ? 'More problems solved, or the same with less penalty.'
      : 'Better luck next time — rematch?';

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-white/10 bg-white/3 p-8 text-center">
        {draw ? (
          <Flag className="mx-auto text-white/50" size={40} />
        ) : (
          <Trophy className={won ? 'mx-auto text-brand' : 'mx-auto text-white/50'} size={40} />
        )}
        <h1 className="mt-4 text-3xl font-bold text-white">{title}</h1>
        <p className="mt-2 text-white/50">{subtitle}</p>
        <div className="mt-6 flex justify-center gap-3">
          <Link to="/duels/new" className="rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-light">
            New duel
          </Link>
          <Link to="/duels/history" className="rounded-lg bg-white/10 px-5 py-2.5 text-sm text-white hover:bg-white/15">
            Duel history
          </Link>
        </div>
      </div>

      <Scoreboard state={state} myUserId={myUserId} />
      <SubmissionFeed submissions={state.submissions} problems={state.problems} startedAtEpoch={state.startedAtEpoch} />
    </div>
  );
};
