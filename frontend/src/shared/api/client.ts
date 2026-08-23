import axios, { AxiosError, type AxiosRequestConfig } from "axios"
import type { StandardResponse } from "../types/api";
import type { LoginResponse } from "@/features/auth/types";
import { ENDPOINTS } from "./endpoints";


export const api=axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1',
    withCredentials:true
})

api.interceptors.request.use((config)=>{
    const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean };


    const isAuthEndpoint =
      original.url?.includes('/auth/login') ||
      original.url?.includes('/auth/register') ||
      original.url?.includes('/auth/refresh');

    if (error.response?.status === 401 && !original._retry && !isAuthEndpoint) {
      original._retry = true;                    

      try {
        const { data:response } = await axios.post<StandardResponse<LoginResponse>>(
          `${import.meta.env.VITE_API_URL}${ENDPOINTS.auth.refresh}`,
          {},
          { withCredentials: true }            
        );

        localStorage.setItem('accessToken', response.data.accessToken);
        original.headers!.Authorization = `Bearer ${response.data.accessToken}`;
        return api(original);                   
      } catch {
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);