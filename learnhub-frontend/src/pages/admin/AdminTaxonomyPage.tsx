import React, { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import {
  BookCopy,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  FolderTree,
  Globe2,
  Layers3,
  Plus,
  Search,
  SlidersHorizontal,
  Sparkles,
  Tags,
} from 'lucide-react';
import AdminWorkspaceLayout from '../../components/layouts/AdminWorkspaceLayout';
import apiClient from '../../lib/api';
import { cn } from '../../lib/cn';
import { useUIStore } from '../../stores/uiStore';

type Category = {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  displayOrder: number;
  active: boolean;
  createdAt?: string;
};

type Subcategory = {
  id: string;
  categoryId: string;
  categoryName: string;
  name: string;
  slug: string;
  description?: string | null;
  displayOrder: number;
  active: boolean;
};

type Level = {
  id: string;
  code: string;
  label: string;
  description?: string | null;
  displayOrder: number;
  active: boolean;
};

type Language = {
  id: string;
  code: string;
  label: string;
  displayOrder: number;
  active: boolean;
};

type Tag = {
  id: string;
  name: string;
  slug: string;
  displayOrder: number;
  active: boolean;
};

type TaxonomyBundle = {
  categories: Category[];
  subcategories: Subcategory[];
  levels: Level[];
  languages: Language[];
  tags: Tag[];
};

type ComposerKey = 'category' | 'subcategory' | 'level' | 'language' | 'tag';
type TaxonomyFilter = 'all' | 'active' | 'inactive' | 'with-children' | 'without-children';
type TaxonomySort = 'popular' | 'name-asc' | 'name-desc' | 'created-desc' | 'created-asc';

const sections = [
  { key: 'categories', label: 'Danh muc', icon: BookCopy },
  { key: 'subcategories', label: 'Phan loai con', icon: FolderTree },
  { key: 'levels', label: 'Trinh do', icon: Layers3 },
  { key: 'languages', label: 'Ngon ngu', icon: Globe2 },
  { key: 'tags', label: 'Tags', icon: Tags },
] as const;

const composerOptions: Array<{
  key: ComposerKey;
  label: string;
  hint: string;
  icon: typeof BookCopy;
}> = [
  { key: 'category', label: 'Danh muc', hint: 'Nhom chinh cho giao vien', icon: BookCopy },
  { key: 'subcategory', label: 'Phan loai con', hint: 'Chi tiet hoa danh muc', icon: FolderTree },
  { key: 'level', label: 'Trinh do', hint: 'Bac do kho noi dung', icon: Layers3 },
  { key: 'language', label: 'Ngon ngu', hint: 'Ngon ngu giang day', icon: Globe2 },
  { key: 'tag', label: 'Tag', hint: 'Ky nang va chu de', icon: Tags },
];

const taxonomyFilterOptions: Array<{ value: TaxonomyFilter; label: string }> = [
  { value: 'all', label: 'Tat ca' },
  { value: 'active', label: 'Dang bat' },
  { value: 'inactive', label: 'Dang tat' },
  { value: 'with-children', label: 'Co phan loai con' },
  { value: 'without-children', label: 'Chua co phan loai con' },
];

const taxonomySortOptions: Array<{ value: TaxonomySort; label: string }> = [
  { value: 'popular', label: 'Pho bien nhat' },
  { value: 'name-asc', label: 'Ten A-Z' },
  { value: 'name-desc', label: 'Ten Z-A' },
  { value: 'created-desc', label: 'Moi tao truoc' },
  { value: 'created-asc', label: 'Cu nhat truoc' },
];

const AdminTaxonomyPage: React.FC = () => {
  const addToast = useUIStore((state) => state.addToast);
  const [bundle, setBundle] = useState<TaxonomyBundle | null>(null);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<string | null>(null);
  const [activeComposer, setActiveComposer] = useState<ComposerKey>('subcategory');
  const [taxonomyQuery, setTaxonomyQuery] = useState('');
  const [taxonomyFilter, setTaxonomyFilter] = useState<TaxonomyFilter>('all');
  const [taxonomySort, setTaxonomySort] = useState<TaxonomySort>('popular');
  const [taxonomyPage, setTaxonomyPage] = useState(1);
  const [isTaxonomyMenuOpen, setIsTaxonomyMenuOpen] = useState(false);
  const taxonomyMenuRef = useRef<HTMLDivElement | null>(null);

  const [categoryName, setCategoryName] = useState('');
  const [categoryDescription, setCategoryDescription] = useState('');
  const [subcategoryCategoryId, setSubcategoryCategoryId] = useState('');
  const [subcategoryName, setSubcategoryName] = useState('');
  const [subcategoryDescription, setSubcategoryDescription] = useState('');
  const [levelLabel, setLevelLabel] = useState('');
  const [levelDescription, setLevelDescription] = useState('');
  const [languageLabel, setLanguageLabel] = useState('');
  const [tagName, setTagName] = useState('');
  const [submitting, setSubmitting] = useState<string | null>(null);

  const loadBundle = async () => {
    setLoading(true);
    setPageError(null);
    try {
      const { data } = await apiClient.get<TaxonomyBundle>('/v1/admin/taxonomy');
      setBundle(data);
      if (!subcategoryCategoryId && data.categories[0]?.id) {
        setSubcategoryCategoryId(data.categories[0].id);
      }
    } catch (err: any) {
      setPageError(err.response?.data?.message || err.response?.data?.error || 'Khong tai duoc taxonomy');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadBundle();
  }, []);

  const groupedSubcategories = useMemo(() => {
    if (!bundle) {
      return [];
    }

    return bundle.categories.map((category) => ({
      category,
      children: bundle.subcategories.filter((item) => item.categoryId === category.id),
    }));
  }, [bundle]);

  const filteredTaxonomy = useMemo(() => {
    const keyword = taxonomyQuery.trim().toLowerCase();

    return groupedSubcategories.filter(({ category, children }) => {
      const matchesFilter =
        taxonomyFilter === 'all'
          ? true
          : taxonomyFilter === 'active'
            ? category.active
            : taxonomyFilter === 'inactive'
              ? !category.active
              : taxonomyFilter === 'with-children'
                ? children.length > 0
                : children.length === 0;

      const haystack = [
        category.name,
        category.slug,
        category.description ?? '',
        ...children.flatMap((child) => [child.name, child.slug, child.description ?? '']),
      ]
        .join(' ')
        .toLowerCase();

      const matchesQuery = !keyword || haystack.includes(keyword);

      return matchesFilter && matchesQuery;
    });
  }, [groupedSubcategories, taxonomyFilter, taxonomyQuery]);

  const sortedTaxonomy = useMemo(() => {
    const items = [...filteredTaxonomy];

    items.sort((left, right) => {
      if (taxonomySort === 'name-asc') {
        return left.category.name.localeCompare(right.category.name);
      }

      if (taxonomySort === 'name-desc') {
        return right.category.name.localeCompare(left.category.name);
      }

      if (taxonomySort === 'created-desc') {
        const leftTime = left.category.createdAt ? new Date(left.category.createdAt).getTime() : 0;
        const rightTime = right.category.createdAt ? new Date(right.category.createdAt).getTime() : 0;
        return rightTime - leftTime || left.category.name.localeCompare(right.category.name);
      }

      if (taxonomySort === 'created-asc') {
        const leftTime = left.category.createdAt ? new Date(left.category.createdAt).getTime() : 0;
        const rightTime = right.category.createdAt ? new Date(right.category.createdAt).getTime() : 0;
        return leftTime - rightTime || left.category.name.localeCompare(right.category.name);
      }

      return (
        right.children.length - left.children.length
        || left.category.displayOrder - right.category.displayOrder
        || left.category.name.localeCompare(right.category.name)
      );
    });

    return items;
  }, [filteredTaxonomy, taxonomySort]);

  useEffect(() => {
    setTaxonomyPage(1);
  }, [taxonomyFilter, taxonomyQuery, taxonomySort]);

  const taxonomyPageSize = 5;
  const taxonomyPageCount = Math.max(1, Math.ceil(sortedTaxonomy.length / taxonomyPageSize));
  const safeTaxonomyPage = Math.min(taxonomyPage, taxonomyPageCount);
  const paginatedTaxonomy = sortedTaxonomy.slice(
    (safeTaxonomyPage - 1) * taxonomyPageSize,
    safeTaxonomyPage * taxonomyPageSize
  );

  useEffect(() => {
    if (taxonomyPage > taxonomyPageCount) {
      setTaxonomyPage(taxonomyPageCount);
    }
  }, [taxonomyPage, taxonomyPageCount]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (!taxonomyMenuRef.current?.contains(event.target as Node)) {
        setIsTaxonomyMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const submitCategory = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting('category');
    try {
      await apiClient.post('/v1/admin/taxonomy/categories', {
        name: categoryName,
        description: categoryDescription || null,
      });
      setCategoryName('');
      setCategoryDescription('');
      addToast('Da tao danh muc moi', 'success');
      await loadBundle();
    } catch (err: any) {
      addToast(err.response?.data?.message || err.response?.data?.error || 'Khong tao duoc danh muc', 'error');
    } finally {
      setSubmitting(null);
    }
  };

  const submitSubcategory = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting('subcategory');
    try {
      await apiClient.post('/v1/admin/taxonomy/subcategories', {
        categoryId: subcategoryCategoryId,
        name: subcategoryName,
        description: subcategoryDescription || null,
      });
      setSubcategoryName('');
      setSubcategoryDescription('');
      addToast('Da tao phan loai con moi', 'success');
      await loadBundle();
    } catch (err: any) {
      addToast(err.response?.data?.message || err.response?.data?.error || 'Khong tao duoc phan loai con', 'error');
    } finally {
      setSubmitting(null);
    }
  };

  const submitLevel = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting('level');
    try {
      await apiClient.post('/v1/admin/taxonomy/levels', {
        label: levelLabel,
        description: levelDescription || null,
      });
      setLevelLabel('');
      setLevelDescription('');
      addToast('Da tao trinh do moi', 'success');
      await loadBundle();
    } catch (err: any) {
      addToast(err.response?.data?.message || err.response?.data?.error || 'Khong tao duoc trinh do', 'error');
    } finally {
      setSubmitting(null);
    }
  };

  const submitLanguage = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting('language');
    try {
      await apiClient.post('/v1/admin/taxonomy/languages', {
        label: languageLabel,
      });
      setLanguageLabel('');
      addToast('Da tao ngon ngu moi', 'success');
      await loadBundle();
    } catch (err: any) {
      addToast(err.response?.data?.message || err.response?.data?.error || 'Khong tao duoc ngon ngu', 'error');
    } finally {
      setSubmitting(null);
    }
  };

  const submitTag = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting('tag');
    try {
      await apiClient.post('/v1/admin/taxonomy/tags', {
        name: tagName,
      });
      setTagName('');
      addToast('Da tao tag moi', 'success');
      await loadBundle();
    } catch (err: any) {
      addToast(err.response?.data?.message || err.response?.data?.error || 'Khong tao duoc tag', 'error');
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <AdminWorkspaceLayout
      title="Danh muc va taxonomy khoa hoc"
      description="Bo taxonomy nay duoc seed theo nhom category pho bien tren cac nen tang lon nhu Udemy, Coursera, LinkedIn Learning va Skillshare, sau do toi uu lai cho LearnHub de giao vien co bo lua chon on dinh khi tao khoa hoc."
      actions={
        <button
          onClick={() => void loadBundle()}
          className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy"
        >
          <Sparkles className="h-4 w-4" />
          Dong bo lai taxonomy
        </button>
      }
    >
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          {sections.map((section) => {
            const Icon = section.icon;
            const count = bundle
              ? (bundle[section.key] as unknown[]).length
              : 0;

            return (
              <div key={section.key} className="rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-sm">
                <div className="flex items-center gap-3 text-sm font-medium text-[#6B7280]">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#EEF2FF] text-lh-blue">
                    <Icon className="h-4.5 w-4.5" />
                  </div>
                  {section.label}
                </div>
                <div className="mt-4 text-3xl font-semibold tracking-[-0.03em] text-[#111827]">{count}</div>
              </div>
            );
          })}
        </div>

        {pageError ? (
          <div className="rounded-2xl border border-[#FBCFE8] bg-[#FDF2F8] px-4 py-3 text-sm font-medium text-lh-pink">
            {pageError}
          </div>
        ) : null}

        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.45fr)_minmax(360px,0.95fr)]">
          <div className="space-y-6">
            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="mb-4 flex items-center justify-between gap-4">
                <div>
                  <div className="text-sm font-medium text-[#6B7280]">Danh muc va phan loai con</div>
                  <h2 className="mt-2 text-2xl font-semibold tracking-[-0.02em] text-[#111827]">Taxonomy hien tai</h2>
                  <p className="mt-2 text-sm leading-6 text-[#6B7280]">
                    Tim nhanh theo ten, slug, mo ta hoac ten phan loai con. Danh sach duoc chia trang de giu workspace gon khi taxonomy tang len.
                  </p>
                </div>
                <div className="rounded-2xl bg-[#F8FAFC] px-3 py-2 text-right">
                  <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#94A3B8]">Visible groups</div>
                  <div className="mt-1 text-sm font-semibold text-[#111827]">{sortedTaxonomy.length}</div>
                </div>
              </div>

              <div className="mb-5 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <div className="relative w-full max-w-xl">
                  <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-[#9CA3AF]" />
                  <input
                    value={taxonomyQuery}
                    onChange={(event) => setTaxonomyQuery(event.target.value)}
                    placeholder="Tim theo danh muc, slug, mo ta, phan loai con..."
                    className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-[#F9FAFB] pl-11 pr-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:bg-white focus:ring-4 focus:ring-[#EEF2FF]"
                  />
                </div>

                <div className="flex items-center gap-3">
                  <div className="relative" ref={taxonomyMenuRef}>
                    <button
                      type="button"
                      onClick={() => setIsTaxonomyMenuOpen((current) => !current)}
                      className="inline-flex h-11 items-center gap-2 rounded-xl border border-[#E5E7EB] bg-[#F9FAFB] px-3 text-sm font-medium text-[#111827] transition hover:border-[#D9DEF2] hover:bg-white"
                    >
                      <SlidersHorizontal className="h-4 w-4 text-[#64748B]" />
                      <span>
                        {taxonomyFilterOptions.find((option) => option.value === taxonomyFilter)?.label}
                        {' · '}
                        {taxonomySortOptions.find((option) => option.value === taxonomySort)?.label}
                      </span>
                      <ChevronDown className={cn('h-4 w-4 text-[#64748B] transition', isTaxonomyMenuOpen && 'rotate-180')} />
                    </button>

                    {isTaxonomyMenuOpen ? (
                      <div className="absolute right-0 top-[calc(100%+10px)] z-20 w-[320px] rounded-2xl border border-[#E5E7EB] bg-white p-3 shadow-[0_18px_48px_rgba(15,23,42,0.14)]">
                        <div className="px-2 pb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-[#94A3B8]">
                          Bo loc
                        </div>
                        <div className="grid gap-1">
                          {taxonomyFilterOptions.map((option) => (
                            <button
                              key={option.value}
                              type="button"
                              onClick={() => {
                                setTaxonomyFilter(option.value);
                                setIsTaxonomyMenuOpen(false);
                              }}
                              className={cn(
                                'flex items-center justify-between rounded-xl px-3 py-2.5 text-sm transition',
                                taxonomyFilter === option.value
                                  ? 'bg-[#EEF2FF] font-semibold text-lh-blue'
                                  : 'text-[#374151] hover:bg-[#F8FAFC]'
                              )}
                            >
                              <span>{option.label}</span>
                              {taxonomyFilter === option.value ? <Check className="h-4 w-4" /> : null}
                            </button>
                          ))}
                        </div>

                        <div className="my-3 border-t border-[#EEF2F7]" />

                        <div className="px-2 pb-2 text-[11px] font-semibold uppercase tracking-[0.14em] text-[#94A3B8]">
                          Sap xep
                        </div>
                        <div className="grid gap-1">
                          {taxonomySortOptions.map((option) => (
                            <button
                              key={option.value}
                              type="button"
                              onClick={() => {
                                setTaxonomySort(option.value);
                                setIsTaxonomyMenuOpen(false);
                              }}
                              className={cn(
                                'flex items-center justify-between rounded-xl px-3 py-2.5 text-sm transition',
                                taxonomySort === option.value
                                  ? 'bg-[#EEF2FF] font-semibold text-lh-blue'
                                  : 'text-[#374151] hover:bg-[#F8FAFC]'
                              )}
                            >
                              <span>{option.label}</span>
                              {taxonomySort === option.value ? <Check className="h-4 w-4" /> : null}
                            </button>
                          ))}
                        </div>
                      </div>
                    ) : null}
                  </div>
                </div>
              </div>

              {loading && !bundle ? (
                <div className="space-y-3">
                  {Array.from({ length: 4 }).map((_, index) => (
                    <div key={index} className="h-20 animate-pulse rounded-2xl bg-[#F3F4F6]" />
                  ))}
                </div>
              ) : paginatedTaxonomy.length > 0 ? (
                <div className="space-y-4">
                  {paginatedTaxonomy.map(({ category, children }) => (
                    <div key={category.id} className="rounded-2xl border border-[#EEF2F7] bg-[#FBFCFE] p-5">
                      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="rounded-full bg-[#EEF2FF] px-2.5 py-1 text-[11px] font-semibold text-lh-blue">
                              {category.slug}
                            </span>
                            <span className={cn(
                              'rounded-full px-2.5 py-1 text-[11px] font-semibold',
                              category.active ? 'bg-[#EEF8F2] text-[#1F7A45]' : 'bg-[#FDE7EC] text-lh-pink'
                            )}>
                              {category.active ? 'Active' : 'Inactive'}
                            </span>
                          </div>
                          <div className="mt-3 text-lg font-semibold text-[#111827]">{category.name}</div>
                          <div className="mt-2 text-sm leading-6 text-[#6B7280]">{category.description}</div>
                        </div>
                        <div className="rounded-xl bg-white px-3 py-2 text-sm font-medium text-[#4B5563] shadow-sm">
                          {children.length} phan loai con
                        </div>
                      </div>

                      <div className="mt-4 flex flex-wrap gap-2">
                        {children.map((child) => (
                          <span key={child.id} className="rounded-full border border-[#D9DEF2] bg-white px-3 py-1.5 text-sm font-medium text-[#374151]">
                            {child.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-[#D9DEF2] bg-[#FBFCFE] px-5 py-8 text-center">
                  <div className="text-sm font-semibold text-[#111827]">Khong co ket qua phu hop</div>
                  <div className="mt-2 text-sm leading-6 text-[#6B7280]">
                    Thu doi tu khoa tim kiem hoac chuyen bo loc de xem nhom taxonomy khac.
                  </div>
                </div>
              )}

              <div className="mt-5 flex flex-col gap-3 border-t border-[#EEF2F7] pt-5 sm:flex-row sm:items-center sm:justify-between">
                <div className="text-sm text-[#6B7280]">
                  Hien thi{' '}
                  <span className="font-semibold text-[#111827]">
                    {sortedTaxonomy.length === 0 ? 0 : (safeTaxonomyPage - 1) * taxonomyPageSize + 1}
                  </span>
                  {' - '}
                  <span className="font-semibold text-[#111827]">
                    {Math.min(safeTaxonomyPage * taxonomyPageSize, sortedTaxonomy.length)}
                  </span>
                  {' / '}
                  <span className="font-semibold text-[#111827]">{sortedTaxonomy.length}</span> nhom
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setTaxonomyPage((page) => Math.max(1, page - 1))}
                    disabled={safeTaxonomyPage === 1}
                    className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3 text-sm font-semibold text-[#4B5563] transition hover:bg-[#F8FAFC] disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Truoc
                  </button>

                  <div className="rounded-xl bg-[#F8FAFC] px-3 py-2 text-sm font-semibold text-[#111827]">
                    Trang {safeTaxonomyPage}/{taxonomyPageCount}
                  </div>

                  <button
                    type="button"
                    onClick={() => setTaxonomyPage((page) => Math.min(taxonomyPageCount, page + 1))}
                    disabled={safeTaxonomyPage === taxonomyPageCount}
                    className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-3 text-sm font-semibold text-[#4B5563] transition hover:bg-[#F8FAFC] disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    Sau
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="text-sm font-medium text-[#6B7280]">Bo cong cu tao nhanh</div>
                  <h2 className="mt-2 text-xl font-semibold tracking-[-0.02em] text-[#111827]">Them taxonomy moi</h2>
                  <p className="mt-2 text-sm leading-6 text-[#6B7280]">
                    Chon loai du lieu can them, sau do dien mot form duy nhat thay vi phai doc qua qua nhieu o nhap cung luc.
                  </p>
                </div>
                <div className="rounded-2xl bg-[#F8FAFC] px-3 py-2 text-right">
                  <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-[#94A3B8]">Active flow</div>
                  <div className="mt-1 text-sm font-semibold text-[#111827]">
                    {composerOptions.find((option) => option.key === activeComposer)?.label}
                  </div>
                </div>
              </div>

              <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
                {composerOptions.map((option) => {
                  const Icon = option.icon;
                  const active = option.key === activeComposer;

                  return (
                    <button
                      key={option.key}
                      type="button"
                      onClick={() => setActiveComposer(option.key)}
                      className={cn(
                        'flex items-start gap-3 rounded-2xl border px-4 py-4 text-left transition',
                        active
                          ? 'border-[#C7D2FE] bg-[#EEF2FF] shadow-sm'
                          : 'border-[#E5E7EB] bg-[#F8FAFC] hover:border-[#D9DEF2] hover:bg-white'
                      )}
                    >
                      <div className={cn(
                        'mt-0.5 flex h-10 w-10 items-center justify-center rounded-xl',
                        active ? 'bg-white text-lh-blue' : 'bg-white text-[#64748B]'
                      )}>
                        <Icon className="h-4.5 w-4.5" />
                      </div>
                      <div className="min-w-0">
                        <div className="text-sm font-semibold text-[#111827]">{option.label}</div>
                        <div className="mt-1 text-xs leading-5 text-[#6B7280]">{option.hint}</div>
                      </div>
                    </button>
                  );
                })}
              </div>

              <div className="mt-6 rounded-2xl border border-[#E5E7EB] bg-[#FBFCFE] p-5">
                {activeComposer === 'category' ? (
                  <form onSubmit={submitCategory}>
                    <div className="flex items-center gap-2 text-sm font-medium text-[#6B7280]">
                      <BookCopy className="h-4 w-4 text-lh-blue" />
                      Tao danh muc moi
                    </div>
                    <div className="mt-4 space-y-3">
                      <input
                        value={categoryName}
                        onChange={(event) => setCategoryName(event.target.value)}
                        placeholder="Vi du: Blockchain va Web3"
                        className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                      <textarea
                        value={categoryDescription}
                        onChange={(event) => setCategoryDescription(event.target.value)}
                        rows={3}
                        placeholder="Mo ta ngan cho category nay"
                        className="w-full rounded-xl border border-[#E5E7EB] bg-white px-4 py-3 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                    </div>
                    <button
                      type="submit"
                      disabled={!categoryName.trim() || submitting === 'category'}
                      className="mt-4 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Plus className="h-4 w-4" />
                      {submitting === 'category' ? 'Dang tao...' : 'Them danh muc'}
                    </button>
                  </form>
                ) : null}

                {activeComposer === 'subcategory' ? (
                  <form onSubmit={submitSubcategory}>
                    <div className="flex items-center gap-2 text-sm font-medium text-[#6B7280]">
                      <FolderTree className="h-4 w-4 text-lh-blue" />
                      Tao phan loai con
                    </div>
                    <div className="mt-4 space-y-3">
                      <select
                        value={subcategoryCategoryId}
                        onChange={(event) => setSubcategoryCategoryId(event.target.value)}
                        className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm text-[#111827] outline-none transition focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      >
                        {bundle?.categories.map((category) => (
                          <option key={category.id} value={category.id}>{category.name}</option>
                        ))}
                      </select>
                      <input
                        value={subcategoryName}
                        onChange={(event) => setSubcategoryName(event.target.value)}
                        placeholder="Vi du: Solidity Development"
                        className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                      <textarea
                        value={subcategoryDescription}
                        onChange={(event) => setSubcategoryDescription(event.target.value)}
                        rows={3}
                        placeholder="Mo ta ngan cho phan loai con"
                        className="w-full rounded-xl border border-[#E5E7EB] bg-white px-4 py-3 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                    </div>
                    <button
                      type="submit"
                      disabled={!subcategoryCategoryId || !subcategoryName.trim() || submitting === 'subcategory'}
                      className="mt-4 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Plus className="h-4 w-4" />
                      {submitting === 'subcategory' ? 'Dang tao...' : 'Them phan loai con'}
                    </button>
                  </form>
                ) : null}

                {activeComposer === 'level' ? (
                  <form onSubmit={submitLevel}>
                    <div className="flex items-center gap-2 text-sm font-medium text-[#6B7280]">
                      <Layers3 className="h-4 w-4 text-lh-blue" />
                      Tao trinh do
                    </div>
                    <div className="mt-4 space-y-3">
                      <input
                        value={levelLabel}
                        onChange={(event) => setLevelLabel(event.target.value)}
                        placeholder="Vi du: Chuyen sau"
                        className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                      <textarea
                        value={levelDescription}
                        onChange={(event) => setLevelDescription(event.target.value)}
                        rows={2}
                        placeholder="Mo ta ngan cho level"
                        className="w-full rounded-xl border border-[#E5E7EB] bg-white px-4 py-3 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                    </div>
                    <button
                      type="submit"
                      disabled={!levelLabel.trim() || submitting === 'level'}
                      className="mt-4 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Plus className="h-4 w-4" />
                      {submitting === 'level' ? 'Dang tao...' : 'Them trinh do'}
                    </button>
                  </form>
                ) : null}

                {activeComposer === 'language' ? (
                  <form onSubmit={submitLanguage}>
                    <div className="flex items-center gap-2 text-sm font-medium text-[#6B7280]">
                      <Globe2 className="h-4 w-4 text-lh-blue" />
                      Tao ngon ngu
                    </div>
                    <div className="mt-4 space-y-3">
                      <input
                        value={languageLabel}
                        onChange={(event) => setLanguageLabel(event.target.value)}
                        placeholder="Vi du: Spanish"
                        className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                    </div>
                    <button
                      type="submit"
                      disabled={!languageLabel.trim() || submitting === 'language'}
                      className="mt-4 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Plus className="h-4 w-4" />
                      {submitting === 'language' ? 'Dang tao...' : 'Them ngon ngu'}
                    </button>
                  </form>
                ) : null}

                {activeComposer === 'tag' ? (
                  <form onSubmit={submitTag}>
                    <div className="flex items-center gap-2 text-sm font-medium text-[#6B7280]">
                      <Tags className="h-4 w-4 text-lh-blue" />
                      Tao tag
                    </div>
                    <div className="mt-4 space-y-3">
                      <input
                        value={tagName}
                        onChange={(event) => setTagName(event.target.value)}
                        placeholder="Vi du: LangChain"
                        className="h-11 w-full rounded-xl border border-[#E5E7EB] bg-white px-4 text-sm text-[#111827] outline-none transition placeholder:text-[#9CA3AF] focus:border-[#D9DEF2] focus:ring-4 focus:ring-[#EEF2FF]"
                      />
                    </div>
                    <button
                      type="submit"
                      disabled={!tagName.trim() || submitting === 'tag'}
                      className="mt-4 inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-lh-blue px-4 text-sm font-semibold text-white transition hover:bg-lh-navy disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Plus className="h-4 w-4" />
                      {submitting === 'tag' ? 'Dang tao...' : 'Them tag'}
                    </button>
                  </form>
                ) : null}
              </div>
            </div>

            <div className="grid gap-6">
              <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">Trinh do</div>
                    <div className="mt-1 text-xs leading-5 text-[#94A3B8]">Danh sach bac do kho hien co cho khoa hoc.</div>
                  </div>
                  <div className="rounded-xl bg-[#EEF2FF] px-2.5 py-1 text-xs font-semibold text-lh-blue">
                    {bundle?.levels.length ?? 0}
                  </div>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  {bundle?.levels.map((level) => (
                    <span
                      key={level.id}
                      title={level.description ?? undefined}
                      className="rounded-full border border-[#E5E7EB] bg-white px-3 py-1.5 text-sm font-medium text-[#374151]"
                    >
                      {level.label}
                    </span>
                  ))}
                </div>
              </div>

              <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">Ngon ngu</div>
                    <div className="mt-1 text-xs leading-5 text-[#94A3B8]">Ngon ngu giao dien va giang day giao vien co the chon.</div>
                  </div>
                  <div className="rounded-xl bg-[#EEF2FF] px-2.5 py-1 text-xs font-semibold text-lh-blue">
                    {bundle?.languages.length ?? 0}
                  </div>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  {bundle?.languages.map((language) => (
                    <span key={language.id} className="rounded-full border border-[#D9DEF2] bg-[#EEF2FF] px-3 py-1.5 text-sm font-medium text-lh-blue">
                      {language.label}
                    </span>
                  ))}
                </div>
              </div>

              <div className="rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-sm">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <div className="text-sm font-medium text-[#6B7280]">Tags pho bien</div>
                    <div className="mt-1 text-xs leading-5 text-[#94A3B8]">Bo tag nhanh de goi y ky nang, cong nghe va chu de.</div>
                  </div>
                  <div className="rounded-xl bg-[#EEF2FF] px-2.5 py-1 text-xs font-semibold text-lh-blue">
                    {bundle?.tags.length ?? 0}
                  </div>
                </div>
                <div className="mt-4 flex flex-wrap gap-2">
                  {bundle?.tags.map((tag) => (
                    <span key={tag.id} className="rounded-full border border-[#E5E7EB] bg-white px-3 py-1.5 text-sm font-medium text-[#374151]">
                      {tag.name}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AdminWorkspaceLayout>
  );
};

export default AdminTaxonomyPage;
