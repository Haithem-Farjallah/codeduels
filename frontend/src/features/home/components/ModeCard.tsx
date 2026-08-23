import { Link } from 'react-router';
import { CheckCircle2, type LucideIcon } from 'lucide-react';

interface ModeCardProps {
  badge: { icon: LucideIcon; label: string };
  icon: LucideIcon;
  title: string;
  description: React.ReactNode;
  features: string[];
  actions?: { label: string; to: string; variant: 'primary' | 'secondary' }[];
  disabled?: boolean;
}

export const ModeCard = ({
  badge, icon: Icon, title, description, features, actions, disabled = false,
}: ModeCardProps) => {
  const BadgeIcon = badge.icon;

  return (
    <div className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/3 p-8">
      <Icon className="pointer-events-none absolute -right-6 -top-6 text-white/4" size={140} />

      <div className={`relative z-10 ${disabled ? 'opacity-60' : ''}`}>
        <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${
          disabled ? 'bg-white/10 text-white/60' : 'bg-brand/15 text-brand-light'
        }`}>
          <BadgeIcon size={12} /> {badge.label}
        </span>

        <h2 className="mt-4 flex items-center gap-3 text-2xl font-bold text-white">
          <Icon className={disabled ? 'text-white/60' : 'text-brand'} size={26} /> {title}
        </h2>

        <p className="mt-2 min-h-12 text-white/50">{description}</p>

        <ul className="mt-6 space-y-2 text-sm text-white/70">
          {features.map((feature) => (
            <li key={feature} className="flex items-center gap-2">
              <CheckCircle2 size={16} className={disabled ? 'text-white/30' : 'text-brand'} />
              {feature}
            </li>
          ))}
        </ul>

        <div className="mt-8 flex flex-col gap-3 sm:flex-row">
          {disabled ? (
            <button disabled className="w-full cursor-not-allowed rounded-lg bg-white/5 px-6 py-3 font-semibold text-white/40">
              Not available yet
            </button>
          ) : (
            actions?.map((action) => (
              <Link
                key={action.to}
                to={action.to}
                className={`flex-1 rounded-lg px-6 py-3 text-center font-semibold text-white transition ${
                  action.variant === 'primary'
                    ? 'bg-brand hover:bg-brand-light'
                    : 'bg-white/10 hover:bg-white/15'
                }`}
              >
                {action.label}
              </Link>
            ))
          )}
        </div>
      </div>
    </div>
  );
};