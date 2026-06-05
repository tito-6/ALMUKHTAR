import { NotificationCenter } from '@/features/notifications/components/notification-center';
import { Breadcrumbs } from '../breadcrumbs';
import SearchInput from '../search-input';
import { ThemeModeToggle } from '../themes/theme-mode-toggle';
import { Separator } from '../ui/separator';
import { SidebarTrigger } from '../ui/sidebar';

export default function Header() {
  return (
    <header className='border-border bg-card/95 sticky top-0 z-20 flex h-14 shrink-0 items-center justify-between gap-2 border-b shadow-xs backdrop-blur'>
      <div className='flex min-w-0 items-center gap-2 px-4'>
        <SidebarTrigger className='-mr-1' />
        <Separator orientation='vertical' className='ml-2 h-4' />
        <div className='min-w-0'>
          <Breadcrumbs />
          <div className='text-muted-foreground hidden text-[11px] md:block'>
            بيئة التشغيل السورية | ربط مباشر مع خادم Spring Boot
          </div>
        </div>
      </div>

      <div className='flex items-center gap-2 px-4'>
        <div className='hidden md:flex'>
          <SearchInput />
        </div>
        <ThemeModeToggle />
        <NotificationCenter />
      </div>
    </header>
  );
}
