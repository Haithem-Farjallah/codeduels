import { cn } from '@/lib/utils';
import { codeforcesProfileUrl, minuteOf } from './format';
import type { DuelState, PlayerScore, ProblemStatus } from '../types';

interface ScoreboardProps {
  state: DuelState;
  myUserId?: string;
}

/** ICPC-style: one row per player, one column per problem, then solved and penalty */
export const Scoreboard = ({ state, myUserId }: ScoreboardProps) => {
  const players = [state.playerOne, state.playerTwo].filter((p): p is PlayerScore => p != null);

  return (
    <div className="overflow-x-auto rounded-2xl border border-white/10 bg-white/3">
      <table className="w-full min-w-120 text-sm">
        <thead>
          <tr className="border-b border-white/10 text-white/50">
            <th className="px-4 py-3 text-left font-medium">Player</th>
            {state.problems.map((problem, i) => (
              <th key={problem.problemId} className="px-2 py-3 text-center font-medium" title={problem.name}>
                {String.fromCharCode(65 + i)}
              </th>
            ))}
            <th className="px-3 py-3 text-center font-medium">Solved</th>
            <th className="px-4 py-3 text-center font-medium">Penalty</th>
          </tr>
        </thead>
        <tbody>
          {players.map((player) => {
            const isMe = player.userId === myUserId;
            const isWinner = state.status === 'COMPLETED' && state.winnerHandle === player.handle;
            return (
              <tr key={player.handle} className={cn('border-b border-white/5 last:border-0', isMe && 'bg-brand/10')}>
                <td className="px-4 py-3">
                  <a
                    href={codeforcesProfileUrl(player.handle)}
                    target="_blank"
                    rel="noreferrer"
                    className="font-semibold text-white hover:text-brand-light"
                  >
                    {player.handle}
                  </a>
                  <div className="text-xs text-white/50">
                    {player.username ?? 'unknown'}
                    {isMe && ' · you'}
                    {isWinner && ' · winner'}
                  </div>
                </td>
                {state.problems.map((problem) => (
                  <td key={problem.problemId} className="px-2 py-3 text-center">
                    <ProblemCell status={player.problems[problem.problemId]} startedAtEpoch={state.startedAtEpoch} />
                  </td>
                ))}
                <td className="px-3 py-3 text-center text-lg font-bold text-white">{player.solvedCount}</td>
                <td className="px-4 py-3 text-center text-white/70">{player.totalPenalty}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

/** "+" / "+2" with the solve minute, "−3" for unsolved attempts, blank when untouched */
const ProblemCell = ({ status, startedAtEpoch }: { status?: ProblemStatus; startedAtEpoch: number | null }) => {
  if (!status || (!status.solved && status.attempts === 0)) {
    return <span className="text-white/20">·</span>;
  }

  if (status.solved) {
    return (
      <span className="inline-flex min-w-12 flex-col rounded-md bg-brand px-2 py-1 font-semibold text-white">
        {status.attempts > 0 ? `+${status.attempts}` : '+'}
        {status.solvedAtEpoch != null && (
          <span className="text-[10px] font-normal text-white/80">
            {minuteOf(status.solvedAtEpoch, startedAtEpoch)}′
          </span>
        )}
      </span>
    );
  }

  return (
    <span className="inline-flex min-w-12 justify-center rounded-md bg-white/10 px-2 py-1 font-semibold text-white/70">
      −{status.attempts}
    </span>
  );
};
