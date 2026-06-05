'use client';

import { Icons } from '@/components/icons';
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar
} from '@/components/ui/sidebar';
import { useSession } from '@/features/almukhtar/session';

export function OrgSwitcher() {
  const { state } = useSidebar();
  const { session } = useSession();

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <SidebarMenuButton size='lg' className='rounded-sm'>
          <div className='bg-sidebar-primary text-sidebar-primary-foreground flex aspect-square size-8 shrink-0 items-center justify-center overflow-hidden rounded-sm'>
            <Icons.galleryVerticalEnd className='size-4' />
          </div>
          <div
            className={`grid flex-1 text-right text-sm leading-tight transition-all duration-200 ease-in-out ${
              state === 'collapsed'
                ? 'invisible max-w-0 overflow-hidden opacity-0'
                : 'visible max-w-full opacity-100'
            }`}
          >
            <span className='truncate font-semibold'>المختار</span>
            <span className='text-sidebar-foreground/65 truncate text-xs'>
              {session?.role ?? 'منصة الحوالات السورية'}
            </span>
          </div>
        </SidebarMenuButton>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
