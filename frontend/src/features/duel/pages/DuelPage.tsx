import { Link, useParams } from 'react-router';
import { useAuth } from '@/features/auth';
import { getErrorMessage } from '@/shared/api/errors';
import { useDuel } from '../hooks/useDuel';
import { DuelLobby } from '../components/DuelLobby';
import { DuelArena } from '../components/DuelArena';
import { DuelResults } from '../components/DuelResults';

/** One route for the whole lifecycle — the view follows the server's status */
export const DuelPage = () => {
  const { duelId = '' } = useParams();
  const { user } = useAuth();
  const { data: state, isLoading, error } = useDuel(duelId);

  if (isLoading) {
    return (
      <div className="flex justify-center py-24">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/20 border-t-brand" />
      </div>
    );
  }

  if (error || !state) {
    return <Message title="Duel not found" body={getErrorMessage(error, 'This duel doesn’t exist or has expired.')} />;
  }

  switch (state.status) {
    case 'WAITING_FOR_OPPONENT':
    case 'SCHEDULED':
      return <DuelLobby state={state} myUserId={user?.id} />;
    case 'ACTIVE':
      return <DuelArena state={state} myUserId={user?.id} />;
    case 'COMPLETED':
      return <DuelResults state={state} myUserId={user?.id} />;
    case 'CANCELED':
    case 'EXPIRED':
      return <Message title="Duel canceled" body="This duel won’t take place." />;
  }
};

const Message = ({ title, body }: { title: string; body: string }) => (
  <div className="mx-auto max-w-md rounded-2xl border border-white/10 bg-white/3 p-8 text-center">
    <h1 className="text-2xl font-bold text-white">{title}</h1>
    <p className="mt-2 text-white/50">{body}</p>
    <Link to="/duels/new" className="mt-6 inline-block rounded-lg bg-brand px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-light">
      Create a duel
    </Link>
  </div>
);
