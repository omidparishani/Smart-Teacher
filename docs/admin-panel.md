# معماری پنل مدیریت (Admin Panel)

> بخش G، ۸، ۹، ۴۷ و ۴۸ خواسته‌ها · نسخه 1.0

---

## ۱. تفکیک از Mini App

> **تصمیم معماری:** پنل مدیریت یک اپلیکیشن **جدا** است، با مخزن کد جدا، بیلد جدا و مسیر جدا. داخل Mini App ایتا قرار **نمی‌گیرد**.

**دلایل:**
1. Mini App محدودیت فضای نمایش (WebView موبایل) دارد؛ پنل مدیریت نیاز به جدول‌های بزرگ، چند ستون و فرم‌های پیچیده دارد.
2. امنیت: پنل ادمین دسترسی به کلیدهای AI و داده مالی دارد. جدا بودن، سطح حمله را کم می‌کند (کوکی HttpOnly، CSRF، 2FA در یک اپ جدا).
3. اندازه باندل: Mini App باید سبک بماند (< 300KB). پنل ادمین می‌تواند سنگین‌تر باشد چون روی دسکتاپ اجرا می‌شود.

```text
https://app.smart-teacher.ir/app/*      → Mini App  (موبایل، RTL، فارسی)
https://app.smart-teacher.ir/admin/*    → Admin Panel (دسکتاپ، RTL، فارسی)
https://app.smart-teacher.ir/api/v1/*   → Backend
```

---

## ۲. پشته فناوری

| مؤلفه | انتخاب | دلیل |
|---|---|---|
| فریم‌ورک | **React 19 + TypeScript** | اکوسیستم غنی برای جدول، فرم و نمودار |
| بیلد | **Vite** | سرعت |
| مسیریابی | React Router | |
| State سرور | **TanStack Query** | کش، بازآزمایی، حالت بارگذاری |
| State کلاینت | Zustand | سبک، بدون Boilerplate |
| فرم | React Hook Form + Zod | اعتبارسنجی یکسان با بک‌اند |
| UI | **shadcn/ui + Radix** | کد در اختیار ما، بدون قفل شدن به کتابخانه |
| استایل | Tailwind CSS | |
| جدول | TanStack Table | مرتب‌سازی، فیلتر، صفحه‌بندی سمت سرور |
| نمودار | Recharts | |
| RTL | `dir="rtl"` روی `<html>` + Tailwind Logical Properties | |
| فونت | Vazirmatn | |

---

## ۳. ساختار پوشه‌ها

```text
admin-panel/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── router/
│   │   └── routes.tsx
│   ├── lib/
│   │   ├── api-client.ts          ← تنها نقطه تماس با HTTP
│   │   ├── query-client.ts
│   │   └── utils.ts
│   ├── stores/
│   │   ├── auth.store.ts
│   │   └── ui.store.ts
│   ├── components/
│   │   ├── ui/                    ← shadcn
│   │   ├── layout/
│   │   │   ├── Sidebar.tsx
│   │   │   ├── Topbar.tsx
│   │   │   └── AdminLayout.tsx
│   │   └── shared/
│   │       ├── DataTable.tsx
│   │       ├── PageHeader.tsx
│   │       ├── StatCard.tsx
│   │       └── ConfirmDialog.tsx
│   ├── features/
│   │   ├── dashboard/
│   │   ├── users/
│   │   ├── tenants/
│   │   ├── schools/
│   │   ├── plans/
│   │   ├── subscriptions/
│   │   ├── payments/
│   │   ├── coupons/
│   │   ├── features/
│   │   ├── providers/
│   │   ├── models/
│   │   ├── prompts/
│   │   │   ├── PromptList.tsx
│   │   │   ├── PromptEditor.tsx
│   │   │   ├── PromptPlayground.tsx
│   │   │   └── PromptVersionDiff.tsx
│   │   ├── usage/
│   │   ├── reports/
│   │   ├── audit/
│   │   ├── tickets/
│   │   └── settings/
│   └── types/
│       └── api.d.ts
```

### قاعده هر Feature

```text
features/<name>/
├── api.ts          ← توابع فراخوانی API (فقط از lib/api-client استفاده می‌کند)
├── hooks.ts        ← هوک‌های TanStack Query
├── types.ts        ← تایپ‌های مختص این Feature
├── components/     ← کامپوننت‌های مختص
└── pages/          ← صفحات مسیریابی‌شده
```

