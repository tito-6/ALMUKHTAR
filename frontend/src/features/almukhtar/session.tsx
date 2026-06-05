'use client';

import { clearSession, getStoredSession, type SessionUser } from '@/lib/api-client';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

export function useSession() {
  const [session, setSession] = useState<SessionUser | null>(null);
  const [ready, setReady] = useState(false);
  const router = useRouter();

  useEffect(() => {
    setSession(getStoredSession());
    setReady(true);
  }, []);

  function signOut() {
    clearSession();
    setSession(null);
    router.push('/auth/sign-in');
  }

  return { session, ready, signOut };
}
