import type { ReactNode } from 'react';
import authBanner from '@/assets/auth_banner.png';

interface AuthLayoutProps {
  children: ReactNode;
  heading: string;
  subheading: string;
}

export const AuthLayout = ({ children, heading, subheading }: AuthLayoutProps) => {
  return (
    <div className="flex h-screen">

      <div className="flex w-full flex-col justify-center px-8 md:w-[44%] md:px-16 bg-dark">
        {children}
      </div>

      <div className="relative hidden md:flex md:w-[56%] flex-col overflow-hidden bg-brand bg-primary">

        <div className="pointer-events-none absolute -right-32 -top-32 h-125 w-125 rounded-full bg-white/10" />
        <div className="pointer-events-none absolute -bottom-40 -left-20 h-105 w-105 rounded-full bg-white/5" />

        <div className="relative z-10 px-16 pt-10">
          <h1 className="text-6xl font-bold leading-tight text-white">
            {heading}
            <span className="block font-light">{subheading}</span>
          </h1>
          <p className="mt-4 text-white/80">Login to access your account</p>
        </div>

        <div className="relative z-10 mt-auto flex justify-center px-16 pb-12">
          <img src={authBanner} alt="" className="max-h-125 w-auto" />
        </div>
      </div>
    </div>
  );
};