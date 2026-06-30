import React, { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import Skeleton from '@mui/material/Skeleton';
import AlternateEmailRoundedIcon from '@mui/icons-material/AlternateEmailRounded';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded';
import LockRoundedIcon from '@mui/icons-material/LockRounded';
import MailLockRoundedIcon from '@mui/icons-material/MailLockRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import ReplayRoundedIcon from '@mui/icons-material/ReplayRounded';
import SchoolRoundedIcon from '@mui/icons-material/SchoolRounded';
import ShieldRoundedIcon from '@mui/icons-material/ShieldRounded';
import apiClient from '../../lib/api';
import { Badge, Button, Input, ProgressBar } from '../../ui-kit';
import { cn } from '../../lib/cn';
import { useAuthStore } from '../../stores/authStore';
import type { AuthResponse, AuthUser, UserRole } from '../../types/auth';

type AuthMode = 'login' | 'register' | 'verify';

interface ApiErrorBody {
  error_code?: string;
  message?: string;
}

const authCopy: Record<AuthMode, { eyebrow: string; title: string; subtitle: string }> = {
  login: {
    eyebrow: 'Secure sign in',
    title: 'Chào mừng quay lại',
    subtitle: 'Đăng nhập để tiếp tục lộ trình học, đánh giá kỹ năng và quản lý tiến độ.',
  },
  register: {
    eyebrow: 'Create account',
    title: 'Bắt đầu với LearnHub',
    subtitle: 'Tạo tài khoản, nhận OTP qua Gmail và kích hoạt trước khi đăng nhập.',
  },
  verify: {
    eyebrow: 'Email verification',
    title: 'Xác thực tài khoản',
    subtitle: 'Nhập mã OTP 6 số được gửi đến email của bạn để hoàn tất kích hoạt.',
  },
};

const featureItems = [
  { icon: <ShieldRoundedIcon fontSize="small" />, label: 'OTP qua queue có retry' },
  { icon: <SchoolRoundedIcon fontSize="small" />, label: 'Theo dõi lộ trình học' },
  { icon: <CheckCircleRoundedIcon fontSize="small" />, label: 'Session bảo mật bằng JWT' },
];

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

const viewMotion = {
  initial: { opacity: 0, y: 12, scale: 0.985 },
  animate: { opacity: 1, y: 0, scale: 1 },
  exit: { opacity: 0, y: -8, scale: 0.99 },
};

export const AuthShell: React.FC<{ mode: AuthMode }> = ({ mode }) => {
  const copy = authCopy[mode];
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setBooting(false), 260);
    return () => window.clearTimeout(timer);
  }, [mode]);

  return (
    <section className="min-h-[calc(100vh-66px)] overflow-hidden bg-[#F7F8FC]">
      <div className="mx-auto grid min-h-[calc(100vh-66px)] max-w-7xl grid-cols-1 lg:grid-cols-[minmax(420px,0.92fr)_minmax(520px,1.08fr)]">
        <aside className="relative hidden border-r border-lh-border bg-lh-dark px-10 py-10 text-white lg:flex lg:flex-col lg:justify-between">
          <div className="absolute inset-x-0 top-0 h-1 bg-lh-pink" />
          <div>
            <div className="mb-12 flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-lh-pink text-white shadow-clay-sm">
                <SchoolRoundedIcon />
              </div>
              <div>
                <div className="text-lg font-black leading-tight">LearnHub</div>
                <div className="text-xs font-bold uppercase tracking-[0.18em] text-white/50">LMS Workspace</div>
              </div>
            </div>

            <motion.div
              key={mode}
              initial={{ opacity: 0, y: 18 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.28 }}
            >
              <Badge variant="info" className="mb-5 border border-white/10 bg-white/10 text-white">
                {copy.eyebrow}
              </Badge>
              <h1 className="max-w-[440px] text-[42px] font-black leading-[1.05] tracking-normal">
                {copy.title}
              </h1>
              <p className="mt-5 max-w-[390px] text-base leading-7 text-[#D9DBF1]">
                {copy.subtitle}
              </p>
            </motion.div>
          </div>

          <div className="space-y-3">
            {featureItems.map((item, index) => (
              <motion.div
                key={item.label}
                initial={{ opacity: 0, x: -16 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.1 + index * 0.08 }}
                className="flex items-center gap-3 rounded-lg border border-white/10 bg-white/10 px-4 py-3 text-sm font-bold text-white/85"
              >
                <span className="flex h-9 w-9 items-center justify-center rounded-md bg-white/10 text-lh-pink">
                  {item.icon}
                </span>
                {item.label}
              </motion.div>
            ))}
          </div>
        </aside>

        <main className="flex items-center justify-center px-4 py-8 sm:px-6 lg:px-12">
          <div className="w-full max-w-[520px]">
            <div className="mb-6 flex items-center justify-between lg:hidden">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-lh-dark text-white">
                  <SchoolRoundedIcon fontSize="small" />
                </div>
                <div className="text-base font-black text-lh-dark">LearnHub</div>
              </div>
              <Badge variant="info">{copy.eyebrow}</Badge>
            </div>

            <div className="rounded-lg border border-lh-border bg-white p-5 shadow-clay-md sm:p-7">
              {booting ? (
                <AuthSkeleton />
              ) : (
                <AnimatePresence mode="wait">
                  <motion.div
                    key={mode}
                    variants={viewMotion}
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
          </div>
        </main>
      </div>
    </section>
  );
};

const AuthSkeleton = () => (
  <div>
    <Skeleton variant="rounded" width={116} height={28} />
    <Skeleton variant="text" width="78%" height={46} className="mt-4" />
    <Skeleton variant="text" width="94%" height={24} />
    <div className="mt-8 space-y-4">
      <Skeleton variant="rounded" height={52} />
      <Skeleton variant="rounded" height={52} />
      <Skeleton variant="rounded" height={52} />
      <Skeleton variant="rounded" height={50} />
    </div>
  </div>
);

const FormHeader: React.FC<{ mode: AuthMode }> = ({ mode }) => (
  <div className="mb-7">
    <Badge variant="info" className="mb-4 bg-lh-surface text-lh-navy">
      {authCopy[mode].eyebrow}
    </Badge>
    <h2 className="text-2xl font-black leading-tight text-lh-dark sm:text-3xl">
      {authCopy[mode].title}
    </h2>
    <p className="mt-2 text-sm leading-6 text-lh-muted">{authCopy[mode].subtitle}</p>
  </div>
);

const IconInput: React.FC<React.ComponentProps<typeof Input> & { icon: React.ReactNode }> = ({
  icon,
  className,
  ...props
}) => (
  <div className="relative">
    <span className="pointer-events-none absolute left-3 top-[34px] z-10 flex h-8 w-8 items-center justify-center rounded-md bg-lh-surface text-lh-navy">
      {icon}
    </span>
    <Input className={cn('h-12 pl-14', className)} {...props} />
  </div>
);

const Alert: React.FC<{ tone: 'error' | 'success'; children: React.ReactNode }> = ({ tone, children }) => (
  <motion.div
    initial={{ opacity: 0, y: -6 }}
    animate={{ opacity: 1, y: 0 }}
    className={cn(
      'mb-4 rounded-lg border px-4 py-3 text-sm font-semibold',
      tone === 'error'
        ? 'border-error-500 bg-error-50 text-error-600'
        : 'border-success-500 bg-success-50 text-success-600'
    )}
  >
    {children}
  </motion.div>
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
      <FormHeader mode="login" />
      {error && <Alert tone="error">{error}</Alert>}

      <div className="space-y-4">
        <IconInput
          icon={<AlternateEmailRoundedIcon fontSize="small" />}
          label="Email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="email"
          required
        />
        <IconInput
          icon={<LockRoundedIcon fontSize="small" />}
          label="Mật khẩu"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
      </div>

      <Button className="mt-6 h-12 bg-lh-pink font-extrabold hover:bg-lh-pink-dark" size="lg" fullWidth loading={loading}>
        Đăng nhập
        <ArrowForwardRoundedIcon fontSize="small" />
      </Button>

      <p className="mt-5 text-center text-sm text-lh-muted">
        Chưa có tài khoản? <Link className="font-extrabold text-lh-navy hover:text-lh-pink" to="/register">Đăng ký</Link>
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
      <FormHeader mode="register" />
      {error && <Alert tone="error">{error}</Alert>}

      <div className="grid gap-4 sm:grid-cols-2">
        <IconInput
          icon={<PersonRoundedIcon fontSize="small" />}
          label="Tên"
          value={form.firstName}
          onChange={(e) => setForm({ ...form, firstName: e.target.value })}
          autoComplete="given-name"
          required
        />
        <Input
          className="h-12"
          label="Họ"
          value={form.lastName}
          onChange={(e) => setForm({ ...form, lastName: e.target.value })}
          autoComplete="family-name"
          required
        />
      </div>

      <div className="mt-4 space-y-4">
        <IconInput
          icon={<AlternateEmailRoundedIcon fontSize="small" />}
          label="Email"
          type="email"
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
          autoComplete="email"
          required
        />
        <IconInput
          icon={<LockRoundedIcon fontSize="small" />}
          label="Mật khẩu"
          type="password"
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
          autoComplete="new-password"
          showStrength
          hint="Tối thiểu 12 ký tự, nên có chữ hoa, số và ký tự đặc biệt."
          required
        />
      </div>

      <div className="mt-5 rounded-lg border border-lh-border bg-lh-surface px-4 py-3">
        <div className="mb-2 flex items-center justify-between text-xs font-extrabold uppercase tracking-[0.12em] text-lh-muted">
          <span>Activation</span>
          <span>OTP</span>
        </div>
        <ProgressBar value={66} />
      </div>

      <Button className="mt-6 h-12 bg-lh-pink font-extrabold hover:bg-lh-pink-dark" size="lg" fullWidth loading={loading}>
        Tạo tài khoản
        <ArrowForwardRoundedIcon fontSize="small" />
      </Button>

      <p className="mt-5 text-center text-sm text-lh-muted">
        Đã có tài khoản? <Link className="font-extrabold text-lh-navy hover:text-lh-pink" to="/login">Đăng nhập</Link>
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
      <FormHeader mode="verify" />
      {message && <Alert tone="success">{message}</Alert>}
      {error && <Alert tone="error">{error}</Alert>}

      <IconInput
        icon={<MailLockRoundedIcon fontSize="small" />}
        label="Email đăng ký"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        autoComplete="email"
        required
      />

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
                if (e.key === 'Backspace' && !digits[index] && index > 0) {
                  inputRefs.current[index - 1]?.focus();
                }
              }}
              whileFocus={{ scale: 1.04 }}
              className="h-12 rounded-lg border border-lh-input text-center text-lg font-black text-lh-dark outline-none transition focus:border-lh-pink focus:ring-2 focus:ring-lh-pink/25 sm:h-14"
              inputMode="numeric"
              maxLength={1}
              aria-label={`OTP digit ${index + 1}`}
            />
          ))}
        </div>
      </div>

      <Button className="mt-6 h-12 bg-lh-pink font-extrabold hover:bg-lh-pink-dark" size="lg" fullWidth loading={loading}>
        Kích hoạt tài khoản
        <CheckCircleRoundedIcon fontSize="small" />
      </Button>

      <Button
        type="button"
        variant="secondary"
        onClick={resend}
        disabled={resending || cooldown > 0 || !email}
        className="mt-3 h-12 border border-lh-border bg-white font-extrabold text-lh-navy hover:bg-lh-surface"
        fullWidth
        loading={resending}
      >
        <ReplayRoundedIcon fontSize="small" />
        {cooldown > 0 ? `Gửi lại sau ${cooldown}s` : 'Gửi lại mã OTP'}
      </Button>

      <p className="mt-5 text-center text-sm text-lh-muted">
        Đã kích hoạt? <Link className="font-extrabold text-lh-navy hover:text-lh-pink" to="/login">Đăng nhập</Link>
      </p>
    </form>
  );
};

export const LoginPage = () => <AuthShell mode="login" />;
export const RegisterPage = () => <AuthShell mode="register" />;
export const VerifyEmailPage = () => <AuthShell mode="verify" />;
