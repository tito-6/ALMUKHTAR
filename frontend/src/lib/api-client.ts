const BASE_URL = '/api/backend';

export interface SessionUser {
  token: string;
  username: string;
  role: string;
  expiresIn: number;
}

export const SESSION_KEY = 'almukhtar.session';

export function getStoredSession(): SessionUser | null {
  if (typeof window === 'undefined') return null;
  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SessionUser;
  } catch {
    window.localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function storeSession(session: SessionUser) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  window.localStorage.removeItem(SESSION_KEY);
}

export async function apiClient<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const session = getStoredSession();
  const headers = new Headers(options?.headers);
  if (!headers.has('Content-Type') && !(options?.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  if (session?.token) {
    headers.set('Authorization', `Bearer ${session.token}`);
  }

  const res = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
    cache: 'no-store'
  });

  if (!res.ok) {
    const message = await res.text();
    throw new Error(message || `API error: ${res.status} ${res.statusText}`);
  }

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
