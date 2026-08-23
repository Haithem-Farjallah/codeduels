import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { getCurrentUser } from '../api/authApi';

export const useAuth = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const token = localStorage.getItem('accessToken');

  const { data: user, isLoading } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
    enabled: !!token,          
    retry: false,             
    staleTime: Infinity,      
  });

  const logout = () => {
    localStorage.removeItem('accessToken');
    queryClient.clear();
    navigate('/login', { replace: true });
  };

  return {
    user,
    isAuthenticated: !!token && !!user,
    isLoading: !!token && isLoading,   
    logout,
  };
};