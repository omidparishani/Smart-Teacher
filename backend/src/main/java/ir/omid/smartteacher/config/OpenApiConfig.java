package ir.omid.smartteacher.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * پیکربندی مستندات OpenAPI.
 *
 * <p>فقط مدل‌های {@code io.swagger.v3.oas.models} استفاده شده‌اند که
 * بخشی از swagger-core هستند و بین نسخه‌های springdoc پایدارند. کلاس‌های
 * مخصوص springdoc (مثل گروه‌بندی) عمداً کنار گذاشته شده‌اند تا نسخه
 * ۳.x که تازه منتشر شده، نقطه شکست اضافه نسازد.
 *
 * <p><b>⚠️ امنیت مستندات:</b> در پروفایل production این مسیرها کاملاً
 * خاموش می‌شوند ({@code springdoc.api-docs.enabled: false}). Swagger
 * در production یعنی نقشه کامل API برای مهاجم — حتی بدون اجرای
 * درخواست، فهرست مسیرها و ساختار داده‌ها را می‌دهد.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI smartTeacherOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("دستیار هوشمند معلم")
                        .version("v1")
                        .description("""
                                API سامانه آموزشی «دستیار هوشمند معلم».

                                **احراز هویت:** همه مسیرهای محافظت‌شده به توکن
                                `Bearer` نیاز دارند. توکن را در هدر
                                `Authorization` بفرستید.

                                **چند‌مستأجری:** شناسه مستأجر هرگز در درخواست
                                فرستاده نمی‌شود؛ از توکن خوانده می‌شود. اگر
                                بخواهید به رکوردی از مستأجر دیگر دسترسی بگیرید،
                                پاسخ `404` می‌گیرید، نه `403` — این عمدی است.
                                """)
                        .license(new License().name("Proprietary")))
                .servers(List.of(new Server()
                        .url("/")
                        .description("سرور جاری")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("توکن دسترسی صادرشده توسط /auth")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