---

## ۴. لایه API — تنها نقطه تماس

```typescript
// lib/api-client.ts
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1';

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public traceId?: string,
    public details?: unknown,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    credentials: 'include',            // کوکی HttpOnly برای پنل ادمین
    headers: {
      'Content-Type': 'application/json',
      ...getCsrfHeader(),              // توکن CSRF
      ...init.headers,
    },
  });

  if (response.status === 401) {
    await refreshSession();
    // تلاش مجدد یک‌بار
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      body?.code ?? 'UNKNOWN_ERROR',
      body?.message ?? 'خطای غیرمنتظره رخ داد.',
      body?.traceId,
      body?.details,
    );
  }

  return response.status === 204 ? (undefined as T) : response.json();
}

export const api = {
  get:   <T>(path: string) => request<T>(path),
  post:  <T>(path: string, body?: unknown) =>
           request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put:   <T>(path: string, body?: unknown) =>
           request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <T>(path: string, body?: unknown) =>
           request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  del:   <T>(path: string) => request<T>(path, { method: 'DELETE' }),
};
```

> **نکته برای شما به‌عنوان توسعه‌دهنده بک‌اند:** تمام تماس‌های HTTP پنل فقط از این یک فایل می‌گذرند. اگر ساختار API عوض شد (مثلاً `/admin/users` به `/admin/accounts`)، **فقط این فایل و `features/*/api.ts`** تغییر می‌کنند — نه کامپوننت‌ها.

---

## ۵. صفحه داشبورد (بخش ۹ خواسته‌ها)

### کارت‌های آماری

```text
┌──────────────┬──────────────┬──────────────┬──────────────┐
│ کل کاربران   │ کاربران فعال │ معلمان       │ مدارس        │
│ 1,247        │ 892          │ 1,180        │ 12           │
│ ▲ ۱۲٪ ماه    │ ▲ ۵٪         │ ▲ ۸٪         │ —            │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ کلاس‌ها      │ دانش‌آموزان   │ کاربران Free │ کاربران Paid │
│ 3,410        │ 78,233       │ 1,020        │ 227          │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ اشتراک فعال  │ در حال انقضا │ درآمد امروز  │ درآمد ماه    │
│ 227          │ 18           │ ۲,۴۵۰,۰۰۰    │ ۴۸,۲۰۰,۰۰۰   │
├──────────────┼──────────────┼──────────────┼──────────────┤
│ تراکنش‌ها    │ درخواست AI   │ توکن AI      │ هزینه AI     │
│ 3,892        │ 412,553      │ 89.2M        │ $127.40      │
└──────────────┴──────────────┴──────────────┴──────────────┘
```

### نمودارها

| نمودار | نوع | بازه |
|---|---|---|
| رشد کاربران | خطی | ۳۰ روز / ۱۲ ماه |
| درآمد | میله‌ای | ۳۰ روز / ۱۲ ماه |
| توزیع اشتراک | دایره‌ای | لحظه‌ای |
| مصرف AI | سطحی انباشته | ۳۰ روز، تفکیک Provider |

### Endpointهای داشبورد

```text
GET /api/v1/admin/dashboard
→ {
    "users":       { "total": 1247, "active": 892, "teachers": 1180, "free": 1020, "paid": 227 },
    "education":   { "schools": 12, "classes": 3410, "students": 78233 },
    "subscriptions":{ "active": 227, "expiringSoon": 18, "expired": 43 },
    "revenue":     { "today": 2450000, "thisMonth": 48200000, "total": 892400000, "currency": "IRT" },
    "transactions":{ "count": 3892, "today": 14 },
    "ai":          { "requests": 412553, "tokens": 89200000, "cost": 127.40, "currency": "USD" },
    "system":      { "errors": 3, "healthStatus": "UP", "lastBackup": "..." },
    "eitaa":       { "activeBots": 1180, "messagesToday": 4521 }
  }

GET /api/v1/admin/dashboard/charts?period=30d
→ {
    "userGrowth":    [ { "date": "2026-09-01", "value": 1180 }, ... ],
    "revenue":       [ { "date": "2026-09-01", "value": 1820000 }, ... ],
    "subscriptions": [ { "plan": "FREE", "count": 1020 }, ... ],
    "aiUsage":       [ { "date": "2026-09-01", "requests": 12400, "cost": 3.82 }, ... ]
  }
```

