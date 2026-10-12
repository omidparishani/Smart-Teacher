# ═══════════════════════════════════════════════════════════
#  Dockerfile — دستیار هوشمند معلم (بک‌اند)
#
#  چندمرحله‌ای: ابزار ساخت (Maven + JDK) در ایمیج نهایی نمی‌ماند.
#  دلیلش فقط حجم نیست — ماندن JDK و Maven یعنی سطح حمله بزرگ‌تر و
#  ابزارهایی که مهاجم پس از نفوذ می‌تواند به کار ببرد.
# ═══════════════════════════════════════════════════════════

# ─────────────────────────── مرحله ۱: ساخت ───────────────────────────
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# ── تعریف Toolchain ──
# ⚠️ چرا لازم است؟ pom.xml از maven-toolchains-plugin استفاده می‌کند تا
# بیلد مستقل از JAVA_HOME سیستم به JDK 25 گره بخورد. آن پلاگین فایل
# ~/.m2/toolchains.xml می‌خواهد؛ در ایمیج Docker چنین فایلی وجود ندارد
# و بیلد بدون آن شکست می‌خورد. چون خود ایمیج JDK 25 دارد، همان را
# به‌عنوان toolchain معرفی می‌کنیم.
# این فایل فقط در مرحله ساخت است و به ایمیج نهایی نمی‌رسد.
RUN mkdir -p /root/.m2 && printf '%s\n' \
    '<?xml version="1.0" encoding="UTF-8"?>' \
    '<toolchains>' \
    '  <toolchain>' \
    '    <type>jdk</type>' \
    '    <provides><version>25</version><vendor>eclipse</vendor></provides>' \
    '    <configuration>' \
    "      <jdkHome>${JAVA_HOME}</jdkHome>" \
    '    </configuration>' \
    '  </toolchain>' \
    '</toolchains>' > /root/.m2/toolchains.xml

# ── لایه وابستگی‌ها ──
# pom.xml تنها کپی می‌شود تا این لایه تا زمانی که وابستگی‌ها عوض
# نشده‌اند از کش بیاید. کپی‌کردن کل src اینجا، هر تغییر یک خط کد را
# به دانلود دوباره همه وابستگی‌ها تبدیل می‌کند.
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

# ── ساخت ──
COPY src ./src

# ⚠️ تست‌ها اینجا اجرا نمی‌شوند: تست جداسازی مستأجر به Docker نیاز
# دارد و ساختن ایمیج داخل خود Docker، یک وابستگی حلقه‌ای می‌سازد.
# تست‌ها پیش از ساخت ایمیج، روی CI یا ماشین توسعه‌دهنده اجرا می‌شوند.
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

# ─────────────────────────── مرحله ۲: اجرا ───────────────────────────
FROM eclipse-temurin:25-jre-alpine

# ── کاربر غیر-روت ──
# ⚠️ اگر فرآیند با کاربر root اجرا شود و مهاجم از یک آسیب‌پذیری نفوذ
# کند، همان دسترسی root را در کانتینر دارد و می‌تواند باینری بریزد یا
# از کانتینر خارج شود. کاربر بدون امتیاز، این مسیر را می‌بندد.
RUN addgroup -S -g 10001 smartteacher \
 && adduser  -S -u 10001 -G smartteacher -s /sbin/nologin smartteacher

WORKDIR /app

COPY --from=builder --chown=smartteacher:smartteacher \
     /build/target/smart-teacher-backend.jar app.jar

USER smartteacher

EXPOSE 8080

# ── تنظیم حافظه ──
# MaxRAMPercentage به‌جای -Xmx ثابت: کانتینر ممکن است با محدودیت حافظه
# متفاوتی اجرا شود و مقدار ثابت یا هدر می‌رود یا OOM می‌دهد.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# ── پروفایل پیش‌فرض ──
# ⚠️ «dev» فقط پیش‌فرض است؛ در production باید صریحاً
# SPRING_PROFILES_ACTIVE=prod تنظیم شود (docker-compose این کار را
# می‌کند). در پروفایل prod مستندسازی API خاموش است.
ENV SPRING_PROFILES_ACTIVE=dev

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
