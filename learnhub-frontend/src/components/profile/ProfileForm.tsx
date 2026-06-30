import React, { useEffect, useMemo, useState } from 'react';
import { Check, PencilLine, X } from 'lucide-react';
import { ErrorResponse, UserProfile, UserProfileUpdatePayload } from '../../types';
import { validateEmail, validateName, validatePhone } from '../../utils/validation';

interface ProfileFormProps {
  profile: UserProfile;
  isLoading: boolean;
  error: ErrorResponse | null;
  onSubmit: (updates: Partial<UserProfileUpdatePayload>) => Promise<void>;
}

type EditableField = keyof UserProfileUpdatePayload;

type FieldConfig = {
  field: EditableField;
  label: string;
  placeholder: string;
  multiline?: boolean;
  required?: boolean;
  maxLength?: number;
  normalize?: (value: string) => string;
  validate?: (value: string) => string | null;
};

const fieldConfigs: FieldConfig[] = [
  {
    field: 'firstName',
    label: 'Ho',
    placeholder: 'Nhap ho cua ban',
    required: true,
    maxLength: 50,
    normalize: (value) => value.trim(),
    validate: (value) => {
      if (!value.trim()) return 'Ho khong duoc de trong';
      return validateName(value.trim()) ? null : 'Ho phai tu 1-50 ky tu';
    },
  },
  {
    field: 'lastName',
    label: 'Ten',
    placeholder: 'Nhap ten cua ban',
    required: true,
    maxLength: 50,
    normalize: (value) => value.trim(),
    validate: (value) => {
      if (!value.trim()) return 'Ten khong duoc de trong';
      return validateName(value.trim()) ? null : 'Ten phai tu 1-50 ky tu';
    },
  },
  {
    field: 'email',
    label: 'Email',
    placeholder: 'email.cua.ban@example.com',
    required: true,
    maxLength: 100,
    normalize: (value) => value.trim(),
    validate: (value) => {
      if (!value.trim()) return 'Email khong duoc de trong';
      return validateEmail(value.trim()) ? null : 'Vui long nhap dia chi email hop le';
    },
  },
  {
    field: 'phone',
    label: 'So dien thoai',
    placeholder: '+84 123 456 789',
    maxLength: 50,
    normalize: (value) => value.trim(),
    validate: (value) => (validatePhone(value.trim()) ? null : 'So dien thoai khong hop le'),
  },
  {
    field: 'location',
    label: 'Dia chi',
    placeholder: 'Thanh pho, Quoc gia',
    maxLength: 100,
    normalize: (value) => value.trim(),
    validate: (value) => (value.trim().length <= 100 ? null : 'Dia chi khong vuot qua 100 ky tu'),
  },
  {
    field: 'bio',
    label: 'Gioi thieu',
    placeholder: 'Gioi thieu mot chut ve ban...',
    multiline: true,
    maxLength: 500,
    normalize: (value) => value.trim(),
    validate: (value) => (value.trim().length <= 500 ? null : 'Gioi thieu khong vuot qua 500 ky tu'),
  },
];

const fieldValueMap = (profile: UserProfile): Record<EditableField, string> => ({
  firstName: profile.firstName || '',
  lastName: profile.lastName || '',
  email: profile.email || '',
  phone: profile.phone || '',
  location: profile.location || '',
  bio: profile.bio || '',
});

