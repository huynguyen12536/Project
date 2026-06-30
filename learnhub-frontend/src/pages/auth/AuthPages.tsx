import React, { FormEvent, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Apple,
  ArrowRight,
  CheckCircle2,
  Eye,
  EyeOff,
  Globe,
  Lock,
  Mail,
  Users,
} from 'lucide-react';
import apiClient from '../../lib/api';
import { cn } from '../../lib/cn';
import { estimatePasswordStrength } from '../../lib/passwordStrength';
import { useAuthStore } from '../../stores/authStore';
import type { UserProfile } from '../../types';
import { Button, Input } from '../../ui-kit';
import type { AuthResponse, AuthUser, UserRole } from '../../types/auth';

type AuthMode = 'login' | 'register' | 'verify' | 'forgot' | 'reset';

interface ApiErrorBody {
  error_code?: string;
  message?: string;
}

function normalizeRole(role?: string): UserRole {
  const value = role?.toLowerCase();
  if (value === 'admin') return 'admin';
  if (value === 'instructor') return 'instructor';
  if (value === 'student' || value === 'learner') return 'student';
  return 'student';
}

function toAuthUser(profile: UserProfile | null, auth: AuthResponse): AuthUser {
  if (!profile) {
    return {
      id: auth.userId,
      email: auth.email,
      firstName: '',
      lastName: '',
      role: normalizeRole(auth.role),
    };
  }

  return {
    id: profile.id,
    email: profile.email,
    firstName: profile.firstName ?? '',
    lastName: profile.lastName ?? '',
    role: normalizeRole(auth.role),
    avatarUrl: profile.avatarUrl ?? undefined,
    bio: profile.bio ?? undefined,
  };
}

function getApiMessage(error: unknown, fallback: string): string {
  const maybe = error as { response?: { data?: ApiErrorBody } };
  return maybe.response?.data?.message || fallback;
}

function getInitialEmail(search: string): string {
  return new URLSearchParams(search).get('email') || '';
}

function getResetToken(search: string): string {
  return new URLSearchParams(search).get('token') || '';
}

function getRedirectPath(search: string): string {
  return new URLSearchParams(search).get('redirect') || '/dashboard';
}

function getAuthCopy(mode: AuthMode) {
  switch (mode) {
    case 'login':
      return {
        badge: 'Dang nhap',
        title: 'Dang nhap de tiep tuc hanh trinh hoc tap cua ban',
        description: 'Truy cap lo trinh dang hoc, noi dung da luu va tien do ca nhan trong mot giao dien thong nhat.',
      };
    case 'register':
      return {
        badge: 'Tao tai khoan',
        title: 'Bat dau tai khoan LearnHub cho lo trinh hoc nghiem tuc',
        description: 'Tao tai khoan moi, nhan OTP qua email va kich hoat ngay de bat dau hoc.',
      };
    case 'verify':
      return {
        badge: 'Xac thuc email',
        title: 'Nhap ma OTP de kich hoat tai khoan',
        description: 'Ma xac thuc gom 6 so da duoc gui vao email cua ban. Ban co the gui lai ma neu can.',
      };
    case 'forgot':
      return {
        badge: 'Quen mat khau',
        title: 'Nhan lien ket dat lai mat khau',
        description: 'Nhap email dang ky. Neu tai khoan ton tai, he thong se gui lien ket dat lai mat khau.',
      };
    case 'reset':
      return {
        badge: 'Dat lai mat khau',
        title: 'Tao mat khau moi cho tai khoan cua ban',
        description: 'Su dung mat khau dai, manh va de phan biet voi cac tai khoan khac de giu an toan.',
      };
  }
}

const inputClassName =
  'h-12 w-full rounded-xl border border-lh-input bg-white px-4 text-sm text-lh-dark outline-none transition placeholder:text-lh-muted focus:border-lh-blue focus:ring-4 focus:ring-[#EEF0FB]';

const pageMotion = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -12 },
};

