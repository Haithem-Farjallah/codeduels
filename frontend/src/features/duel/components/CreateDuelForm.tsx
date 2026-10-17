import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { createDuelSchema, type CreateDuelFormValues } from '../schemas';
import { useCreateDuel } from '../hooks/useDuelMutations';
import { FormField, inputClass } from './FormField';
import { loadHandle, saveHandle } from './handleStorage';

const DURATIONS = [30, 45, 60, 90, 120, 180];
const DELAYS = [0, 1, 2, 5, 10];
const MAX_PROBLEMS = 5;

export const CreateDuelForm = () => {
  const { mutate, isPending } = useCreateDuel();

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateDuelFormValues>({
    resolver: zodResolver(createDuelSchema),
    defaultValues: {
      handle: loadHandle(),
      problems: [{ url: '' }],
      durationMinutes: 60,
      startDelayMinutes: 1,
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: 'problems' });

  const onSubmit = (values: CreateDuelFormValues) => {
    saveHandle(values.handle);
    mutate({
      handle: values.handle,
      problemUrls: values.problems.map((p) => p.url),
      durationMinutes: values.durationMinutes,
      startDelayMinutes: values.startDelayMinutes,
    });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <FormField label="Your Codeforces handle" hint="Submissions from this handle count for you." error={errors.handle?.message}>
        <input {...register('handle')} placeholder="tourist" autoComplete="off" className={inputClass} />
      </FormField>

      <div>
        <span className="text-sm font-medium text-white">Problems</span>
        <span className="mt-0.5 block text-xs text-white/50">
          Paste links like codeforces.com/contest/2271/problem/E — up to {MAX_PROBLEMS}.
        </span>
        <div className="mt-2 space-y-2">
          {fields.map((field, index) => (
            <div key={field.id}>
              <div className="flex gap-2">
                <span className="flex w-10 shrink-0 items-center justify-center rounded-lg bg-brand/15 font-bold text-brand-light">
                  {String.fromCharCode(65 + index)}
                </span>
                <input
                  {...register(`problems.${index}.url`)}
                  placeholder="https://codeforces.com/contest/…/problem/…"
                  autoComplete="off"
                  className={inputClass}
                />
                {fields.length > 1 && (
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    className="shrink-0 rounded-lg px-3 text-white/50 hover:bg-white/5 hover:text-white"
                    aria-label={`Remove problem ${String.fromCharCode(65 + index)}`}
                  >
                    <Trash2 size={16} />
                  </button>
                )}
              </div>
              {errors.problems?.[index]?.url && (
                <p className="mt-1 text-xs text-red-400">{errors.problems[index]?.url?.message}</p>
              )}
            </div>
          ))}
        </div>
        {errors.problems?.root && <p className="mt-2 text-xs text-red-400">{errors.problems.root.message}</p>}
        {errors.problems?.message && <p className="mt-2 text-xs text-red-400">{errors.problems.message}</p>}
        {fields.length < MAX_PROBLEMS && (
          <button
            type="button"
            onClick={() => append({ url: '' })}
            className="mt-3 inline-flex items-center gap-2 text-sm text-brand-light hover:text-white"
          >
            <Plus size={16} /> Add problem
          </button>
        )}
      </div>

      <div className="grid gap-6 sm:grid-cols-2">
        <FormField label="Duration" error={errors.durationMinutes?.message}>
          <select {...register('durationMinutes', { setValueAs: Number })} className={inputClass}>
            {DURATIONS.map((m) => (
              <option key={m} value={m} className="bg-surface">
                {m} minutes
              </option>
            ))}
          </select>
        </FormField>
        <FormField label="Start delay" hint="Countdown after your opponent joins." error={errors.startDelayMinutes?.message}>
          <select {...register('startDelayMinutes', { setValueAs: Number })} className={inputClass}>
            {DELAYS.map((m) => (
              <option key={m} value={m} className="bg-surface">
                {m === 0 ? 'Start immediately' : `${m} minute${m > 1 ? 's' : ''}`}
              </option>
            ))}
          </select>
        </FormField>
      </div>

      <Button type="submit" disabled={isPending} className="h-12 w-full cursor-pointer bg-brand text-white hover:bg-brand-light">
        {isPending ? 'Checking handle and problems…' : 'Create duel'}
      </Button>
    </form>
  );
};
