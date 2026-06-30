import React from 'react';
import Skeleton from '@mui/material/Skeleton';
import { cn } from '../../lib/cn';

interface RouteSkeletonProps {
  path: string;
  compact?: boolean;
}

const CardSkeleton = () => (
  <div className="rounded-2xl border border-lh-border bg-white p-4 shadow-clay-sm">
    <Skeleton variant="rounded" height={180} className="rounded-xl" />
    <Skeleton variant="text" width="78%" height={34} className="mt-4" />
    <Skeleton variant="text" width="62%" height={24} />
    <Skeleton variant="text" width="48%" height={24} />
  </div>
);

const AuthSkeleton = () => (
  <div className="mx-auto max-w-[520px] rounded-lg border border-lh-border bg-white p-5 shadow-clay-md sm:p-7">
    <Skeleton variant="rounded" width={120} height={28} />
    <Skeleton variant="text" width="68%" height={46} className="mt-4" />
    <Skeleton variant="text" width="88%" height={24} />
    <div className="mt-8 space-y-4">
      <Skeleton variant="rounded" height={52} />
      <Skeleton variant="rounded" height={52} />
      <Skeleton variant="rounded" height={52} />
      <Skeleton variant="rounded" height={52} />
    </div>
  </div>
);

const HomeSkeleton = () => (
  <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
    <div className="grid gap-8 lg:grid-cols-[1.05fr_0.95fr]">
      <div className="rounded-3xl border border-lh-border bg-white p-6 shadow-clay-sm">
        <Skeleton variant="text" width="30%" height={24} />
        <Skeleton variant="text" width="82%" height={70} className="mt-3" />
        <Skeleton variant="text" width="92%" height={28} />
        <Skeleton variant="rounded" height={56} className="mt-6" />
        <div className="mt-8 grid gap-4 sm:grid-cols-3">
          <Skeleton variant="rounded" height={92} />
          <Skeleton variant="rounded" height={92} />
          <Skeleton variant="rounded" height={92} />
        </div>
      </div>
      <div className="rounded-3xl border border-lh-border bg-white p-6 shadow-clay-sm">
        <Skeleton variant="rounded" height={420} />
      </div>
    </div>

    <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
      {Array.from({ length: 8 }).map((_, index) => (
        <CardSkeleton key={index} />
      ))}
    </div>
  </div>
);

const DashboardSkeleton = () => (
  <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-md">
      <Skeleton variant="rounded" width={168} height={28} />
      <Skeleton variant="text" width="62%" height={58} className="mt-4" />
      <Skeleton variant="text" width="86%" height={26} />
      <Skeleton variant="rounded" width={260} height={40} className="mt-4" />
    </div>

    <div className="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_340px]">
      <div className="space-y-6">
        <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
          <div className="grid gap-5 lg:grid-cols-2">
            <Skeleton variant="rounded" height={320} />
            <Skeleton variant="rounded" height={320} />
          </div>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="rounded-[24px] border border-lh-border bg-white p-5 shadow-clay-sm">
              <Skeleton variant="text" width="58%" height={24} />
              <Skeleton variant="text" width="40%" height={42} className="mt-3" />
              <Skeleton variant="text" width="72%" height={22} className="mt-4" />
            </div>
          ))}
        </div>
        <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
          <Skeleton variant="text" width="32%" height={34} />
          <div className="mt-6 grid gap-4 md:grid-cols-2">
            {Array.from({ length: 4 }).map((_, index) => (
              <CardSkeleton key={index} />
            ))}
          </div>
        </div>
      </div>
      <div className="space-y-6">
        <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
          <Skeleton variant="text" width="54%" height={28} />
          <div className="mt-4 space-y-4">
            <Skeleton variant="rounded" height={92} />
            <Skeleton variant="rounded" height={92} />
            <Skeleton variant="rounded" height={92} />
          </div>
        </div>
        <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
          <Skeleton variant="text" width="48%" height={28} />
          <div className="mt-4 space-y-4">
            <Skeleton variant="rounded" height={92} />
            <Skeleton variant="rounded" height={92} />
            <Skeleton variant="rounded" height={110} />
          </div>
        </div>
      </div>
    </div>
  </div>
);

const CoursesSkeleton = () => (
  <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
    <Skeleton variant="text" width="24%" height={28} />
    <Skeleton variant="text" width="38%" height={60} className="mt-2" />
    <div className="mt-8 grid gap-8 lg:grid-cols-[260px_1fr]">
      <div className="rounded-2xl border border-lh-border bg-white p-5 shadow-clay-sm">
        <Skeleton variant="text" width="52%" height={24} />
        <div className="mt-4 space-y-3">
          <Skeleton variant="rounded" height={18} />
          <Skeleton variant="rounded" height={18} />
          <Skeleton variant="rounded" height={18} />
          <Skeleton variant="rounded" height={18} />
        </div>
      </div>
      <div>
        <div className="flex items-center justify-between gap-4">
          <Skeleton variant="rounded" width="24%" height={20} />
          <Skeleton variant="rounded" width="22%" height={20} />
        </div>
        <div className="mt-6 grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <CardSkeleton key={index} />
          ))}
        </div>
      </div>
    </div>
  </div>
);

