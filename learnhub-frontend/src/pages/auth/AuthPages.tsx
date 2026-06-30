import React, { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import Skeleton from '@mui/material/Skeleton';
import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import MarkEmailReadOutlinedIcon from '@mui/icons-material/MarkEmailReadOutlined';
import ShieldOutlinedIcon from '@mui/icons-material/ShieldOutlined';
import apiClient from '../../lib/api';
import { Input } from '../../ui-kit/Input';
import { Button } from '../../ui-kit/Button';
import { useAuthStore } from '../../stores/authStore';
import type { AuthResponse, AuthUser, UserRole } from '../../types/auth';

type AuthMode = 'login' | 'register' | 'verify';

interface ApiErrorBody {
  error_code?: string;
  message?: string;
  details?: {
    errors?: Record<string, string>;
  };
}

function getApiMessage(error: unknown, fallback: string): string {
  const maybe = error as { response?: { data?: ApiErrorBody } };
  return maybe.response?.data?.message || fallback;
}

function normalizeRole(role?: string): UserRole {
  const value = role?.toLowerCase();
  if (value === 'admin' || value === 'instructor' || value === 'student') {
    return value;
  }
  return 'student';
}

function getInitialEmail(search: string): string {
  return new URLSearchParams(search).get('email') || '';
}

const authCopy: Record<AuthMode, { title: string; subtitle: string }> = {
  login: {
    title: 'Đăng nhập LearnHub',
    subtitle: 'Tiếp tục lộ trình học và mở khóa đánh giá kỹ năng của bạn.',
  },
  register: {
    title: 'Tạo tài khoản mới',
    subtitle: 'Đăng ký trong vài bước, sau đó nhập OTP được gửi qua Gmail.',
  },
  verify: {
    title: 'Kích hoạt tài khoản',
    subtitle: 'Nhập mã OTP 6 số đã được gửi tới email đăng ký của bạn.',
  },
};

const panelVariants = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -10 },
};

export const AuthShell: React.FC<{ mode: AuthMode }> = ({ mode }) => {
  const copy = authCopy[mode];
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setBooting(false), 280);
    return () => window.clearTimeout(timer);
  }, [mode]);

  return (
    <section className="min-h-[calc(100vh-66px)] bg-lh-surface">
      <div className="mx-auto grid min-h-[calc(100vh-66px)] max-w-6xl grid-cols-1 lg:grid-cols-[0.95fr_1.05fr]">
        <aside className="hidden bg-lh-dark px-10 py-12 text-white lg:flex lg:flex-col lg:justify-between">
          <div>
            <div className="mb-10 inline-flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded bg-lh-pink">
                <ShieldOutlinedIcon fontSize="small" />
              </div>
              <span className="text-sm font-extrabold uppercase tracking-[0.18em] text-white/70">Secure access</span>
            </div>
            <motion.h1
              key={copy.title}
              initial={{ opacity: 0, y: 18 }}
              animate={{ opacity: 1, y: 0 }}
              className="max-w-md text-4xl font-black leading-tight"
            >
              {copy.title}
            </motion.h1>
            <p className="mt-5 max-w-sm text-base leading-7 text-[#C7C9E4]">{copy.subtitle}</p>
          </div>

          <div className="grid gap-3">
            {[
              'OTP Gmail bảo vệ tài khoản mới',
              'JWT session cho các API học tập',
              'Rate limit chống spam và brute force',
            ].map((item, index) => (
              <motion.div
                key={item}
                initial={{ opacity: 0, x: -12 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.1 + index * 0.08 }}
                className="flex items-center gap-3 rounded bg-white/8 px-4 py-3 text-sm font-semibold text-white/85"
              >
                <MarkEmailReadOutlinedIcon fontSize="small" className="text-lh-pink" />
                {item}
              </motion.div>
            ))}
          </div>
        </aside>

        <main className="flex items-center justify-center px-4 py-10 sm:px-6 lg:px-10">
          <div className="w-full max-w-[460px] rounded-lg border border-lh-border bg-white p-6 shadow-clay-md sm:p-8">
            {booting ? (
              <AuthSkeleton />
            ) : (
              <AnimatePresence mode="wait">
                <motion.div
                  key={mode}
                  variants={panelVariants}
                  initial="initial"
                  animate="animate"
                  exit="exit"
                  transition={{ duration: 0.22 }}
                >
                  {mode === 'login' && <LoginPanel />}
                  {mode === 'register' && <RegisterPanel />}
                  {mode === 'verify' && <VerifyPanel />}
                </motion.div>
              </AnimatePresence>
            )}
          </div>
        </main>
      </div>
    </section>
  );
};