export const AuthShell: React.FC<{ mode: AuthMode }> = ({ mode }) => {
  const copy = getAuthCopy(mode);

  return (
    <section className="bg-lh-surface font-jakarta text-lh-dark">
      <div className="mx-auto flex min-h-[calc(100vh-66px)] max-w-7xl items-center justify-center px-4 py-10 sm:px-6 lg:px-8 lg:py-16">
        <main className="flex w-full items-center justify-center">
          <div className="w-full max-w-[520px]">
            <AnimatePresence mode="wait">
              <motion.div
                key={mode}
                variants={pageMotion}
                initial="initial"
                animate="animate"
                exit="exit"
                transition={{ duration: 0.24, ease: 'easeOut' }}
                className="rounded-2xl border border-lh-border bg-white p-6 shadow-clay-md sm:p-8"
              >
                <div className="mb-6">
                  <span className="inline-flex rounded-full bg-[#EEF0FB] px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-lh-blue">
                    {copy.badge}
                  </span>
                  <h1 className="mt-4 text-2xl font-black leading-tight text-lh-dark sm:text-[2rem]">
                    {copy.title}
                  </h1>
                  <p className="mt-3 text-sm leading-6 text-lh-muted">
                    {copy.description}
                  </p>
                </div>

                {mode === 'login' && <LoginPanel />}
                {mode === 'register' && <RegisterPanel />}
                {mode === 'verify' && <VerifyPanel />}
                {mode === 'forgot' && <ForgotPasswordPanel />}
                {mode === 'reset' && <ResetPasswordPanel />}
              </motion.div>
            </AnimatePresence>
          </div>
        </main>
      </div>
    </section>
  );
};

const InlineAlert: React.FC<{ tone: 'error' | 'success'; children: React.ReactNode }> = ({ tone, children }) => (
  <div
    className={cn(
      'mb-4 rounded-xl border px-4 py-3 text-sm font-semibold',
      tone === 'error'
        ? 'border-lh-pink bg-[#FDE7EC] text-lh-pink'
        : 'border-lh-blue bg-[#EEF0FB] text-lh-blue'
    )}
  >
    {children}
  </div>
);

const AuthDivider = () => (
  <div className="my-5 flex items-center gap-3">
    <span className="h-px flex-1 bg-lh-border" />
    <span className="text-xs font-medium uppercase tracking-[0.2em] text-lh-muted">Lua chon khac</span>
    <span className="h-px flex-1 bg-lh-border" />
  </div>
);

const SocialRow = () => (
  <div className="grid grid-cols-3 gap-3">
    <SocialButton icon={Globe} label="Google" />
    <SocialButton icon={Users} label="Facebook" />
    <SocialButton icon={Apple} label="Apple" />
  </div>
);

const SocialButton: React.FC<{
  icon: React.ComponentType<{ className?: string }>;
  label: string;
}> = ({ icon: Icon, label }) => (
  <button
    type="button"
    disabled
    className="inline-flex h-12 items-center justify-center rounded-full border border-lh-input bg-white text-lh-navy"
    title={`${label} se duoc bat khi provider san sang`}
  >
    <Icon className="h-5 w-5" />
  </button>
);

const FormField: React.FC<{
  label: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  autoComplete?: string;
  required?: boolean;
  hint?: string;
}> = ({ label, type = 'text', value, onChange, placeholder, autoComplete, required = false, hint }) => (
  <Input
      label={label}
      type={type}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      autoComplete={autoComplete}
      placeholder={placeholder}
      required={required}
      hint={hint}
      className={inputClassName}
  />
);

