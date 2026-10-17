import { useEffect, useState } from 'react';
import { serverNowSeconds } from './serverClock';

/** Server-adjusted epoch seconds, re-rendering once a second while `active` */
export const useServerNow = (active = true) => {
  const [now, setNow] = useState(serverNowSeconds);

  useEffect(() => {
    if (!active) return;
    const id = window.setInterval(() => setNow(serverNowSeconds()), 1000);
    return () => window.clearInterval(id);
  }, [active]);

  return now;
};