> **کارایی:** این کوئری‌ها سنگین‌اند. نتیجه در فاز ۱۲ به مدت ۵ دقیقه کش می‌شود. در فاز اول مستقیم محاسبه می‌شوند (حجم داده کم است).

---

## ۶. ویرایشگر پرامپت (بخش ۴۸)

این پیچیده‌ترین صفحه پنل است. سه بخش:

### بخش ۱ — متادیتا
```text
نام:          [بررسی‌کننده تکلیف        ]
کد:           [AI_HOMEWORK_CHECKER      ] (غیرقابل ویرایش پس از ایجاد)
Feature:      [AI_HOMEWORK_CHECK      ▼]
توضیح:        [...................................]
```

### بخش ۲ — ویرایشگر (دو ستونه)
```text
┌─ System Prompt ──────────┬─ User Prompt ───────────────┐
│ شما یک دستیار آموزشی... │ اطلاعات تکلیف:              │
│                          │ - دانش‌آموز: {{studentName}} │
│ [متغیر +] [Schema +]     │ ...                          │
└──────────────────────────┴──────────────────────────────┘
┌─ متغیرها ────────────────────────────────────────────────┐
│ نام          نوع              الزامی  پیش‌فرض            │
│ studentName  STRING           ✓       —              [×] │
│ answer       UNTRUSTED_TEXT   ✓       —              [×] │
│ [+ افزودن متغیر]                                         │
└──────────────────────────────────────────────────────────┘
┌─ Response Schema (JSON) ────────────────────────────────┐
│ { "type": "object", "required": [...], ... }             │
│ ✓ معتبر                                                  │
└──────────────────────────────────────────────────────────┘
```

**اعتبارسنجی زنده:** وقتی Admin تایپ می‌کند:
- متغیرهای استفاده‌شده در متن به‌صورت Chip با رنگ هایلایت می‌شوند
- متغیر ناشناخته → قرمز
- متغیر `required` که استفاده نشده → هشدار زرد
- `UNTRUSTED_TEXT` در System Prompt → **خطای قرمز** (قاعده ۳ انتشار)

### بخش ۳ — Playground کنار ادیتور

با کلیک «تست»، پرامپت رندر می‌شود و به Provider انتخابی ارسال می‌گردد. نتیجه در پنل کنار نمایش داده می‌شود:

```text
Provider: [Anthropic ▼]   Model: [claude-sonnet-5 ▼]
Temperature: [0.2]  Max Tokens: [2048]

[▶ اجرا]  [اجرا با همه Providerها]

┌─ پرامپت رندرشده (System) ───┬─ نتیجه ─────────────────┐
│ شما یک دستیار آموزشی...     │ {                        │
│                             │   "score": 17.5,         │
├─ پرامپت رندرشده (User) ─────┤   "correct": true,       │
│ ...                         │   "feedback": "..."      │
│ <student_answer>            │ }                        │
│ علی پاسخ داد...             │                          │
│ </student_answer>           │ ✓ Schema معتبر            │
└─────────────────────────────┴──────────────────────────┘
⏱ 2.1s   📥 512   📤 187   💰 $0.0042
```

> **نمایش پرامپت رندرشده حیاتی است.** Admin باید ببیند متغیرها واقعاً کجا قرار گرفته‌اند. بدون این، دیباگ پرامپت تقریباً غیرممکن است.

---

## ۷. صفحه مدیریت Provider

```text
┌─ Providerها ─────────────────────────────────────────────┐
│ کد        نام         وضعیت    مدل پیش‌فرض      اولویت    │
│ ANTHROPIC Anthropic   ● فعال   claude-sonnet-5  1        │
│ XAI       xAI         ● فعال   grok-4           2        │
│ OPENAI    OpenAI      ○ غیرفعال gpt-5          3        │
└──────────────────────────────────────────────────────────┘
```

### فرم Provider

