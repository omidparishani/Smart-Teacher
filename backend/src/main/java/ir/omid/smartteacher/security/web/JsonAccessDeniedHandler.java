package ir.omid.smartteacher.security.web;

import ir.omid.smartteacher.common.error.ApiError;
import ir.omid.smartteacher.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
// ⚠️ Jackson 3 (tools.jackson) — نه Jackson 2. Spring Boot 4 پیکربندی
// خودکار جکسون را به ماژول spring-boot-jackson برده و آن ماژول Jackson 3
// را سیم‌کشی می‌کند؛ نسخه‌ی com.fasterxml.jackson Beanی ندارد.
import tools.jackson.databind.ObjectMapper;

/**
 * پاسخ ۴۰۳ با قالب استاندارد {@link ApiError}.
 *
 * <p><b>تفاوتش با {@code TenantGuard} را اشتباه نگیرید:</b> اینجا کاربر
 * عملیاتی را خواسته که <i>نوع</i> آن را مجاز نیست انجام دهد (مثل
 * حذف یک کلاس بدون مجوز). هیچ رکورد مشخصی هدف نبوده، پس {@code 403}
 * درست است و چیزی لو نمی‌دهد.
 *
 * <p>در مقابل، وقتی رکورد مشخصی با شناسه خواسته می‌شود و مالکش مستأجر
 * دیگری است، {@code TenantGuard} عمداً {@code 404} می‌دهد تا وجود
 * رکورد لو نرود.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        ErrorCode code = ErrorCode.ACCESS_DENIED;

        response.setStatus(code.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = ApiError.of(code, code.defaultMessage(),
                JwtAuthenticationFilter.resolveTraceId(request), request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
