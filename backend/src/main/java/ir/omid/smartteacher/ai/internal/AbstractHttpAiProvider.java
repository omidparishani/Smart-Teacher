package ir.omid.smartteacher.ai.internal;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.AiProviderException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * پایهٔ مشترک Providerهای HTTP — <b>بدون نام فیلد JSON حدسی</b>.
 *
 * <p>زیرکلاس‌ها پس از مطالعهٔ مستندات رسمی API، متدهای abstract
 * {@link #buildRequestPayload(AiRequest)}، {@link #parseSuccessResponse(byte[], AiRequest)}
 * و {@link #resolveEndpoint(AiRequest)} را پیاده می‌کنند. تا آن زمان
 * {@link #generate(AiRequest)} باید خطای پیکربندی بدهد.
 *
 * <p>این کلاس فقط زیرساخت HTTP (کلاینت، timeout، placeholder retry) را
 * فراهم می‌کند — نگاشت فیلدهای درخواست/پاسخ در subclasses است.
 */
public abstract class AbstractHttpAiProvider implements AiProvider {

    protected final HttpClient httpClient;
    protected final Duration defaultTimeout;
    protected final int maxRetries;

    protected AbstractHttpAiProvider(HttpClient httpClient,
                                     Duration defaultTimeout,
                                     int maxRetries) {
        this.httpClient = httpClient;
        this.defaultTimeout = defaultTimeout;
        this.maxRetries = Math.max(0, maxRetries);
    }

    /** کلاینت پیش‌فرض JDK — connect timeout قابل تنظیم. */
    protected static HttpClient createDefaultClient(Duration connectTimeout) {
        return HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    /**
     * ساخت بدنهٔ درخواست HTTP — <b>فقط پس از تأیید مستندات رسمی</b>.
     *
     * <p>نام فیلدهای JSON را حدس نزنید؛ از schema رسمی provider استفاده کنید.
     */
    protected abstract byte[] buildRequestPayload(AiRequest request);

    /**
     * پارس پاسخ موفق HTTP — <b>فقط پس از تأیید مستندات رسمی</b>.
     */
    protected abstract AiResponse parseSuccessResponse(byte[] body, AiRequest request);

    /** آدرس endpoint — از baseUrl پیکربندی‌شده یا مستندات رسمی. */
    protected abstract URI resolveEndpoint(AiRequest request);

    /** هدرهای HTTP — subclasses پس از مستندات رسمی تکمیل می‌کنند. */
    protected abstract Map<String, String> buildRequestHeaders(AiRequest request);

    /** Content-Type درخواست — پیش‌فرض application/json؛ subclass می‌تواند override کند. */
    protected String requestContentType() {
        return "application/json";
    }

    /**
     * آیا پیکربندی کامل است و می‌توان HTTP فرستاد؟
     *
     * <p>اسکلت‌ها {@code false} برمی‌گردانند تا Mock ناخواسته جایگزین نشود.
     */
    protected abstract boolean isHttpConfigured();

    /**
     * اجرای HTTP با retry placeholder.
     *
     * <p>Retry فعلاً فقط ساختار دارد — سیاست دقیق (کدام status codeها، backoff)
     * پس از تعریف Adapter واقعی تکمیل می‌شود.
     */
    protected AiResponse executeHttp(AiRequest request) {
        if (!isHttpConfigured()) {
            throw new AiProviderException(code(),
                    code() + " هنوز به مستندات رسمی وصل نشده است",
                    false);
        }

        Duration timeout = request.timeout() != null ? request.timeout() : defaultTimeout;
        AiProviderException lastError = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                byte[] payload = buildRequestPayload(request);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(resolveEndpoint(request))
                        .timeout(timeout)
                        .header("Content-Type", requestContentType())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(payload));

                buildRequestHeaders(request).forEach(builder::header);

                HttpResponse<byte[]> response = httpClient.send(
                        builder.build(),
                        HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseSuccessResponse(response.body(), request);
                }

                lastError = new AiProviderException(code(),
                        "HTTP " + response.statusCode(),
                        response.statusCode() >= 500 || response.statusCode() == 429);
            } catch (AiProviderException e) {
                lastError = e;
                if (!e.isFallbackEligible() || attempt >= maxRetries) {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiProviderException(code(), "HTTP interrupted", true, e);
            } catch (Exception e) {
                lastError = new AiProviderException(code(), humanizeTransportError(e), true, e);
                if (attempt >= maxRetries) {
                    throw lastError;
                }
            }
            // TODO: backoff بین تلاش‌ها — پس از تعریف سیاست retry رسمی
        }

        throw lastError != null
                ? lastError
                : new AiProviderException(code(), "HTTP failed", true);
    }

    /**
     * پیام فنی JDK (مثل PKIX) را به توضیح فارسی قابل‌اقدام تبدیل می‌کند.
     *
     * <p>⚠️ عمداً SSL را غیرفعال نمی‌کنیم — دور زدن trust store یک حفره
     * امنیتی است. راه‌حل درست: وارد کردن CA پروکسی/آنتی‌ویروس به truststore
     * جاوا، یا استفاده از baseUrl بدون MITM.
     */
    protected static String humanizeTransportError(Throwable error) {
        String raw = rootMessage(error);
        String lower = raw.toLowerCase();
        if (lower.contains("pkix")
                || lower.contains("certpath")
                || lower.contains("certificate_unknown")
                || lower.contains("unable to find valid certification path")) {
            return "خطای گواهی SSL (PKIX): جاوا به زنجیرهٔ گواهی سرور/پروکسی اعتماد ندارد. "
                    + "اگر آنتی‌ویروس یا پروکسی HTTPS را باز می‌کند، گواهی CA آن را به "
                    + "truststore همان JDK اضافه کنید؛ یا baseUrl مستقیم و معتبر بگذارید "
                    + "(برای Groq معمولاً https://api.groq.com/openai/v1).";
        }
        if (lower.contains("connection refused") || lower.contains("connect timed out")) {
            return "اتصال به سرور Provider برقرار نشد: " + raw;
        }
        return raw.isBlank() ? "خطای شبکه در فراخوانی Provider" : raw;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        String message = current.getMessage() == null ? "" : current.getMessage();
        while (current.getCause() != null) {
            current = current.getCause();
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
        }
        return message;
    }

    @Override
    public <T> AiStructuredResponse<T> generateStructured(AiRequest request, Class<T> responseType) {
        throw new AiProviderException(code(),
                "generateStructured برای " + code() + " هنوز پیاده نشده است",
                false);
    }

    @Override
    public boolean supports(String capability) {
        return false;
    }

    @Override
    public List<AiModelDescriptor> models() {
        return List.of();
    }
}
