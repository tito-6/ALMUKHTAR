'use client';

import { useCallback, useEffect, useState } from 'react';
import { Icons } from '@/components/icons';
import PageContainer from '@/components/layout/page-container';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { apiClient } from '@/lib/api-client';
import { toast } from 'sonner';
import { useBackend } from './use-backend';
import type { Notification } from './types';

const TYPE_TABS: [string, string][] = [
  ['', 'الكل'],
  ['SYSTEM', 'النظام'],
  ['ALERT', 'تنبيه'],
  ['TRANSACTION', 'معاملات'],
  ['CAMPAIGN', 'حملات'],
];

function toArray<T>(raw: T[] | { content?: T[]; data?: T[] } | null | undefined): T[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  const r = raw as { content?: T[]; data?: T[] };
  if (Array.isArray(r.content)) return r.content;
  if (Array.isArray(r.data)) return r.data;
  return [];
}

export function NotificationsPage() {
  const [tick, setTick] = useState(0);
  const refresh = useCallback(() => setTick((n) => n + 1), []);

  const notifications = useBackend<Notification[] | { content?: Notification[]; data?: Notification[] }>(
    `/notifications/my?_t=${tick}`
  );

  const [typeTab, setTypeTab] = useState('');
  const [markingAll, setMarkingAll] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const notificationList = toArray(
    notifications.data as Notification[] | { content?: Notification[]; data?: Notification[] } | null
  );

  const filtered = typeTab
    ? notificationList.filter((n) => n.type === typeTab)
    : notificationList;

  const unread = notificationList.filter((n) => !n.read).length;

  // Auto-refresh every 30 seconds
  useEffect(() => {
    const id = setInterval(refresh, 30_000);
    return () => clearInterval(id);
  }, [refresh]);

  async function markRead(id: number) {
    try {
      await apiClient(`/notifications/${id}/read`, { method: 'PATCH' });
      toast.success('تم تحديد الإشعار كمقروء');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تحديث الإشعار');
    }
  }

  async function markAllRead() {
    setMarkingAll(true);
    try {
      await apiClient('/notifications/mark-all-read', { method: 'POST' });
      toast.success('تم تحديد جميع الإشعارات كمقروءة');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل تحديث الإشعارات');
    } finally {
      setMarkingAll(false);
    }
  }

  async function deleteNotification(id: number) {
    setDeletingId(id);
    try {
      await apiClient(`/notifications/${id}`, { method: 'DELETE' });
      toast.success('تم حذف الإشعار');
      refresh();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'فشل حذف الإشعار');
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <PageContainer
      pageTitle='الإشعارات'
      pageDescription='إشعارات النظام والتنبيهات الواردة من الخادم.'
    >
      <div className='space-y-4'>
        {/* Header bar */}
        <div className='flex flex-wrap items-center justify-between gap-3'>
          <div className='flex items-center gap-2'>
            <span className='text-sm font-medium'>الإشعارات</span>
            {unread > 0 && (
              <Badge className='text-xs h-5 px-1.5'>{unread} غير مقروء</Badge>
            )}
            {notifications.loading && (
              <Icons.spinner className='size-4 animate-spin text-muted-foreground' />
            )}
          </div>
          <div className='flex gap-2'>
            {unread > 0 && (
              <Button size='sm' variant='outline' className='h-8 rounded-sm text-xs'
                onClick={() => { void markAllRead(); }} disabled={markingAll}>
                {markingAll && <Icons.spinner className='me-1.5 size-3.5 animate-spin' />}
                <Icons.check className='me-1.5 size-3.5' />
                قراءة الكل
              </Button>
            )}
            <Button size='sm' variant='ghost' className='h-8 text-xs' onClick={refresh}>
              <Icons.spinner className='me-1 size-3.5' />
              تحديث
            </Button>
          </div>
        </div>

        {/* Type filter tabs */}
        <div className='flex gap-1 border-b border-border pb-0'>
          {TYPE_TABS.map(([tab, label]) => (
            <button key={tab}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px ${
                typeTab === tab
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
              onClick={() => setTypeTab(tab)}>
              {label}
            </button>
          ))}
        </div>

        {/* Notifications list */}
        <div className='gov-panel rounded-md'>
          {notifications.error && (
            <div className='border-b border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive rounded-t-md'>
              تعذر تحميل الإشعارات: {notifications.error}
            </div>
          )}

          <div className='divide-y divide-border'>
            {filtered.map((n) => (
              <div key={n.id}
                className={`flex items-start gap-3 px-4 py-3 transition-colors ${
                  !n.read ? 'bg-primary/5' : ''
                }`}>
                {/* Read indicator */}
                <div
                  className={`mt-2 size-2 shrink-0 rounded-full ${
                    !n.read ? 'bg-primary' : 'bg-transparent border border-border'
                  }`}
                />

                {/* Content */}
                <div className='flex-1 min-w-0'>
                  <div className='flex flex-wrap items-center gap-2 mb-0.5'>
                    {n.title && (
                      <span className='text-sm font-medium'>{n.title}</span>
                    )}
                    {n.type && (
                      <Badge variant='outline' className='text-xs h-4 px-1.5'>{n.type}</Badge>
                    )}
                    {n.read && (
                      <Badge variant='secondary' className='text-xs h-4 px-1.5'>مقروء</Badge>
                    )}
                  </div>
                  <div className='text-sm text-muted-foreground'>{n.message ?? '-'}</div>
                  <div className='mt-1 text-xs text-muted-foreground'>{n.createdAt ?? ''}</div>
                </div>

                {/* Actions */}
                <div className='flex shrink-0 items-center gap-1'>
                  {n.link && (
                    <a href={n.link} target='_blank' rel='noopener noreferrer'>
                      <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                        title='فتح الرابط'>
                        <Icons.externalLink className='size-3.5' />
                      </Button>
                    </a>
                  )}
                  {!n.read && (
                    <Button size='sm' variant='ghost' className='h-7 text-xs px-2'
                      onClick={() => { void markRead(n.id); }} title='تحديد كمقروء'>
                      <Icons.check className='size-3.5' />
                    </Button>
                  )}
                  <Button size='sm' variant='ghost' className='h-7 text-xs px-2 text-destructive hover:text-destructive'
                    disabled={deletingId === n.id}
                    onClick={() => { void deleteNotification(n.id); }} title='حذف'>
                    {deletingId === n.id
                      ? <Icons.spinner className='size-3.5 animate-spin' />
                      : <Icons.trash className='size-3.5' />}
                  </Button>
                </div>
              </div>
            ))}

            {!notifications.loading && !filtered.length && (
              <div className='px-4 py-16 text-center'>
                <Icons.notification className='mx-auto mb-3 size-12 text-muted-foreground/30' />
                <div className='text-sm font-medium text-muted-foreground mb-1'>
                  {typeTab ? 'لا توجد إشعارات من هذا النوع' : 'لا توجد إشعارات'}
                </div>
                <div className='text-xs text-muted-foreground'>
                  ستظهر الإشعارات هنا فور ورودها
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
