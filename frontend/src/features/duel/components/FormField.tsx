import type { ReactNode } from 'react';

export const inputClass =
  'w-full rounded-lg border border-white/10 bg-white/5 px-4 py-3 text-white placeholder:text-white/30 focus:border-brand-light focus:outline-none';

interface FormFieldProps {
  label: string;
  hint?: string;
  error?: string;
  children: ReactNode;
}

export const FormField = ({ label, hint, error, children }: FormFieldProps) => (
  <label className="block">
    <span className="text-sm font-medium text-white">{label}</span>
    {hint && <span className="mt-0.5 block text-xs text-white/50">{hint}</span>}
    <div className="mt-2">{children}</div>
    {error && <p className="mt-2 text-xs text-red-400">{error}</p>}
  </label>
);
