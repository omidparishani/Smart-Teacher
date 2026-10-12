import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { ApiError, prefetchCsrf } from '../../lib/api-client';
import { useAuthStore } from '../../stores/auth.store';
import { login } from '../admin/api';

export function LoginPage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);
  const setSession = useAuthStore((s) => s.setSession);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  if (user || accessToken) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setPending(true);
    try {
      const response = await login(username.trim(), password);
      setSession(response.accessToken, response.refreshToken, response.user);
      await prefetchCsrf();
      void navigate('/', { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('ورود انجام نشد. دوباره تلاش کنید.');
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <form
        onSubmit={(e) => void onSubmit(e)}
        className="w-full max-w-md border border-line bg-white/90 p-8 shadow-sm backdrop-blur"
      >
        <p className="text-sm font-medium text-primary">Smart Teacher</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">ورود به پنل مدیریت</h1>
        <p className="mt-2 text-sm text-muted">نام کاربری و رمز عبور ادمین را وارد کنید.</p>

        <label className="mt-6 block text-sm">
          نام کاربری
          <input
            className="mt-1 w-full border border-line bg-surface px-3 py-2 outline-none focus:border-primary"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </label>

        <label className="mt-4 block text-sm">
          رمز عبور
          <input
            type="password"
            className="mt-1 w-full border border-line bg-surface px-3 py-2 outline-none focus:border-primary"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>

        {error ? (
          <p className="mt-4 text-sm text-danger" role="alert">
            {error}
          </p>
        ) : null}

        <button
          type="submit"
          disabled={pending}
          className="mt-6 w-full bg-primary px-4 py-2.5 text-sm font-medium text-white disabled:opacity-60"
        >
          {pending ? 'در حال ورود…' : 'ورود'}
        </button>
      </form>
    </div>
  );
}
