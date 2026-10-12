import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { CreditCard } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { EmptyState } from '../../components/shared/EmptyState';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import {
  checkout,
  currentSubscription,
  listInvoices,
  listPlans,
  type PlanDto,
} from '../../lib/billing-api';

/**
 * صفحه اشتراک من — پلن جاری، خرید، فاکتورها (فاز ۱۰).
 */
export function SubscriptionPage() {
  const queryClient = useQueryClient();
  const [coupon, setCoupon] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const subQuery = useQuery({
    queryKey: ['subscription', 'current'],
    queryFn: () => currentSubscription(),
  });
  const plansQuery = useQuery({
    queryKey: ['plans'],
    queryFn: () => listPlans(),
  });
  const invoicesQuery = useQuery({
    queryKey: ['subscription', 'invoices'],
    queryFn: () => listInvoices(),
  });

  const checkoutMutation = useMutation({
    mutationFn: (plan: PlanDto) => checkout(plan.id, coupon.trim() || undefined),
    onSuccess: async () => {
      setFormError(null);
      setCoupon('');
      await queryClient.invalidateQueries({ queryKey: ['subscription'] });
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'خرید انجام نشد.');
    },
  });

  const loading = subQuery.isPending || plansQuery.isPending || invoicesQuery.isPending;
  if (loading) {
    return <LoadingSkeleton lines={4} />;
  }
  if (subQuery.isError || plansQuery.isError) {
    return <ErrorCard title="خطا" message="بارگذاری اشتراک ناموفق بود." />;
  }

  const current = subQuery.data;
  const plans = plansQuery.data ?? [];

  return (
    <>
      <PageHeader title="اشتراک من" description="پلن جاری، ارتقا و فاکتورها" />

      <section className="mb-4 rounded-xl border border-border bg-card p-4">
        <h2 className="text-sm font-medium text-muted-foreground">پلن فعلی</h2>
        {current ? (
          <div className="mt-2 space-y-1 text-sm">
            <p className="text-lg font-semibold">{current.planName ?? current.planCode}</p>
            <p>وضعیت: {current.status}</p>
            {current.endDate ? (
              <p className="text-muted-foreground">
                پایان: {new Date(current.endDate).toLocaleDateString('fa-IR')}
              </p>
            ) : null}
          </div>
        ) : (
          <EmptyState
            icon={<CreditCard className="size-8" />}
            title="بدون اشتراک فعال"
            description="یکی از پلن‌های زیر را انتخاب کنید."
          />
        )}
      </section>

      <section className="mb-4">
        <h2 className="mb-2 text-sm font-medium">خرید / ارتقا</h2>
        <label className="mb-3 block text-sm">
          کد تخفیف (اختیاری)
          <input
            className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2"
            value={coupon}
            onChange={(e) => setCoupon(e.target.value)}
            placeholder="SAVE20"
          />
        </label>
        {formError ? <p className="mb-2 text-sm text-destructive">{formError}</p> : null}
        <ul className="flex flex-col gap-2">
          {plans.map((plan) => (
            <li
              key={plan.id}
              className="flex items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3"
            >
              <div>
                <p className="font-medium">{plan.name}</p>
                <p className="text-xs text-muted-foreground">
                  {Number(plan.price).toLocaleString('fa-IR')} {plan.currency}
                </p>
              </div>
              <Button
                variant="secondary"
                disabled={checkoutMutation.isPending}
                onClick={() => checkoutMutation.mutate(plan)}
              >
                انتخاب
              </Button>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h2 className="mb-2 text-sm font-medium">فاکتورها</h2>
        {invoicesQuery.isError ? (
          <p className="text-sm text-destructive">بارگذاری فاکتورها ناموفق بود.</p>
        ) : (invoicesQuery.data?.length ?? 0) === 0 ? (
          <p className="text-sm text-muted-foreground">فاکتوری ثبت نشده است.</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {invoicesQuery.data.map((inv) => (
              <li
                key={inv.id}
                className="rounded-xl border border-border bg-card px-4 py-3 text-sm"
              >
                <p className="font-medium">{inv.invoiceNumber}</p>
                <p className="text-muted-foreground">
                  {inv.status} · {Number(inv.total).toLocaleString('fa-IR')} {inv.currency}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
