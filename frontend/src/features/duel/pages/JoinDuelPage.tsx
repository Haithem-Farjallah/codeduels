import { Link, useSearchParams } from 'react-router';
import { Users } from 'lucide-react';
import { JoinDuelForm } from '../components/JoinDuelForm';

export const JoinDuelPage = () => {
  const [params] = useSearchParams();

  return (
    <div className="mx-auto max-w-md">
      <h1 className="flex items-center gap-3 text-3xl font-bold text-white">
        <Users className="text-brand" size={28} /> Join a duel
      </h1>
      <p className="mt-2 text-white/50">Enter the room code your opponent shared.</p>
      <div className="mt-8 rounded-2xl border border-white/10 bg-white/3 p-8">
        <JoinDuelForm initialCode={params.get('code') ?? ''} />
      </div>
      <p className="mt-6 text-center text-sm text-white/50">
        Want to host instead?{' '}
        <Link to="/duels/new" className="text-brand-light hover:text-white">
          Create a duel
        </Link>
      </p>
    </div>
  );
};
