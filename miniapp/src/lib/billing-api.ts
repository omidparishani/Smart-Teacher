import { apiFetch } from './api-client';

export interface PlanDto {
  id: string;
  code: string;
  name: string;
  description: string | null;
  price: number | string;
  currency: string;
  billingPeriod: string;
}

export interface SubscriptionDto {
  id: string;
  planId: string;
  planCode: string | null;
  planName: string | null;
  status: string;
  startDate: string | null;
  endDate: string | null;
  pricePaid: number | string | null;
  invoiceNumber: string | null;
  paymentAuthority: string | null;
  redirectUrl: string | null;
}

export interface InvoiceDto {
  id: string;
  invoiceNumber: string;
  subscriptionId: string | null;
  subtotal: number | string;
  discount: number | string;
  total: number | string;
  currency: string;
  status: string;
  issuedAt: string | null;
}

export function listPlans(): Promise<PlanDto[]> {
  return apiFetch<PlanDto[]>('/plans');
}

export function currentSubscription(): Promise<SubscriptionDto | null> {
  return apiFetch<SubscriptionDto | null>('/subscription/current');
}

export function checkout(planId: string, couponCode?: string): Promise<SubscriptionDto> {
  return apiFetch<SubscriptionDto>('/subscription/checkout', {
    method: 'POST',
    body: { planId, couponCode: couponCode ?? null },
  });
}

export function listInvoices(): Promise<InvoiceDto[]> {
  return apiFetch<InvoiceDto[]>('/subscription/invoices');
}
