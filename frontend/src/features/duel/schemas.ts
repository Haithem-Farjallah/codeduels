import { z } from 'zod';

const CF_PROBLEM_URL =
  /codeforces\.com\/(?:(?:contest|gym)\/\d+\/problem\/[A-Za-z]\d?|problemset\/problem\/\d+\/[A-Za-z]\d?)/;

const handle = z
  .string()
  .trim()
  .min(3, 'Enter your Codeforces handle')
  .max(24, 'Codeforces handles are at most 24 characters');

export const createDuelSchema = z
  .object({
    handle,
    problems: z
      .array(
        z.object({
          url: z
            .string()
            .trim()
            .min(1, 'Paste a problem link')
            .regex(CF_PROBLEM_URL, 'Not a Codeforces problem link'),
        }),
      )
      .min(1, 'Add at least one problem')
      .max(5, 'A duel can have at most 5 problems'),
    durationMinutes: z.number().int().min(10).max(180),
    startDelayMinutes: z.number().int().min(0).max(10),
  })
  .refine(
    (v) => new Set(v.problems.map((p) => p.url.trim())).size === v.problems.length,
    { message: 'The same problem appears more than once', path: ['problems'] },
  );

export type CreateDuelFormValues = z.infer<typeof createDuelSchema>;

export const joinDuelSchema = z.object({
  roomCode: z
    .string()
    .trim()
    .length(6, 'Room codes are 6 characters')
    .transform((v) => v.toUpperCase()),
  handle,
});

export type JoinDuelFormInput = z.input<typeof joinDuelSchema>;
export type JoinDuelFormValues = z.output<typeof joinDuelSchema>;
