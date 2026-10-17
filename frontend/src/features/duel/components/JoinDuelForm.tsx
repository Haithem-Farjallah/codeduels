import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { joinDuelSchema, type JoinDuelFormInput, type JoinDuelFormValues } from '../schemas';
import { useJoinDuel } from '../hooks/useDuelMutations';
import { FormField, inputClass } from './FormField';
import { loadHandle, saveHandle } from './handleStorage';

export const JoinDuelForm = ({ initialCode = '' }: { initialCode?: string }) => {
  const { mutate, isPending } = useJoinDuel();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<JoinDuelFormInput, unknown, JoinDuelFormValues>({
    resolver: zodResolver(joinDuelSchema),
    defaultValues: { roomCode: initialCode.toUpperCase(), handle: loadHandle() },
  });

  const onSubmit = (values: JoinDuelFormValues) => {
    saveHandle(values.handle);
    mutate({ roomCode: values.roomCode, payload: { handle: values.handle } });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <FormField label="Room code" error={errors.roomCode?.message}>
        <input
          {...register('roomCode')}
          placeholder="ABC123"
          maxLength={6}
          autoComplete="off"
          className={`${inputClass} font-mono text-lg tracking-[0.3em] uppercase`}
        />
      </FormField>

      <FormField label="Your Codeforces handle" hint="Submissions from this handle count for you." error={errors.handle?.message}>
        <input {...register('handle')} placeholder="tourist" autoComplete="off" className={inputClass} />
      </FormField>

      <Button type="submit" disabled={isPending} className="h-12 w-full cursor-pointer bg-brand text-white hover:bg-brand-light">
        {isPending ? 'Joining…' : 'Join duel'}
      </Button>
    </form>
  );
};