const PasswordField: React.FC<{
  label?: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete?: string;
  required?: boolean;
  hint?: string;
  showStrength?: boolean;
}> = ({ label, value, onChange, autoComplete, required = false, hint, showStrength = false }) => {
  const [visible, setVisible] = useState(false);
  const strength = showStrength ? estimatePasswordStrength(value) : null;

  return (
    <div>
      {label ? <label className="mb-2 block text-sm font-semibold text-lh-dark">{label}</label> : null}
      <div className="relative">
        <input
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          autoComplete={autoComplete}
          required={required}
          className={cn(inputClassName, 'pr-12')}
        />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          className="absolute right-3 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-lh-muted transition hover:bg-[#EEF0FB] hover:text-lh-blue"
          aria-label={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
      {strength && value.length > 0 && (
        <div className="mt-3">
          <div className="flex gap-1">
            {[0, 1, 2, 3, 4].map((index) => (
              <span
                key={index}
                className={cn(
                  'h-1.5 flex-1 rounded-full',
                  index <= strength.score ? 'bg-lh-pink' : 'bg-lh-border'
                )}
              />
            ))}
          </div>
          <p className="mt-2 text-xs text-lh-muted">
            {strength.label} - {strength.entropyBits} bits
          </p>
        </div>
      )}
      {hint && <p className="mt-2 text-xs leading-5 text-lh-muted">{hint}</p>}
    </div>
  );
};

const PrimaryButton: React.FC<React.ButtonHTMLAttributes<HTMLButtonElement> & { loading?: boolean }> = ({
  children,
  className,
  loading = false,
  ...props
}) => (
  <motion.div whileTap={{ scale: 0.985 }}>
    <Button
      type={props.type ?? 'button'}
      disabled={props.disabled}
      loading={loading}
      size="lg"
      fullWidth
      className={cn(
        'h-12 rounded-lg bg-lh-pink text-sm font-bold text-white transition hover:bg-lh-pink-dark focus-visible:ring-lh-blue',
        className
      )}
      {...props}
    >
      {children}
    </Button>
  </motion.div>
);

const TextButton: React.FC<React.ButtonHTMLAttributes<HTMLButtonElement>> = ({ children, className, ...props }) => (
  <button
    className={cn('text-sm font-semibold text-lh-blue underline-offset-4 transition hover:underline', className)}
    {...props}
  >
    {children}
  </button>
);

const LoginPanel = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((state) => state.login);
  const [email, setEmail] = useState(getInitialEmail(location.search));
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const redirectPath = getRedirectPath(location.search);

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const { data } = await apiClient.post<AuthResponse>('/v1/auth/login', { email, password });
      let profile: UserProfile | null = null;

      try {
        const profileResponse = await apiClient.get<UserProfile>(`/v1/users/${data.userId}`);
        profile = profileResponse.data;
      } catch {
        profile = null;
      }

      const user = toAuthUser(profile, data);
      login({ ...data, user });
      navigate(redirectPath);
    } catch (error) {
      const code = (error as { response?: { data?: ApiErrorBody } }).response?.data?.error_code;
      if (code === 'EMAIL_NOT_VERIFIED') {
        navigate(`/verify-email?email=${encodeURIComponent(email)}`);
        return;
      }
      setError(getApiMessage(error, 'Khong the dang nhap. Vui long kiem tra lai email va mat khau.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      {error && <InlineAlert tone="error">{error}</InlineAlert>}

      <div className="space-y-4">
        <FormField
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          placeholder="ban@learnhub.vn"
          autoComplete="email"
          required
        />
        <div>
          <div className="mb-2 flex items-center justify-between gap-3">
            <label className="text-sm font-semibold text-lh-dark">Mat khau</label>
            <Link to="/forgot-password" className="text-sm font-semibold text-lh-blue underline-offset-4 transition hover:underline">
              Quen mat khau?
            </Link>
          </div>
          <PasswordField value={password} onChange={setPassword} autoComplete="current-password" required />
        </div>
      </div>

      <PrimaryButton loading={loading} type="submit" className="mt-6">
        Tiep tuc
        {!loading && <ArrowRight className="h-4 w-4" />}
      </PrimaryButton>

      <AuthDivider />
      <SocialRow />

      <p className="mt-6 text-center text-sm text-lh-muted">
        Ban khong co tai khoan?{' '}
        <Link to="/register" className="font-semibold text-lh-blue underline-offset-4 transition hover:underline">
          Dang ky
        </Link>
      </p>
      <div className="mt-3 text-center">
        <TextButton type="button">Dang nhap bang ten to chuc cua ban</TextButton>
      </div>
    </form>
  );
};

const RegisterPanel = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    if (form.password !== form.confirmPassword) {
      setError('Mat khau xac nhan khong khop.');
      return;
    }

    setLoading(true);
    try {
      await apiClient.post('/v1/auth/register', {
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        password: form.password,
      });
      navigate(`/verify-email?email=${encodeURIComponent(form.email)}`);
    } catch (error) {
      setError(getApiMessage(error, 'Khong the tao tai khoan. Vui long kiem tra lai thong tin.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      {error && <InlineAlert tone="error">{error}</InlineAlert>}

      <div className="grid gap-4 sm:grid-cols-2">
        <FormField
          label="Ten"
          value={form.firstName}
          onChange={(value) => setForm((current) => ({ ...current, firstName: value }))}
          placeholder="Huy"
          autoComplete="given-name"
          required
        />
        <FormField
          label="Ho"
          value={form.lastName}
          onChange={(value) => setForm((current) => ({ ...current, lastName: value }))}
          placeholder="Nguyen"
          autoComplete="family-name"
          required
        />
      </div>

      <div className="mt-4 space-y-4">
        <FormField
          label="Email"
          type="email"
          value={form.email}
          onChange={(value) => setForm((current) => ({ ...current, email: value }))}
          placeholder="ban@learnhub.vn"
          autoComplete="email"
          required
        />
        <PasswordField
          label="Mat khau"
          value={form.password}
          onChange={(value) => setForm((current) => ({ ...current, password: value }))}
          autoComplete="new-password"
          hint="Toi thieu 12 ky tu, uu tien chu hoa, chu thuong, so va ky tu dac biet."
          required
          showStrength
        />
        <PasswordField
          label="Xac nhan mat khau"
          value={form.confirmPassword}
          onChange={(value) => setForm((current) => ({ ...current, confirmPassword: value }))}
          autoComplete="new-password"
          required
        />
      </div>

      <div className="mt-5 rounded-2xl border border-lh-border bg-lh-surface px-4 py-4 text-sm text-lh-navy">
        Sau khi dang ky, ban se nhan ma OTP qua email de kich hoat tai khoan truoc khi dang nhap.
      </div>

      <PrimaryButton loading={loading} type="submit" className="mt-6">
        Tao tai khoan
        {!loading && <ArrowRight className="h-4 w-4" />}
      </PrimaryButton>

      <p className="mt-6 text-center text-sm text-lh-muted">
        Da co tai khoan?{' '}
        <Link to="/login" className="font-semibold text-lh-blue underline-offset-4 transition hover:underline">
          Dang nhap
        </Link>
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

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setTimeout(() => setCooldown((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [cooldown]);

  const otp = digits.join('');

  const setDigit = (index: number, value: string) => {
    const clean = value.replace(/\D/g, '').slice(-1);
    const next = [...digits];
    next[index] = clean;
    setDigits(next);
    if (clean && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const onPaste = (event: React.ClipboardEvent<HTMLInputElement>) => {
    const clean = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (clean.length < 2) return;
    event.preventDefault();
    setDigits(clean.padEnd(6, '').split('').slice(0, 6));
  };

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setMessage('');

    if (otp.length !== 6) {
      setError('Vui long nhap du 6 so OTP.');
      return;
    }

    setLoading(true);
    try {
      await apiClient.post('/v1/auth/verify-email-otp', { email, otp });
      setMessage('Tai khoan da duoc kich hoat. Dang chuyen sang dang nhap...');
      window.setTimeout(() => navigate(`/login?email=${encodeURIComponent(email)}`), 700);
    } catch (error) {
      setError(getApiMessage(error, 'OTP khong hop le hoac da het han.'));
    } finally {
      setLoading(false);
    }
  };

  const onResend = async () => {
    setError('');
    setMessage('');
    setResending(true);

    try {
      await apiClient.post('/v1/auth/resend-verification-otp', { email });
      setDigits(['', '', '', '', '', '']);
      setCooldown(60);
      setMessage('Neu email dang cho kich hoat, he thong da gui ma OTP moi.');
    } catch (error) {
      setError(getApiMessage(error, 'Khong the gui lai OTP luc nay.'));
    } finally {
      setResending(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      {message && <InlineAlert tone="success">{message}</InlineAlert>}
      {error && <InlineAlert tone="error">{error}</InlineAlert>}

      <div className="space-y-4">
        <FormField
          label="Email dang ky"
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="email"
          required
        />
        <div>
          <label className="mb-2 block text-sm font-semibold text-lh-dark">Ma OTP</label>
          <div className="grid grid-cols-6 gap-2">
            {digits.map((digit, index) => (
              <motion.input
                key={index}
                ref={(node) => {
                  inputRefs.current[index] = node;
                }}
                value={digit}
                onChange={(event) => setDigit(index, event.target.value)}
                onPaste={onPaste}
                onKeyDown={(event) => {
                  if (event.key === 'Backspace' && !digits[index] && index > 0) {
                    inputRefs.current[index - 1]?.focus();
                  }
                }}
                whileFocus={{ scale: 1.03 }}
                inputMode="numeric"
                maxLength={1}
                className="h-12 rounded-xl border border-lh-input text-center text-lg font-black text-lh-dark outline-none transition focus:border-lh-blue focus:ring-4 focus:ring-[#EEF0FB]"
                aria-label={`OTP digit ${index + 1}`}
              />
            ))}
          </div>
        </div>
      </div>

      <PrimaryButton loading={loading} type="submit" className="mt-6">
        Kich hoat tai khoan
        {!loading && <CheckCircle2 className="h-4 w-4" />}
      </PrimaryButton>

      <button
        type="button"
        onClick={onResend}
        disabled={resending || cooldown > 0 || !email}
        className="mt-3 inline-flex h-12 w-full items-center justify-center rounded-lg border border-lh-input bg-white px-4 text-sm font-semibold text-lh-blue transition hover:bg-lh-surface disabled:cursor-not-allowed disabled:opacity-60"
      >
        {resending ? 'Dang gui lai...' : cooldown > 0 ? `Gui lai sau ${cooldown}s` : 'Gui lai ma OTP'}
      </button>

      <p className="mt-6 text-center text-sm text-lh-muted">
        Da kich hoat?{' '}
        <Link to="/login" className="font-semibold text-lh-blue underline-offset-4 transition hover:underline">
          Dang nhap
        </Link>
      </p>
    </form>
  );
};

const ForgotPasswordPanel = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const response = await apiClient.post<{ message: string }>('/v1/auth/forgot-password', { email });
      setMessage(response.data.message || 'Neu email ton tai, lien ket dat lai mat khau da duoc gui.');
    } catch (error) {
      setError(getApiMessage(error, 'Khong the gui yeu cau dat lai mat khau luc nay.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      {message && <InlineAlert tone="success">{message}</InlineAlert>}
      {error && <InlineAlert tone="error">{error}</InlineAlert>}

      <FormField
        label="Email"
        type="email"
        value={email}
        onChange={setEmail}
        placeholder="ban@learnhub.vn"
        autoComplete="email"
        required
      />

      <PrimaryButton loading={loading} type="submit" className="mt-6">
        Gui lien ket dat lai
        {!loading && <Mail className="h-4 w-4" />}
      </PrimaryButton>

      <p className="mt-6 text-center text-sm text-lh-muted">
        Quay lai{' '}
        <Link to="/login" className="font-semibold text-lh-blue underline-offset-4 transition hover:underline">
          Dang nhap
        </Link>
      </p>
    </form>
  );
};

const ResetPasswordPanel = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [token, setToken] = useState(getResetToken(location.search));
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setMessage('');

    if (password !== confirmPassword) {
      setError('Mat khau xac nhan khong khop.');
      return;
    }

    setLoading(true);
    try {
      const response = await apiClient.post<{ message: string }>('/v1/auth/reset-password', {
        token,
        newPassword: password,
      });
      setMessage(response.data.message || 'Dat lai mat khau thanh cong.');
      window.setTimeout(() => navigate('/login'), 900);
    } catch (error) {
      setError(getApiMessage(error, 'Khong the dat lai mat khau voi token hien tai.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={onSubmit}>
      {message && <InlineAlert tone="success">{message}</InlineAlert>}
      {error && <InlineAlert tone="error">{error}</InlineAlert>}

      <div className="space-y-4">
        <FormField
          label="Reset token"
          value={token}
          onChange={setToken}
          placeholder="Token tu email"
          required
        />
        <PasswordField
          label="Mat khau moi"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
          hint="Mat khau moi phai dai toi thieu 12 ky tu."
          required
          showStrength
        />
        <PasswordField
          label="Xac nhan mat khau moi"
          value={confirmPassword}
          onChange={setConfirmPassword}
          autoComplete="new-password"
          required
        />
      </div>

      <PrimaryButton loading={loading} type="submit" className="mt-6">
        Dat lai mat khau
        {!loading && <Lock className="h-4 w-4" />}
      </PrimaryButton>

      <p className="mt-6 text-center text-sm text-lh-muted">
        Da nho mat khau?{' '}
        <Link to="/login" className="font-semibold text-lh-blue underline-offset-4 transition hover:underline">
          Dang nhap
        </Link>
      </p>
    </form>
  );
};

export const LoginPage = () => <AuthShell mode="login" />;
export const RegisterPage = () => <AuthShell mode="register" />;
export const VerifyEmailPage = () => <AuthShell mode="verify" />;
export const ForgotPasswordPage = () => <AuthShell mode="forgot" />;
export const ResetPasswordPage = () => <AuthShell mode="reset" />;

export default LoginPage;
