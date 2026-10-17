import { cn } from '@/lib/utils';
import { formatVerdict, minuteOf } from './format';
import type { DuelProblem, DuelSubmission } from '../types';

interface SubmissionFeedProps {
  submissions: DuelSubmission[];
  problems: DuelProblem[];
  startedAtEpoch: number | null;
}

export const SubmissionFeed = ({ submissions, problems, startedAtEpoch }: SubmissionFeedProps) => {
  const letterOf = (problemId: string) => {
    const index = problems.findIndex((p) => p.problemId === problemId);
    return index >= 0 ? String.fromCharCode(65 + index) : problemId;
  };

  return (
    <div className="rounded-2xl border border-white/10 bg-white/3 p-5">
      <h3 className="text-sm font-semibold text-white/50">Submissions</h3>
      {submissions.length === 0 ? (
        <p className="mt-3 text-sm text-white/50">No submissions yet.</p>
      ) : (
        <ul className="mt-3 max-h-96 space-y-1 overflow-y-auto">
          {submissions.map((s) => (
            <li key={s.submissionId} className="flex items-center gap-3 rounded-lg px-3 py-2 text-sm hover:bg-white/5">
              <span className="w-10 shrink-0 text-white/50">{minuteOf(s.submittedAtEpoch, startedAtEpoch)}′</span>
              <span className="min-w-0 flex-1 truncate font-medium text-white">{s.handle}</span>
              <span className="w-6 shrink-0 text-center font-bold text-white/70">{letterOf(s.problemId)}</span>
              <span
                className={cn(
                  'w-40 shrink-0 text-right',
                  s.verdict === 'OK' ? 'font-semibold text-brand-light' : 'text-white/50',
                )}
                title={s.penalised ? '+20 minutes if solved later' : undefined}
              >
                {formatVerdict(s.verdict)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};