export const ProfileForm: React.FC<ProfileFormProps> = ({
  profile,
  isLoading,
  error,
  onSubmit,
}) => {
  const [editingField, setEditingField] = useState<EditableField | null>(null);
  const [drafts, setDrafts] = useState<Record<EditableField, string>>(() => fieldValueMap(profile));
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [savingField, setSavingField] = useState<EditableField | null>(null);
  const [savedField, setSavedField] = useState<EditableField | null>(null);

  useEffect(() => {
    setDrafts(fieldValueMap(profile));
  }, [profile]);

  const profileValues = useMemo(() => fieldValueMap(profile), [profile]);

  const beginEdit = (field: EditableField) => {
    setEditingField(field);
    setFieldError(null);
    setSavedField(null);
    setDrafts((current) => ({
      ...current,
      [field]: profileValues[field],
    }));
  };

  const cancelEdit = () => {
    if (!editingField) return;
    setDrafts((current) => ({
      ...current,
      [editingField]: profileValues[editingField],
    }));
    setEditingField(null);
    setFieldError(null);
  };

  const handleSave = async (config: FieldConfig) => {
    const rawValue = drafts[config.field] ?? '';
    const normalizedValue = config.normalize ? config.normalize(rawValue) : rawValue;
    const validationMessage = config.validate ? config.validate(normalizedValue) : null;

    if (validationMessage) {
      setFieldError(validationMessage);
      return;
    }

    if (normalizedValue === profileValues[config.field]) {
      setEditingField(null);
      setFieldError(null);
      return;
    }

    setSavingField(config.field);
    setFieldError(null);
    try {
      await onSubmit({ [config.field]: normalizedValue } as Partial<UserProfileUpdatePayload>);
      setSavedField(config.field);
      setEditingField(null);
      window.setTimeout(() => setSavedField((current) => (current === config.field ? null : current)), 2200);
    } finally {
      setSavingField(null);
    }
  };

  return (
    <div className="rounded-[28px] border border-[#E7E9F2] bg-white p-8 shadow-[0_12px_32px_rgba(21,22,46,0.04)]">
      <div className="mb-8 flex items-start justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-[-0.03em] text-[#111827]">Thong tin tai khoan</h2>
          <p className="mt-2 text-sm text-[#6B7280]">
            Moi truong thong tin co the duoc chinh sua rieng va gui len bang PATCH.
          </p>
        </div>
        {savedField ? (
          <div className="inline-flex items-center gap-2 rounded-full bg-[#EEF8F2] px-4 py-2 text-sm font-semibold text-[#1F7A45]">
            <Check className="h-4 w-4" />
            Da cap nhat
          </div>
        ) : null}
      </div>

      {error ? (
        <div className="mb-6 rounded-2xl border border-lh-pink/30 bg-lh-pink/10 p-4">
          <p className="text-sm font-semibold text-lh-pink">{error.error}</p>
        </div>
      ) : null}

      <div className="space-y-4">
        {fieldConfigs.map((config) => {
          const isEditing = editingField === config.field;
          const isSaving = savingField === config.field;
          const value = drafts[config.field] ?? '';
          const displayValue = profileValues[config.field]?.trim() || 'Chua cap nhat';

          return (
            <div
              key={config.field}
              className="rounded-2xl border border-[#E7E9F2] bg-[#FBFCFE] p-5 transition hover:border-[#D9DEF2]"
            >
              <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0 flex-1">
                  <div className="text-xs font-semibold uppercase tracking-[0.12em] text-[#9CA3AF]">{config.label}</div>
                  {!isEditing ? (
                    <div className="mt-3 whitespace-pre-wrap text-base leading-7 text-[#111827]">{displayValue}</div>
                  ) : config.multiline ? (
                    <div className="mt-3">
                      <textarea
                        value={value}
                        maxLength={config.maxLength}
                        rows={4}
                        onChange={(event) => {
                          setDrafts((current) => ({ ...current, [config.field]: event.target.value }));
                          setFieldError(null);
                        }}
                        className="w-full rounded-2xl border border-[#D9DEF2] bg-white px-4 py-3 text-sm text-[#111827] outline-none transition focus:border-lh-blue focus:ring-4 focus:ring-[#EEF2FF]"
                        placeholder={config.placeholder}
                      />
                      <div className="mt-2 flex justify-end text-xs text-[#9CA3AF]">{value.length}/{config.maxLength}</div>
                    </div>
                  ) : (
                    <div className="mt-3">
                      <input
                        type={config.field === 'email' ? 'email' : 'text'}
                        value={value}
                        maxLength={config.maxLength}
                        onChange={(event) => {
                          setDrafts((current) => ({ ...current, [config.field]: event.target.value }));
                          setFieldError(null);
                        }}
                        className="h-12 w-full rounded-2xl border border-[#D9DEF2] bg-white px-4 text-sm text-[#111827] outline-none transition focus:border-lh-blue focus:ring-4 focus:ring-[#EEF2FF]"
                        placeholder={config.placeholder}
                      />
                    </div>
                  )}
                  {isEditing && fieldError ? <p className="mt-2 text-sm text-lh-pink">{fieldError}</p> : null}
                </div>

                <div className="flex shrink-0 items-center gap-2">
                  {!isEditing ? (
                    <button
                      type="button"
                      onClick={() => beginEdit(config.field)}
                      disabled={isLoading}
                      className="inline-flex h-10 items-center gap-2 rounded-xl border border-[#D9DEF2] bg-white px-3.5 text-sm font-semibold text-lh-blue transition hover:bg-[#F8FAFF] disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <PencilLine className="h-4 w-4" />
                      Chinh sua
                    </button>
                  ) : (
                    <>
                      <button
                        type="button"
                        onClick={() => handleSave(config)}
                        disabled={isLoading || isSaving}
                        className="inline-flex h-10 items-center gap-2 rounded-xl bg-lh-blue px-3.5 text-sm font-semibold text-white transition hover:bg-lh-navy disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <Check className="h-4 w-4" />
                        {isSaving ? 'Dang luu...' : 'Luu'}
                      </button>
                      <button
                        type="button"
                        onClick={cancelEdit}
                        disabled={isLoading || isSaving}
                        className="inline-flex h-10 items-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3.5 text-sm font-semibold text-[#4B5563] transition hover:bg-[#F9FAFB] disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <X className="h-4 w-4" />
                        Huy
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ProfileForm;
