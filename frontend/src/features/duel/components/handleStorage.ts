const KEY = 'cfHandle';

/** Remembers the last Codeforces handle used so the forms can prefill it */
export const loadHandle = () => {
  try {
    return localStorage.getItem(KEY) ?? '';
  } catch {
    return '';
  }
};

export const saveHandle = (handle: string) => {
  try {
    localStorage.setItem(KEY, handle);
  } catch {
    /* storage unavailable — prefill is a nicety */
  }
};
