import { useAuth } from '@/features/auth';
import { Link, NavLink, Outlet } from 'react-router';

export const Layout = () => {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-surface text-white">
      <header className="border-b border-white/10">
        <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">

          <Link to="/" className="text-lg font-semibold">
            Code<span className="text-brand">Duels</span>
          </Link>

          <div className="flex items-center gap-6 text-sm">
            <NavLink
              to="/problems"
              className={({ isActive }) =>
                isActive ? 'text-white' : 'text-white/50 hover:text-white/80'
              }
            >
              Problems
            </NavLink>
            <NavLink
              to="/matches/new"
              className={({ isActive }) =>
                isActive ? 'text-white' : 'text-white/50 hover:text-white/80'
              }
            >
              New Match
            </NavLink>
            <NavLink
              to="/history"
              className={({ isActive }) =>
                isActive ? 'text-white' : 'text-white/50 hover:text-white/80'
              }
            >
              History
            </NavLink>
          </div>

          <div className="flex items-center gap-4">
            <span className="text-sm text-white/70">{user?.username}</span>
            <button
              onClick={logout}
              className="rounded-md bg-white/10 px-4 py-2 text-sm hover:bg-white/15"
            >
              Logout
            </button>
          </div>
        </nav>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
};