import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { loginSchema, type LoginFormValues } from '../schemas';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { login } from '../api/authApi';
import { toast } from 'sonner';
import { getErrorMessage } from '@/shared/api/errors';

export const LoginForm = () => {
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  const { mutate, isPending } = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      localStorage.setItem('accessToken', data.accessToken);
      navigate('/');
    },
    onError: (error) => toast.error(getErrorMessage(error,"Failed to login, please try again later"))
  });

  return (
    <div className="mx-auto w-full max-w-sm">

      <h2 className="text-4xl font-bold text-white">Login</h2>
      <p className="mt-3 text-sm text-white/50">Enter your account details</p>

      <form onSubmit={handleSubmit((values) => mutate(values))} className="mt-12 space-y-8">

        <div>
          <input
            {...register('email')}
            type="email"
            placeholder="Email"
            className="w-full border-0 border-b border-white/25 bg-transparent pb-2
                       text-white placeholder:text-white/50
                       focus:border-brand-light focus:outline-none"
          />
          {errors.email && (
            <p className="mt-2 text-xs text-red-400">{errors.email.message}</p>
          )}
        </div>

        <div className="relative">
          <input
          {...register('password')}
            type={showPassword ? 'text' : 'password'}
            placeholder="Password"
            className="w-full border-0 border-b border-white/25 bg-transparent pb-2 pr-8
                       text-white placeholder:text-white/50
                       focus:border-brand-light focus:outline-none"
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-0 top-0 text-white/50 hover:text-white/80 "
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
          {errors.password && (
            <p className="mt-2 text-xs text-red-400">{errors.password.message}</p>
          )}
        </div>

        <Link to="/forgot-password" className="block text-sm text-white/50 hover:text-white/80">
          Forgot Password?
        </Link>

        <Button type="submit" disabled={isPending} className="h-12 w-full bg-brand text-white hover:bg-brand-light bg-primary cursor-pointer">
          {isPending ? 'Signing in…' : 'Login'}
        </Button>
      </form>

      <div className="mt-32 flex items-center justify-between ">
        <span className="text-sm text-white/50">Don't have an account?</span>
        <Link
          to="/register"
          className="rounded-md bg-white/10 px-5 py-2.5 text-sm text-white hover:bg-white/15"
        >
          Sign up
        </Link>
      </div>
    </div>
  );
};