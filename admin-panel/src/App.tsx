import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { configureAuth } from './lib/api-client';
import { AppRoutes } from './router/routes';
import { useAuthStore } from './stores/auth.store';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

configureAuth({
  getAccessToken: () => useAuthStore.getState().accessToken,
  onSessionLost: () => useAuthStore.getState().clearSession(),
});

useAuthStore.getState().hydrateFromStorage();

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </QueryClientProvider>
  );
}
