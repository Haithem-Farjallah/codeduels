export const ENDPOINTS = {
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    refresh: '/auth/refresh',
    logout: '/auth/logout',
  },
  problems: {
    list: '/problems',
    create: '/problems',
    bySlug: (slug: string) => `/problems/${slug}`,
    publish: (slug: string) => `/problems/${slug}/publish`,
  },
  matches: {
    create: '/matches',
    join: (roomCode: string) => `/matches/join/${roomCode}`,
    state: (matchId: string) => `/matches/${matchId}`,
    results: (matchId: string) => `/matches/${matchId}/results`,
    history: '/matches/history',
  },
  submissions: {
    create: '/submissions',
    byId: (id: string) => `/submissions/${id}`,
    list: '/submissions',
  },
  users:{
    me:"/users/me"
  }
} as const;