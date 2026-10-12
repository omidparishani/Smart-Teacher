# ─────────────────────────────────────────────────────────────
# Smart Teacher — Admin Panel Dockerfile (Multi-stage build)
# ─────────────────────────────────────────────────────────────

# Stage 1: Build
FROM node:22-alpine AS builder

WORKDIR /app

# کپی فایل‌های پکیج و نصب وابستگی‌ها
COPY package*.json ./
RUN npm ci

# کپی سورس‌کد و بیلد
COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine

# کپی خروجی بیلد به ریشه Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# کپی کانفیگ اختصاصی SPA
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
