'use client';

import { Icons } from '@/components/icons';
import { useKBar } from 'kbar';
import { Button } from './ui/button';

export default function SearchInput() {
  const { query } = useKBar();
  return (
    <div className='w-full space-y-2'>
      <Button
        variant='outline'
        className='bg-background text-muted-foreground relative h-9 w-full justify-start rounded-sm text-sm font-normal shadow-none sm:pr-12 md:w-40 lg:w-64'
        onClick={query.toggle}
      >
        <Icons.search className='mr-2 h-4 w-4' />
        بحث سريع...
        <kbd className='bg-muted pointer-events-none absolute top-[0.3rem] right-[0.3rem] hidden h-6 items-center gap-1 rounded-sm border px-1.5 font-mono text-[10px] font-medium opacity-100 select-none sm:flex'>
          <span className='text-xs'>⌘</span>K
        </kbd>
      </Button>
    </div>
  );
}
