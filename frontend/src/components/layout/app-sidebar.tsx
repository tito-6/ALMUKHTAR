'use client';

import { Icons } from '@/components/icons';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from '@/components/ui/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail
} from '@/components/ui/sidebar';
import { navGroups } from '@/config/nav-config';
import { useSession } from '@/features/almukhtar/session';
import { useFilteredNavGroups } from '@/hooks/use-nav';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { OrgSwitcher } from '../org-switcher';

export default function AppSidebar() {
  const pathname = usePathname();
  const { session, signOut } = useSession();
  const router = useRouter();
  const filteredGroups = useFilteredNavGroups(navGroups);

  return (
    <Sidebar side='right' collapsible='icon' className='border-sidebar-border border-r-0 border-l'>
      <SidebarHeader className='border-sidebar-border border-b px-2 py-3 group-data-[collapsible=icon]:pt-4'>
        <OrgSwitcher />
      </SidebarHeader>
      <SidebarContent className='overflow-x-hidden py-2'>
        {filteredGroups.map((group) => (
          <SidebarGroup key={group.label || 'ungrouped'} className='py-1'>
            {group.label && (
              <SidebarGroupLabel className='text-sidebar-foreground/65 px-3 text-[11px] font-semibold'>
                {group.label}
              </SidebarGroupLabel>
            )}
            <SidebarMenu className='gap-1 px-1'>
              {group.items.map((item) => {
                const Icon = item.icon ? Icons[item.icon] : Icons.logo;
                const isActive =
                  pathname === item.url || (item.url !== '/dashboard/overview' && pathname.startsWith(item.url));
                return (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      asChild
                      tooltip={item.title}
                      isActive={isActive}
                      className='h-9 rounded-sm'
                    >
                      <Link href={item.url}>
                        <Icon />
                        <span>{item.title}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                );
              })}
            </SidebarMenu>
          </SidebarGroup>
        ))}
      </SidebarContent>
      <SidebarFooter className='border-sidebar-border border-t p-2'>
        <SidebarMenu>
          <SidebarMenuItem>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <SidebarMenuButton
                  size='lg'
                  className='data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground rounded-sm'
                >
                  <div className='bg-sidebar-primary text-sidebar-primary-foreground flex h-8 w-8 items-center justify-center rounded-sm'>
                    <Icons.user className='size-4' />
                  </div>
                  <div className='grid flex-1 text-right text-sm leading-tight'>
                    <span className='truncate font-medium'>{session?.username ?? 'غير مسجل'}</span>
                    <span className='text-sidebar-foreground/65 truncate text-xs'>
                      {session?.role ?? 'جلسة غير فعالة'}
                    </span>
                  </div>
                  <Icons.chevronsDown className='mr-auto size-4' />
                </SidebarMenuButton>
              </DropdownMenuTrigger>
              <DropdownMenuContent
                className='w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-md'
                side='bottom'
                align='end'
                sideOffset={4}
              >
                <DropdownMenuLabel className='p-0 font-normal'>
                  <div className='px-3 py-2 text-right'>
                    <div className='font-medium'>{session?.username ?? 'المختار'}</div>
                    <div className='text-muted-foreground text-xs'>
                      {session?.role ?? 'بدون دور'}
                    </div>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuGroup>
                  <DropdownMenuItem onClick={() => router.push('/dashboard/profile')}>
                    <Icons.account className='ml-2 h-4 w-4' />
                    الملف الشخصي
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={() => router.push('/dashboard/notifications')}>
                    <Icons.notification className='ml-2 h-4 w-4' />
                    التنبيهات
                  </DropdownMenuItem>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={signOut}>
                  <Icons.logout className='ml-2 h-4 w-4' />
                  تسجيل الخروج
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}
