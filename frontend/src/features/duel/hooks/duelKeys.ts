export const duelKeys = {
  all: ['duel'] as const,
  detail: (duelId: string) => ['duel', duelId] as const,
  history: (page: number) => ['duel', 'history', page] as const,
};