const OrdersSkeleton = () => (
  <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
    <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-md">
      <Skeleton variant="rounded" width={190} height={28} />
      <Skeleton variant="text" width="48%" height={56} className="mt-4" />
      <Skeleton variant="text" width="82%" height={24} />
    </div>

    <div className="mt-6 grid gap-4 sm:grid-cols-3">
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="rounded-[24px] border border-lh-border bg-white p-5 shadow-clay-sm">
          <Skeleton variant="text" width="52%" height={24} />
          <Skeleton variant="text" width="34%" height={40} className="mt-3" />
          <Skeleton variant="text" width="72%" height={22} className="mt-4" />
        </div>
      ))}
    </div>

    <div className="mt-6 rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
      <div className="flex flex-col gap-3 lg:flex-row lg:justify-between">
        <div>
          <Skeleton variant="text" width={150} height={24} />
          <Skeleton variant="text" width={260} height={38} className="mt-2" />
        </div>
        <div className="grid gap-3 sm:grid-cols-2 lg:w-[520px]">
          <Skeleton variant="rounded" height={44} />
          <Skeleton variant="rounded" height={44} />
        </div>
      </div>

      <div className="mt-6 space-y-3">
        {Array.from({ length: 6 }).map((_, index) => (
          <Skeleton key={index} variant="rounded" height={56} />
        ))}
      </div>
    </div>
  </div>
);

const LearningSkeleton = () => (
  <div className="min-h-screen bg-[#F5F7FB]">
    <div className="border-b border-[#E8ECF5] bg-white px-4 py-4 sm:px-6 xl:px-8">
      <div className="mx-auto flex max-w-[1600px] items-center justify-between gap-4">
        <Skeleton variant="rounded" width={180} height={40} />
        <Skeleton variant="text" width="32%" height={34} className="hidden xl:block" />
        <div className="flex items-center gap-3">
          <Skeleton variant="rounded" width={180} height={32} className="hidden lg:block" />
          <Skeleton variant="rounded" width={120} height={40} className="hidden lg:block" />
          <Skeleton variant="rounded" width={40} height={40} className="xl:hidden" />
        </div>
      </div>
    </div>

    <div className="mx-auto flex max-w-[1600px] gap-6 px-4 py-6 sm:px-6 xl:px-8">
      <div className="min-w-0 flex-1">
        <div className="overflow-hidden rounded-[28px] border border-lh-border bg-white shadow-clay-sm">
          <Skeleton variant="rounded" height={420} className="rounded-none" />
          <div className="border-t border-lh-border p-5">
            <div className="flex gap-3 overflow-hidden">
              {Array.from({ length: 4 }).map((_, index) => (
                <Skeleton key={index} variant="rounded" width={120} height={44} />
              ))}
            </div>
            <div className="mt-5 grid gap-4 lg:grid-cols-2">
              <Skeleton variant="rounded" height={220} />
              <Skeleton variant="rounded" height={220} />
            </div>
          </div>
        </div>
      </div>
      <div className="hidden w-[360px] xl:block">
        <div className="rounded-[28px] border border-lh-border bg-white p-4 shadow-clay-sm">
          <Skeleton variant="text" width="44%" height={28} />
          <div className="mt-4 space-y-3">
            {Array.from({ length: 6 }).map((_, index) => (
              <Skeleton key={index} variant="rounded" height={68} />
            ))}
          </div>
        </div>
      </div>
    </div>
  </div>
);

