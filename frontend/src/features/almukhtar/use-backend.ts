'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { apiClient } from '@/lib/api-client';
import type { BackendState } from './types';

/**
 * Fetches data from the backend API and tracks loading/error state.
 * Pass `null` as endpoint to skip the fetch (useful for conditional fetching based on session).
 */
export function useBackend<T>(endpoint: string | null): BackendState<T> & { refetch: () => void } {
  const [state, setState] = useState<BackendState<T>>({ loading: !!endpoint });
  const endpointRef = useRef(endpoint);

  const fetchData = useCallback(async (ep: string) => {
    setState((prev) => ({ ...prev, loading: true, error: undefined }));
    try {
      const data = await apiClient<T>(ep);
      setState({ data, loading: false });
    } catch (err) {
      setState({
        loading: false,
        error: err instanceof Error ? err.message : 'خطأ في جلب البيانات من الخادم'
      });
    }
  }, []);

  useEffect(() => {
    endpointRef.current = endpoint;
    if (!endpoint) {
      setState({ loading: false });
      return;
    }
    void fetchData(endpoint);
  }, [endpoint, fetchData]);

  const refetch = useCallback(() => {
    const ep = endpointRef.current;
    if (ep) void fetchData(ep);
  }, [fetchData]);

  return { ...state, refetch };
}
