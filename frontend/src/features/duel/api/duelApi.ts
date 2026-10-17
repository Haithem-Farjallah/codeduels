import { api } from '@/shared/api/client';
import { ENDPOINTS } from '@/shared/api/endpoints';
import type { StandardResponse } from '@/shared/types/api';
import type {
  CreateDuelRequest,
  CreateDuelResponse,
  DuelHistoryItem,
  DuelState,
  JoinDuelRequest,
  JoinDuelResponse,
  Page,
} from '../types';

export const createDuel = async (payload: CreateDuelRequest): Promise<CreateDuelResponse> => {
  const { data: result } = await api.post<StandardResponse<CreateDuelResponse>>(ENDPOINTS.duels.create, payload);
  return result.data;
};

export const joinDuel = async (roomCode: string, payload: JoinDuelRequest): Promise<JoinDuelResponse> => {
  const { data: result } = await api.post<StandardResponse<JoinDuelResponse>>(ENDPOINTS.duels.join(roomCode), payload);
  return result.data;
};

export const getDuelState = async (duelId: string): Promise<DuelState> => {
  const { data: result } = await api.get<StandardResponse<DuelState>>(ENDPOINTS.duels.state(duelId));
  return result.data;
};

export const cancelDuel = async (duelId: string): Promise<void> => {
  await api.delete(ENDPOINTS.duels.cancel(duelId));
};

export const getDuelHistory = async (page: number): Promise<Page<DuelHistoryItem>> => {
  const { data: result } = await api.get<StandardResponse<Page<DuelHistoryItem>>>(ENDPOINTS.duels.history, {
    params: { page, size: 10 },
  });
  return result.data;
};
