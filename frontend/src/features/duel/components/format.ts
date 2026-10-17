/** 75 → "01:15", 3725 → "1:02:05" */
export const formatClock = (totalSeconds: number) => {
  const s = Math.max(0, Math.floor(totalSeconds));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const mm = String(m).padStart(2, '0');
  const ss = String(sec).padStart(2, '0');
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
};

/** Minutes since the duel started, the way ICPC scoreboards show it */
export const minuteOf = (epoch: number, startedAtEpoch: number | null) =>
  startedAtEpoch == null ? 0 : Math.floor((epoch - startedAtEpoch) / 60);

/** WRONG_ANSWER → "Wrong answer", OK → "Accepted" */
export const formatVerdict = (verdict: string) =>
  verdict === 'OK'
    ? 'Accepted'
    : verdict.charAt(0) + verdict.slice(1).toLowerCase().replaceAll('_', ' ');

export const codeforcesProfileUrl = (handle: string) => `https://codeforces.com/profile/${handle}`;
