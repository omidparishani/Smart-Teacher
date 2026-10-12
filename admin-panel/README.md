# پنل مدیریت — دستیار هوشمند معلم

اپلیکیشن جدا از Mini App ایتا (`docs/admin-panel.md`). دسکتاپ، RTL، فارسی.

## پیش‌نیاز

- Node.js ≥ ۲۲
- بک‌اند Spring روی `http://localhost:8080`

## اجرا

```bash
cd admin-panel
npm install
npm run dev
```

آدرس پیش‌فرض توسعه: `http://localhost:5174`  
پروکسی Vite مسیر `/api` را به بک‌اند می‌فرستد.

## ساخت و آزمون

```bash
npm run build
npm test
```

## متغیر محیطی

| متغیر | پیش‌فرض | توضیح |
|---|---|---|
| `VITE_API_URL` | `/api/v1` | نشانی پایه API |

## ورود

نام کاربری و رمز، از پروفایل `dev` بک‌اند:

| مورد | مقدار پیش‌فرض توسعه |
|---|---|
| آدرس | `http://localhost:5174` |
| نام کاربری | `superadmin` |
| رمز | `ChangeMe-Admin-1!` |

> ⚠️ کاربر پیش‌فرض را `config/DevBootstrap.java` و **فقط** در پروفایل `dev` می‌سازد
> (`app.dev.seed=true`). با پروفایل `prod` هیچ کاربر پیش‌فرضی ساخته نمی‌شود.

## محدوده فعلی

| صفحه | مسیر | API |
|---|---|---|
| ورود | `/login` | `POST /auth/login` |
| داشبورد | `/` | `GET /admin/dashboard` |
| کاربران | `/users` | `GET /admin/users` |
| مستأجرها | `/tenants` | `/admin/tenants` |
| مدارس | `/schools` | `/admin/schools` |
| امکانات | `/features` | `/admin/features` |
| پرامپت‌ها | `/prompts` | `/admin/prompts` |
| AI Providers | `/ai/providers` | `/admin/ai/providers` |
| پلن‌ها | `/plans` | `/admin/plans` |
| کوپن‌ها | `/coupons` | `/admin/coupons` |
| اشتراک‌ها | `/subscriptions` | `/admin/subscriptions` |
| گزارش‌ها | `/reports` | `/admin/reports` |

مسیرهای دیگر به داشبورد هدایت می‌شوند؛ همه‌ی مسیرها به‌جز `/login` پشت `RequireAuth` هستند.

### احراز هویت

- بک‌اند **کوکی HttpOnly دوگانه** می‌فرستد: `ST_ADMIN_ACCESS` و `ST_ADMIN_REFRESH`
  (مسیر `/api/v1`). توکن‌ها برای سازگاری در بدنه JSON هم برمی‌گردند و کلاینت آن‌ها را
  در `sessionStorage` نگه می‌دارد.
- **CSRF فعال است**: توکن از `GET /api/v1/admin/csrf` گرفته و در هدر `X-XSRF-TOKEN`
  فرستاده می‌شود — ولی فقط وقتی درخواست Bearer ندارد (درخواست Bearer از CSRF معاف است).
- قفل حساب: ۵ تلاش ناموفق → ۱۵ دقیقه.
- 2FA سوپرادمین با `totpCode` پشتیبانی می‌شود (`docs/runbook.md` بخش ۸).

> ⚠️ **توجه:** کلاینت فعلی توکن را تازه‌سازی نمی‌کند — روی `401` نشست پاک می‌شود و
> کاربر به صفحه ورود برمی‌گردد. مسیر `POST /auth/refresh-admin` در بک‌اند موجود است
> ولی از این پنل صدا زده نمی‌شود.
