import { AuthLayout } from '../components/AuthLayout';
import { LoginForm } from '../components/LoginForm';

export const LoginPage = () => (
  <AuthLayout heading="Welcome to" subheading="CodeDuels">
    <LoginForm />
  </AuthLayout>
);