const InstructorSkeleton = () => (
  <div className="min-h-screen bg-[#F7F8FC] px-4 py-6 sm:px-6 xl:px-8">
    <div className="mx-auto max-w-[1600px]">
      <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
        <div className="flex items-center justify-between gap-4">
          <div>
            <Skeleton variant="rounded" width={170} height={36} />
            <Skeleton variant="text" width={420} height={48} className="mt-3" />
          </div>
          <Skeleton variant="rounded" width={170} height={48} />
        </div>
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[260px_minmax(0,1fr)]">
        <div className="rounded-[28px] border border-lh-border bg-white p-4 shadow-clay-sm">
          <Skeleton variant="text" width="52%" height={28} />
          <div className="mt-5 space-y-3">
            {Array.from({ length: 5 }).map((_, index) => (
              <Skeleton key={index} variant="rounded" height={48} />
            ))}
          </div>
        </div>

        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-3">
            {Array.from({ length: 3 }).map((_, index) => (
              <div key={index} className="rounded-[24px] border border-lh-border bg-white p-5 shadow-clay-sm">
                <Skeleton variant="text" width="52%" height={24} />
                <Skeleton variant="text" width="38%" height={42} className="mt-3" />
                <Skeleton variant="text" width="78%" height={22} className="mt-4" />
              </div>
            ))}
          </div>

          <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
            <Skeleton variant="text" width="28%" height={34} />
            <div className="mt-6 space-y-3">
              {Array.from({ length: 3 }).map((_, index) => (
                <Skeleton key={index} variant="rounded" height={110} />
              ))}
            </div>
          </div>

          <div className="rounded-[28px] border border-lh-border bg-white p-6 shadow-clay-sm">
            <Skeleton variant="text" width="32%" height={34} />
            <div className="mt-6 space-y-4">
              {Array.from({ length: 3 }).map((_, index) => (
                <Skeleton key={index} variant="rounded" height={190} />
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
);

const DetailSkeleton = () => (
  <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
    <div className="grid gap-10 lg:grid-cols-[1.4fr_0.8fr]">
      <div>
        <Skeleton variant="text" width="28%" height={24} />
        <Skeleton variant="text" width="86%" height={58} className="mt-3" />
        <Skeleton variant="text" width="78%" height={28} />
        <div className="mt-6 grid gap-4 sm:grid-cols-3">
          <Skeleton variant="rounded" height={84} />
          <Skeleton variant="rounded" height={84} />
          <Skeleton variant="rounded" height={84} />
        </div>
        <div className="mt-10 space-y-5">
          <Skeleton variant="rounded" height={180} />
          <Skeleton variant="rounded" height={240} />
          <Skeleton variant="rounded" height={120} />
        </div>
      </div>
      <div className="sticky top-24 rounded-2xl border border-lh-border bg-white p-5 shadow-clay-sm">
        <Skeleton variant="rounded" height={220} />
        <Skeleton variant="text" width="66%" height={42} className="mt-5" />
        <Skeleton variant="rounded" height={48} className="mt-4" />
        <Skeleton variant="rounded" height={48} className="mt-3" />
        <div className="mt-5 space-y-3">
          <Skeleton variant="text" width="72%" height={20} />
          <Skeleton variant="text" width="90%" height={20} />
          <Skeleton variant="text" width="82%" height={20} />
        </div>
      </div>
    </div>
  </div>
);

const ProfileSkeleton = () => (
  <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
    <div className="grid gap-8 lg:grid-cols-3">
      <div className="space-y-6 lg:sticky lg:top-8">
        <div className="rounded-2xl border border-lh-border bg-white p-6 shadow-clay-sm">
          <div className="flex flex-col items-center">
            <Skeleton variant="circular" width={128} height={128} />
            <Skeleton variant="text" width="72%" height={38} className="mt-4" />
            <Skeleton variant="text" width="48%" height={24} />
            <Skeleton variant="text" width="88%" height={24} />
          </div>
        </div>
        <div className="rounded-2xl border border-lh-border bg-white p-6 shadow-clay-sm">
          <Skeleton variant="rounded" height={180} />
        </div>
      </div>
      <div className="lg:col-span-2 rounded-2xl border border-lh-border bg-white p-6 shadow-clay-sm">
        <Skeleton variant="text" width="32%" height={38} />
        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <Skeleton variant="rounded" height={56} />
          <Skeleton variant="rounded" height={56} />
          <Skeleton variant="rounded" height={56} className="sm:col-span-2" />
          <Skeleton variant="rounded" height={140} className="sm:col-span-2" />
        </div>
        <Skeleton variant="rounded" height={52} className="mt-6" />
      </div>
    </div>
  </div>
);

export const RouteSkeleton: React.FC<RouteSkeletonProps> = ({ path, compact = false }) => {
  const normalized = path.toLowerCase();

  return (
    <div className={cn(
      compact ? 'min-h-screen bg-[#F7F8FC]' : 'min-h-[calc(100vh-66px)] bg-[#F7F8FC]',
      normalized.startsWith('/login') ||
      normalized.startsWith('/register') ||
      normalized.startsWith('/verify-email') ||
      normalized.startsWith('/forgot-password') ||
      normalized.startsWith('/reset-password')
        ? 'px-4 py-8 sm:px-6'
        : ''
    )}>
      {normalized.startsWith('/login') ||
      normalized.startsWith('/register') ||
      normalized.startsWith('/verify-email') ||
      normalized.startsWith('/forgot-password') ||
      normalized.startsWith('/reset-password')
        ? <AuthSkeleton />
        : normalized.startsWith('/instructor/')
          ? <InstructorSkeleton />
        : normalized.startsWith('/learn/')
          ? <LearningSkeleton />
        : normalized.startsWith('/dashboard')
          ? <DashboardSkeleton />
        : normalized.startsWith('/orders')
          ? <OrdersSkeleton />
        : normalized.startsWith('/courses/') || normalized.startsWith('/profile')
          ? (normalized.startsWith('/profile') ? <ProfileSkeleton /> : <DetailSkeleton />)
          : normalized.startsWith('/courses')
            ? <CoursesSkeleton />
            : <HomeSkeleton />}
    </div>
  );
};

export default RouteSkeleton;
