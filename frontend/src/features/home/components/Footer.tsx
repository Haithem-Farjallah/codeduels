import { Link } from "react-router";

export const Footer = ()=>{
    return(
        <section className="rounded-2xl border border-white/10 bg-white/3 p-8 text-center">
          <h2 className="text-xl font-bold text-white">Track your progress</h2>
          <p className="mt-2 text-white/50">
            Sign in to see your match history, win rate and stats.
          </p>
          <Link
            to="/login"
            className="mt-6 inline-block rounded-lg bg-brand px-6 py-3 font-semibold text-white transition hover:bg-brand-light"
          >
            Login
          </Link>
        </section>
    )
}