```text
کد:           [ANTHROPIC       ] (غیرقابل ویرایش پس از ایجاد)
نام:          [Anthropic       ]
نوع:          [Anthropic     ▼]
Base URL:     [https://api.anthropic.com]
API Key:      [••••••••••••••4f2a]  [تغییر کلید]
              ↑ هرگز مقدار کامل نمایش داده نمی‌شود
Organization: [                 ]
مدل پیش‌فرض:  [claude-sonnet-5 ▼]
Timeout:      [60] ثانیه
حداکثر تلاش:  [3]
اولویت:       [1]
فعال:         [✓]
[تست اتصال]  [دریافت مدل‌ها]
```

> **نمایش کلید:** فقط ۴ کاراکتر آخر. حتی Super Admin هم نمی‌تواند کلید را ببیند — فقط جایگزین کند. این تصمیم امنیتی آگاهانه است: اگر Admin بتواند کلید را ببیند، می‌تواند آن را کپی و جای دیگری استفاده کند، و در صورت نشت، مسئولیت مشخص نیست.

---

## ۸. صفحه تخصیص (Provider / Prompt / Feature)

یک کامپوننت مشترک برای هر سه، چون ساختار یکسان است:

```text
┌─ تخصیص Provider — بررسی تکلیف ───────────────────────────┐
│ [+ افزودن تخصیص]                                          │
│                                                           │
│ سطح       هدف                Provider      مدل      اولویت│
│ GLOBAL    —                  xAI           grok-4    100  │
│ PLAN      حرفه‌ای            Anthropic     sonnet-5  100  │
│ TENANT    آقای احمدی         OpenAI        gpt-5     100  │
│ FEATURE   AI_HOMEWORK_CHECK  xAI           grok-4-fast 50 │
│ CLASS     پنجم الف           Anthropic     opus-5-5  100  │
│                                                 [ویرایش][×]│
└───────────────────────────────────────────────────────────┘
```

با انتخاب سطح، فیلد هدف به‌صورت داینامیک عوض می‌شود (مستأجر، کلاس، پلن یا Feature).

### ابزار «چرا این Provider؟»

دکمه‌ای که نشان می‌دهد برای یک ترکیب مشخص، کدام Provider انتخاب می‌شود و چرا:

```text
GET /api/v1/admin/ai/resolve-trace
    ?featureCode=AI_HOMEWORK_CHECK
    &tenantId=...
    &userId=...
    &classId=...

→ {
    "resolved": { "provider": "ANTHROPIC", "model": "claude-opus-5-5" },
    "trace": [
      { "scope": "USER",   "scopeId": "...", "result": "MISS", "reason": "تخصیصی تعریف نشده" },
      { "scope": "CLASS",  "scopeId": "...", "result": "HIT",
        "assignmentId": "...", "provider": "ANTHROPIC", "model": "claude-opus-5-5" }
    ]
  }
```

> این ابزار برای پشتیبانی حیاتی است. وقتی معلمی می‌گوید «چرا از مدل گران استفاده شد؟»، Admin می‌تواند در چند ثانیه پاسخ دهد.

---

## ۹. جدول‌های بزرگ — الگوی مشترک

```typescript
<DataTable
  columns={userColumns}
  query={useUsers}
  filters={[
    { key: 'status',  label: 'وضعیت',  options: STATUS_OPTIONS },
    { key: 'role',    label: 'نقش',    options: ROLE_OPTIONS },
    { key: 'plan',    label: 'پلن',    options: PLAN_OPTIONS },
  ]}
  searchPlaceholder="جست‌وجوی نام، موبایل یا ایمیل..."
  actions={<Button>افزودن کاربر</Button>}
/>
```

- صفحه‌بندی و مرتب‌سازی **سمت سرور** (نه کلاینت)
- `size` پیش‌فرض ۲۰
- ذخیره فیلترها در URL (`?status=ACTIVE&page=2`) تا لینک قابل اشتراک باشد

---

## ۱۰. دسترسی و امنیت در پنل

| کنترل | پیاده‌سازی |
|---|---|
| توکن | کوکی `HttpOnly` — هرگز در `localStorage` |
| CSRF | توکن از کوکی خوانده، در Header ارسال می‌شود |
| مسیر محافظت‌شده | `<RequireAuth>` + `<RequirePermission permission="PLAN:UPDATE">` |
| انقضای نشست | ریدایرکت خودکار به `/login` با پیام فارسی |
| مخفی‌سازی داده حساس | کلید API هرگز به کلاینت نمی‌آید |
| خروج | فراخوانی `/auth/logout` + پاک‌سازی Query Cache |

