package ir.omid.smartteacher.security.web;

import ir.omid.smartteacher.common.error.ApiError;
import ir.omid.smartteacher.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
// ⚠️ Jackson 3 (tools.jackson) — نه Jackson 2. Spring Boot 4 پیکربندی
// خودکار جکسون را به ماژول spring-boot-jackson برده و آن ماژول Jackson 3
// را سیم‌کشی می‌کند؛ نسخه‌ی com.fasterxml.jackson Beanی ندارد.
import tools.jackson.databind.ObjectMapper;

/**
 * پاسخ ۴۰۱ با قالب استاندارد {@link ApiError}.
 *
 * <p><b>چرا لازم است؟</b> پاسخ پیش‌فرض Spring Security یک صفحه HTML یا
 * یک بدنه خالی با هدر {@code WWW-Authenticate} است. اگر بگذاریم همان
 * بماند، کلاینت Mini App برای هر خطای احراز هویت با یک شکل داده ناهمگون
 * روبه‌رو می‌شود و باید دو مسیر کد جدا برای تحلیل خطا داشته باشد.
 *
 * <p>⚠️ این کلاس هرگز دلیل واقعی رد شدن را نمی‌گوید. «توکن منقضی
 * شده» و «امضا نامعتبر است» از بیرون یکی به نظر می‌رسند.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorCode code = ErrorCode.UNAUTHENTICATED;

        // اگر فیلتر JWT دلیل شکست را ثبت کرده باشد، همان را برمی‌گردانیم
        // تا کاربر بفهمد «نشست منقضی شده» یا «باید وارد شوم».
        Object reason = request.getAttribute(JwtAuthenticationFilter.ATTRIBUTE_FAILURE_REASON);
        if (reason instanceof ErrorCode failureCode) {
            code = failureCode;
        }

        response.setStatus(code.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = ApiError.of(code, code.defaultMessage(),
                JwtAuthenticationFilter.resolveTraceId(request), request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
