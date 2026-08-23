import {z} from "zod"

export const loginSchema= z.object({
    email: z.string().min(1, 'Email is required').email('Enter a valid email').max(100,"email max reached"),
    password: z.string().min(5, 'Password is required').max(16,"password max reached"),
})

export type LoginFormValues = z.infer<typeof loginSchema>;