```typescript
export function RequirePermission({ permission, children }: Props) {
  const { hasPermission } = useAuth();
  if (!hasPermission(permission)) {
    return <AccessDenied />;     // صفحه فارسی «شما به این بخش دسترسی ندارید»
  }
  return <>{children}</>;
}
```

> **مهم:** این بررسی‌ها فقط **UX** هستند. امنیت واقعی در بک‌اند با `@PreAuthorize` اعمال می‌شود. مخفی‌کردن دکمه، جایگزین کنترل سرور نیست.

---

## ۱۱. تأیید عملیات حساس

برای عملیات برگشت‌ناپذیر، دیالوگ تأیید با **تایپ کردن نام منبع**:

```text
┌─ حذف Provider ──────────────────────────────────┐
│ ⚠ این عملیات برگشت‌ناپذیر است.                   │
│                                                  │
│ Provider «Anthropic» حذف خواهد شد. ۱۴ تخصیص      │
│ فعال و ۱۸ مدل مرتبط نیز تحت تأثیر قرار می‌گیرند. │
│                                                  │
│ برای تأیید، نام Provider را تایپ کنید:           │
│ [                                  ]             │
│                                                  │
│         [انصراف]  [حذف]  ← تا تایپ نشود، غیرفعال │
└──────────────────────────────────────────────────┘
```

این الگو برای: حذف Provider، تغییر قیمت پلن، بازگشت وجه، تغییر نقش کاربر، حذف پلن.

---

## ۱۲. حالت‌های رابط کاربری

| حالت | رفتار |
|---|---|
| بارگذاری | Skeleton (نه Spinner تمام‌صفحه) |
| خطا | کارت خطا + دکمه «تلاش مجدد» + نمایش `traceId` |
| خالی | تصویر + متن راهنما + دکمه اقدام اصلی |
| بدون دسترسی | پیام فارسی روشن |
| آفلاین | نوار هشدار بالای صفحه |

---

## ۱۳. دسترس‌پذیری (Accessibility)

- ناوبری کامل با کیبورد (Tab، Enter، Escape)
- `aria-label` فارسی روی همه دکمه‌های آیکونی
- کنتراست حداقل ۴.۵:۱ در هر دو حالت روشن و تاریک
- Focus Ring واضح
- اعلام تغییرات مهم به Screen Reader (`aria-live`)

---

## ۱۴. حالت تاریک

- تشخیص خودکار از سیستم + امکان تغییر دستی
- ذخیره انتخاب کاربر در پروفایل (نه `localStorage`، تا بین دستگاه‌ها هماهنگ باشد)
- تمام رنگ‌ها با CSS Variables — هیچ رنگ Hardcode‌شده‌ای در کامپوننت‌ها نیست

---

## ۱۵. راهنمای توسعه Frontend (بخش ۷۵ خواسته‌ها)

برای هر صفحه، این مستندات در `admin-panel/README.md` نوشته می‌شود:

| پرسش | پاسخ در کجا |
|---|---|
| این کامپوننت چیست؟ | JSDoc بالای فایل |
| این فایل چه کاری می‌کند؟ | JSDoc بالای فایل |
| API کجاست؟ | `features/<name>/api.ts` |
| State کجا مدیریت می‌شود؟ | سرور: TanStack Query · کلاینت: `stores/` |
| UI را کجا تغییر دهم؟ | `features/<name>/components/` |
| API را کجا تغییر دهم؟ | `features/<name>/api.ts` + `lib/api-client.ts` |

### مثال مستندسازی هر Feature

```typescript
/**
 * صفحه مدیریت پلن‌ها
 *
 * API:      features/plans/api.ts  →  GET/POST/PUT/DELETE /admin/plans
 * State:    TanStack Query (کلید: ['plans'])
 * تغییر UI: features/plans/components/PlanForm.tsx
 * تغییر API: features/plans/api.ts  +  lib/api-client.ts
 */
export function PlansPage() { ... }
```
