import { useState } from 'react';
import { Link } from 'react-router';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { History } from 'lucide-react';
import { cn } from '@/lib/utils';
import { getDuelHistory } from '../api/duelApi';
import { duelKeys } from '../hooks/duelKeys';
import type { DuelResult } from '../types';

const RESULT_STYLE: Record<DuelResult, string> = {
  WIN: 'bg-brand text-white',
  LOSS: 'bg-white/10 text-white/70',
  DRAW: 'bg-white/5 text-white/50',
};

export const DuelHistoryPage = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useQuery({
    queryKey: duelKeys.history(page),
    queryFn: () => getDuelHistory(page),
    placeholderData: keepPreviousData,
  });

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="flex items-center gap-3 text-3xl font-bold text-white">
        <History className="text-brand" size={28} /> Duel history
      </h1>

      <div className="mt-8 space-y-3">
        {isLoading && <p className="text-white/50">Loading…</p>}
        {data?.content.length === 0 && (
          <div className="rounded-2xl border border-white/10 bg-white/3 p-8 text-center text-white/50">
            No finished duels yet.{' '}
            <Link to="/duels/new" className="text-brand-light hover:text-white">
              Start one
            </Link>
          </div>
        )}
        {data?.content.map((item) => (
          <Link
            key={item.duelId}
            to={`/duels/${item.duelId}`}
            className="flex items-center gap-4 rounded-2xl border border-white/10 bg-white/3 px-5 py-4 transition hover:bg-white/5"
          >
            <span className={cn('w-16 rounded-md py-1 text-center text-xs font-bold', RESULT_STYLE[item.result])}>
              {item.result}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate font-semibold text-white">
                {item.yourHandle} <span className="text-white/30">vs</span> {item.opponentHandle ?? '—'}
              </p>
              <p className="text-xs text-white/50">
                {item.problemCount} problem{item.problemCount !== 1 && 's'}
                {item.endedAt && ` · ${new Date(item.endedAt).toLocaleString()}`}
              </p>
            </div>
            <div className="text-right text-sm">
              <p className="font-semibold text-white">
                {item.yourSolved} – {item.opponentSolved}
              </p>
              <p className="text-xs text-white/50">
                penalty {item.yourPenalty} / {item.opponentPenalty}
              </p>
            </div>
          </Link>
        ))}
      </div>

      {data && data.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-4 text-sm">
          <button
            onClick={() => setPage((p) => p - 1)}
            disabled={page === 0}
            className="rounded-lg bg-white/10 px-4 py-2 text-white hover:bg-white/15 disabled:opacity-40"
          >
            Previous
          </button>
          <span className="text-white/50">
            {page + 1} / {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={page + 1 >= data.totalPages}
            className="rounded-lg bg-white/10 px-4 py-2 text-white hover:bg-white/15 disabled:opacity-40"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};
