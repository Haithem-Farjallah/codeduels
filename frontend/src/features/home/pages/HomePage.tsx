import { Swords, Code2, Timer, Zap } from 'lucide-react';
import { useAuth } from '@/features/auth';
import { Hero } from '../components/Hero';
import { ModeCard } from '../components/ModeCard';
import { Footer } from '../components/Footer';


export const HomePage = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="space-y-12 pb-20">
      <Hero />

      <section className="grid gap-6 lg:grid-cols-2">
        <ModeCard
          badge={{ icon: Timer, label: 'SPEED RUN' }}
          icon={Code2}
          title="Standard Match"
          description={<>A rapid 1v1 on a single problem from our library. <span className="text-white/70">Sudden death</span> — the first correct solution wins.</>}
          features={[
            'Single problem, our library',
            'First accepted answer ends the match',
            'Wrong answers add a time penalty',
          ]}
          actions={[
            { label: 'Create Match', to: '/matches/new', variant: 'primary' },
            { label: 'Join Match', to: '/matches/join', variant: 'secondary' },
          ]}
        />

        <ModeCard
          badge={{ icon: Zap, label: 'COMING SOON' }}
          icon={Swords}
          title="Codeforces Duel"
          description={<>Battle on problems from Codeforces with <span className="text-white/70">ICPC scoring</span> — points plus time penalty.</>}
          features={[
            'Multiple problems per duel',
            'Official ICPC penalty rules',
            'Submit directly on Codeforces',
          ]}
          disabled
        />
      </section>

      {!isAuthenticated && <Footer />}
    </div>
  );
};