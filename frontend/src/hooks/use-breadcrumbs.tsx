'use client';

import { usePathname } from 'next/navigation';
import { useMemo } from 'react';

type BreadcrumbItem = {
  title: string;
  link: string;
};

const routeTitles: Record<string, string> = {
  dashboard: 'مكتب العمليات',
  overview: 'مركز القيادة',
  transfers: 'الحوالات',
  wallet: 'المحافظ',
  liquidity: 'الفروع والسيولة',
  assistant: 'مساعد العمليات',
  services: 'الفواتير والتعبئة',
  lending: 'القروض الصغيرة',
  vault: 'الوثائق والتحقق',
  users: 'المستخدمون',
  notifications: 'التنبيهات',
  profile: 'الملف الشخصي'
};

export function useBreadcrumbs() {
  const pathname = usePathname();

  const breadcrumbs = useMemo(() => {
    const segments = pathname.split('/').filter(Boolean);
    return segments.map((segment, index) => {
      const path = `/${segments.slice(0, index + 1).join('/')}`;
      return {
        title: routeTitles[segment] ?? segment,
        link: path
      };
    });
  }, [pathname]);

  return breadcrumbs satisfies BreadcrumbItem[];
}
