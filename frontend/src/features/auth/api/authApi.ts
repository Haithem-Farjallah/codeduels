import { api } from '@/shared/api/client';
import { ENDPOINTS } from '@/shared/api/endpoints';
import type { StandardResponse } from '@/shared/types/api';
import type { LoginRequest, LoginResponse, User } from '../types';


export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  const { data:result } = await api.post<StandardResponse<LoginResponse>>(ENDPOINTS.auth.login, payload);
  return result.data;             
};

export const getCurrentUser = async():Promise<User>=>{
  const { data:result } = await api.get<StandardResponse<User>>(ENDPOINTS.users.me);
  return result.data; 
}