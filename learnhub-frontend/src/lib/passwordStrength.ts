/**
 * Entropy-based password strength estimation.
 *
 * Estimates bits of entropy from the character pool size and length,
 * then maps to a 0-4 score with a human label.
 */

export interface PasswordStrength {
  score: 0 | 1 | 2 | 3 | 4;
  label: 'Very weak' | 'Weak' | 'Fair' | 'Strong' | 'Very strong';
  entropyBits: number;
}

export function estimatePasswordStrength(password: string): PasswordStrength {
  if (!password) {
    return { score: 0, label: 'Very weak', entropyBits: 0 };
  }

  let pool = 0;
  if (/[a-z]/.test(password)) pool += 26;
  if (/[A-Z]/.test(password)) pool += 26;
  if (/[0-9]/.test(password)) pool += 10;
  if (/[^a-zA-Z0-9]/.test(password)) pool += 33;

  const entropyBits = password.length * Math.log2(pool || 1);

  let score: PasswordStrength['score'];
  if (entropyBits < 28) score = 0;
  else if (entropyBits < 40) score = 1;
  else if (entropyBits < 60) score = 2;
  else if (entropyBits < 80) score = 3;
  else score = 4;

  const labels: PasswordStrength['label'][] = [
    'Very weak',
    'Weak',
    'Fair',
    'Strong',
    'Very strong',
  ];

  return { score, label: labels[score], entropyBits: Math.round(entropyBits) };
}
