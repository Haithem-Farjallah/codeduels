import { useAuth } from '@/features/auth';

export const Hero = () => {
  const { user, isAuthenticated } = useAuth();

  return (
    <section>
      <h1 className="text-4xl font-bold text-white">
        {isAuthenticated ? (
          <>Welcome back, <span className="text-brand">{user?.username}</span></>
        ) : (
          <>Sharpen your skills in <span className="text-brand">1v1 duels</span></>
        )}
      </h1>
      <p className="mt-3 text-lg text-white/50">
        Race an opponent to solve the same problem first.
      </p>
    </section>
  );
};