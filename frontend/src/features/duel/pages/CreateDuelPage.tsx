import { Link } from 'react-router';
import { Swords } from 'lucide-react';
import { CreateDuelForm } from '../components/CreateDuelForm';

export const CreateDuelPage = () => (
  <div className="mx-auto max-w-2xl">
    <h1 className="flex items-center gap-3 text-3xl font-bold text-white">
      <Swords className="text-brand" size={28} /> New Codeforces duel
    </h1>
    <p className="mt-2 text-white/50">
      Pick the problems, share the room code, and race on codeforces.com. Most problems solved wins; ties go to
      the lower ICPC penalty.
    </p>
    <div className="mt-8 rounded-2xl border border-white/10 bg-white/3 p-8">
      <CreateDuelForm />
    </div>
    <p className="mt-6 text-center text-sm text-white/50">
      Got a code already?{' '}
      <Link to="/duels/join" className="text-brand-light hover:text-white">
        Join a duel
      </Link>
    </p>
  </div>
);
