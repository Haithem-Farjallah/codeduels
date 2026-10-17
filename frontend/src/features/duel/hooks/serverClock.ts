import type { DuelState } from '../types';

/**
 * Timing is server-owned; the client only displays it. Every state the server sends carries
 * secondsRemaining computed at send time, which tells us how far the local clock is off.
 */
let offsetSeconds = 0;

export const syncServerClock = (state: DuelState) => {
  const target =
    state.status === 'SCHEDULED' ? state.scheduledAtEpoch :
    state.status === 'ACTIVE' ? state.endsAtEpoch : null;

  // secondsRemaining is clamped at 0, so a zero tells us nothing about the clock
  if (target == null || state.secondsRemaining <= 0) return;

  const serverNow = target - state.secondsRemaining;
  offsetSeconds = serverNow - Date.now() / 1000;
};

export const serverNowSeconds = () => Date.now() / 1000 + offsetSeconds;
