import { AxiosError } from 'axios';
import type { StandardResponse } from '@/shared/types/api';

export const getErrorMessage = (error: unknown, fallback = 'Something went wrong'): string => {
  if (error instanceof AxiosError) {
    console.log(error)
    const body = error.response?.data as StandardResponse<unknown> | undefined;
    if (body?.message) return body.message;
    if (!error.response) return 'Cannot reach the server';
  }
  return fallback;
};