import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { getErrorMessage } from '@/shared/api/errors';
import { cancelDuel, createDuel, joinDuel } from '../api/duelApi';
import { duelKeys } from './duelKeys';
import type { CreateDuelRequest, JoinDuelRequest } from '../types';

export const useCreateDuel = () => {
  const navigate = useNavigate();
  return useMutation({
    mutationFn: (payload: CreateDuelRequest) => createDuel(payload),
    onSuccess: (data) => navigate(`/duels/${data.duelId}`),
    onError: (error) => toast.error(getErrorMessage(error, 'Could not create the duel')),
  });
};

export const useJoinDuel = () => {
  const navigate = useNavigate();
  return useMutation({
    mutationFn: ({ roomCode, payload }: { roomCode: string; payload: JoinDuelRequest }) =>
      joinDuel(roomCode, payload),
    onSuccess: (data) => navigate(`/duels/${data.duelId}`),
    onError: (error) => toast.error(getErrorMessage(error, 'Could not join the duel')),
  });
};

export const useCancelDuel = (duelId: string) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => cancelDuel(duelId),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: duelKeys.detail(duelId) });
      toast.success('Duel canceled');
      navigate('/');
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Could not cancel the duel')),
  });
};
