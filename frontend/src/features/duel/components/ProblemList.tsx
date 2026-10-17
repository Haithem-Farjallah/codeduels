import { ExternalLink } from 'lucide-react';
import type { DuelProblem } from '../types';

export const ProblemList = ({ problems }: { problems: DuelProblem[] }) => (
  <div className="rounded-2xl border border-white/10 bg-white/3 p-5">
    <h3 className="text-sm font-semibold text-white/50">Problems</h3>
    <ul className="mt-3 space-y-2">
      {problems.map((problem, i) => (
        <li key={problem.problemId}>
          <a
            href={problem.url ?? `https://codeforces.com/problemset/problem/${problem.problemId}`}
            target="_blank"
            rel="noreferrer"
            className="group flex items-center gap-3 rounded-lg px-3 py-2 transition hover:bg-white/5"
          >
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-brand/15 text-sm font-bold text-brand-light">
              {String.fromCharCode(65 + i)}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-white">{problem.name}</span>
              <span className="text-xs text-white/50">{problem.problemId}</span>
            </span>
            <ExternalLink size={16} className="text-white/30 group-hover:text-brand-light" />
          </a>
        </li>
      ))}
    </ul>
    <p className="mt-4 text-xs text-white/50">
      Submit on Codeforces with the handle you entered — verdicts show up here automatically.
    </p>
  </div>
);
