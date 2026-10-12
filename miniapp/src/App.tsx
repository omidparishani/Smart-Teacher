import { useState } from 'react';
import { RouterProvider } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { createAppRouter } from './router/routes';
import { createQueryClient } from './lib/query-client';

/**
 * ریشه برنامه.
 *
 * <p>⚠️ هر دو نمونه با {@code useState} ساخته می‌شوند و نه در سطح ماژول.
 * دلیلش: در سطح ماژول، یک نسخه بین همه آزمون‌ها به اشتراک گذاشته می‌شود و
 * حافظه پنهان و تاریخچه مسیرها از یک آزمون به آزمون بعدی سرریز می‌کند.
 */
export function App() {
  const [queryClient] = useState(createQueryClient);
  const [router] = useState(createAppRouter);

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