const AuthSkeleton = () => (
  <div>
    <Skeleton variant="text" width="70%" height={38} />
    <Skeleton variant="text" width="92%" height={24} />
    <div className="mt-8 space-y-4">
      <Skeleton variant="rounded" height={46} />
      <Skeleton variant="rounded" height={46} />
      <Skeleton variant="rounded" height={48} />
    </div>
  </div>
);

const FieldIcon: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <span className="inline-flex h-9 w-9 items-center justify-center rounded bg-lh-surface text-lh-navy">
    {children}
  </span>
);

const Header: React.FC<{ mode: AuthMode }> = ({ mode }) => (
  <div className="mb-7">
    <div className="mb-4 inline-flex items-center gap-2 rounded bg-lh-surface px-3 py-2 text-xs font-extrabold uppercase tracking-[0.16em] text-lh-navy">
      <MarkEmailReadOutlinedIcon fontSize="small" />
      LearnHub Auth
    </div>
    <h2 className="text-2xl font-black text-lh-dark">{authCopy[mode].title}</h2>
    <p className="mt-2 text-sm leading-6 text-lh-muted">{authCopy[mode].subtitle}</p>
  </div>
);

const LoginPanel = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((state) => state.login);
  const [email, setEmail] = useState(getInitialEmail(location.search));
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await apiClient.post<AuthResponse>('/v1/auth/login', { email, password });
      const user: AuthUser = {
        id: data.userId,
        email: data.email,
        firstName: '',
        lastName: '',
        role: normalizeRole(data.role),
      };
      login({ ...data, user });
      navigate('/courses');
    } catch (err) {
      const code = (err as { response?: { data?: ApiErrorBody } }).response?.data?.error_code;
      if (code === 'EMAIL_NOT_VERIFIED') {
        navigate(`/verify-email?email=${encodeURIComponent(email)}`);
        return;
      }
      setError(getApiMessage(err, 'Không thể đăng nhập. Kiểm tra lại email và mật khẩu.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      <Header mode="login" />
      {error && <div className="mb-4 rounded border border-error-500 bg-error-50 px-4 py-3 text-sm font-semibold text-error-600">{error}</div>}
      <div className="space-y-4">
        <div className="flex gap-3">
          <FieldIcon><EmailOutlinedIcon fontSize="small" /></FieldIcon>
          <Input label="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="flex gap-3">
          <FieldIcon><LockOutlinedIcon fontSize="small" /></FieldIcon>
          <Input label="Mật khẩu" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
      </div>
      <Button className="mt-6 bg-lh-pink hover:bg-lh-pink-dark" size="lg" fullWidth loading={loading}>
        Đăng nhập
      </Button>
      <p className="mt-5 text-center text-sm text-lh-muted">
        Chưa có tài khoản? <Link className="font-extrabold text-lh-navy" to="/register">Đăng ký</Link>
      </p>
    </form>
  );
};

const RegisterPanel = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await apiClient.post('/v1/auth/register', form);
      navigate(`/verify-email?email=${encodeURIComponent(form.email)}`);
    } catch (err) {
      setError(getApiMessage(err, 'Không thể tạo tài khoản. Vui lòng kiểm tra thông tin.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      <Header mode="register" />
      {error && <div className="mb-4 rounded border border-error-500 bg-error-50 px-4 py-3 text-sm font-semibold text-error-600">{error}</div>}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="flex gap-3">
          <FieldIcon><PersonOutlineOutlinedIcon fontSize="small" /></FieldIcon>
          <Input label="Tên" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
        </div>
        <Input label="Họ" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
      </div>
      <div className="mt-4 space-y-4">
        <div className="flex gap-3">
          <FieldIcon><EmailOutlinedIcon fontSize="small" /></FieldIcon>
          <Input label="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
        </div>
        <div className="flex gap-3">
          <FieldIcon><LockOutlinedIcon fontSize="small" /></FieldIcon>
          <Input
            label="Mật khẩu"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            showStrength
            hint="Tối thiểu 12 ký tự, nên có chữ hoa, số và ký tự đặc biệt."
            required
          />
        </div>
      </div>
      <Button className="mt-6 bg-lh-pink hover:bg-lh-pink-dark" size="lg" fullWidth loading={loading}>
        Tạo tài khoản
      </Button>
      <p className="mt-5 text-center text-sm text-lh-muted">
        Đã có tài khoản? <Link className="font-extrabold text-lh-navy" to="/login">Đăng nhập</Link>
      </p>
    </form>
  );
};

const VerifyPanel = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const inputRefs = useRef<Array<HTMLInputElement | null>>([]);
  const [email, setEmail] = useState(getInitialEmail(location.search));
  const [digits, setDigits] = useState(['', '', '', '', '', '']);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const otp = useMemo(() => digits.join(''), [digits]);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setTimeout(() => setCooldown((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [cooldown]);

  const setDigit = (index: number, value: string) => {
    const clean = value.replace(/\D/g, '').slice(-1);
    const next = [...digits];
    next[index] = clean;
    setDigits(next);
    if (clean && index < 5) inputRefs.current[index + 1]?.focus();
  };

  const onPaste = (event: React.ClipboardEvent<HTMLInputElement>) => {
    const clean = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (clean.length < 2) return;
    event.preventDefault();
    setDigits(clean.padEnd(6, '').split('').slice(0, 6));
    inputRefs.current[Math.min(clean.length, 6) - 1]?.focus();
  };

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setMessage('');
    if (otp.length !== 6) {
      setError('Vui lòng nhập đủ 6 số OTP.');
      return;
    }
    setLoading(true);
    try {
      await apiClient.post('/v1/auth/verify-email-otp', { email, otp });
      setMessage('Tài khoản đã được kích hoạt. Đang chuyển tới đăng nhập...');
      window.setTimeout(() => navigate(`/login?email=${encodeURIComponent(email)}`), 700);
    } catch (err) {
      setError(getApiMessage(err, 'OTP không hợp lệ hoặc đã hết hạn.'));
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    setError('');
    setMessage('');
    setResending(true);
    try {
      await apiClient.post('/v1/auth/resend-verification-otp', { email });
      setCooldown(60);
      setDigits(['', '', '', '', '', '']);
      setMessage('Nếu email đang chờ xác thực, mã OTP mới đã được gửi.');
    } catch (err) {
      setError(getApiMessage(err, 'Không thể gửi lại OTP lúc này.'));
    } finally {
      setResending(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      <Header mode="verify" />
      {message && <div className="mb-4 rounded border border-success-500 bg-success-50 px-4 py-3 text-sm font-semibold text-success-600">{message}</div>}
      {error && <div className="mb-4 rounded border border-error-500 bg-error-50 px-4 py-3 text-sm font-semibold text-error-600">{error}</div>}
      <Input label="Email đăng ký" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <div className="mt-5">
        <label className="mb-2 block text-sm font-medium text-gray-700">Mã OTP</label>
        <div className="grid grid-cols-6 gap-2">
          {digits.map((digit, index) => (
            <motion.input
              key={index}
              ref={(node) => { inputRefs.current[index] = node; }}
              value={digit}
              onChange={(e) => setDigit(index, e.target.value)}
              onPaste={onPaste}
              onKeyDown={(e) => {
                if (e.key === 'Backspace' && !digits[index] && index > 0) inputRefs.current[index - 1]?.focus();
              }}
              whileFocus={{ scale: 1.04 }}
              className="h-12 rounded-lg border border-lh-input text-center text-lg font-black text-lh-dark outline-none focus:border-lh-pink focus:ring-2 focus:ring-lh-pink/25"
              inputMode="numeric"
              maxLength={1}
              aria-label={`OTP digit ${index + 1}`}
            />
          ))}
        </div>
      </div>
      <Button className="mt-6 bg-lh-pink hover:bg-lh-pink-dark" size="lg" fullWidth loading={loading}>
        Kích hoạt tài khoản
      </Button>
      <button
        type="button"
        onClick={resend}
        disabled={resending || cooldown > 0 || !email}
        className="mt-4 w-full rounded border border-lh-border px-4 py-3 text-sm font-extrabold text-lh-navy transition-colors hover:bg-lh-surface disabled:cursor-not-allowed disabled:opacity-50"
      >
        {resending ? 'Đang gửi lại...' : cooldown > 0 ? `Gửi lại sau ${cooldown}s` : 'Gửi lại mã OTP'}
      </button>
      <p className="mt-5 text-center text-sm text-lh-muted">
        Đã kích hoạt? <Link className="font-extrabold text-lh-navy" to="/login">Đăng nhập</Link>
      </p>
    </form>
  );
};

export const LoginPage = () => <AuthShell mode="login" />;
export const RegisterPage = () => <AuthShell mode="register" />;
export const VerifyEmailPage = () => <AuthShell mode="verify" />;
