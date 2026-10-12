package ir.omid.smartteacher.eitaa;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * سرور ساختگی ایتا — برای سنجیدن لایه HTTP واقعی.
 *
 * <p><b>🔴 چرا نمی‌شود این آزمون‌ها را روی {@code MockEitaaAdapter}
 * نوشت؟</b> چون شبیه‌ساز همه‌چیز را دور می‌زند: هیچ HTTP ای انجام
 * نمی‌شود، هیچ پاسخی پارس نمی‌شود، هیچ {@code ok} ای بررسی نمی‌شود. اگر
 * آزمون «{@code ok:false} موفق تلقی نشود» روی شبیه‌ساز نوشته شود، یک
 * آزمون سبز تولید می‌کند که هیچ‌کدام از آن سه لایه را لمس نکرده —
 * یعنی امنیتی که فکر می‌کنیم داریم و نداریم.
 *
 * <p>این سرور روی یک درگاه محلی گوش می‌دهد، درخواست‌ها را ثبت می‌کند،
 * و هر پاسخی که آزمون بخواهد برمی‌گرداند — از جمله پاسخ‌های بدشکل.
 * پس می‌شود دقیقاً همان حالتی را ساخت که در Production نادر است و
 * همان حالتی که اگر درست گرفته نشود، گران تمام می‌شود.
 *
 * <p>هیچ درخواستی به بیرون نمی‌رود: ریشه URL به {@code 127.0.0.1}
 * اشاره می‌کند. این مهم است — آزمون نباید بتواند پیام واقعی به گروه
 * واقعی یک معلم بفرستد.
 */
final class EitaaStubServer implements AutoCloseable {

    /** یک درخواست دریافتی — برای بازرسی بدنه و مسیر. */
    record Received(String method, String path, String body, String contentType) {
    }

    /** پاسخ پیش‌فرض: موفق، با شناسه پیام. */
    private static final String DEFAULT_RESPONSE =
            "{\"ok\":true,\"result\":{\"message_id\":\"85917\"}}";

    private final HttpServer server;
    private final List<Received> requests = new CopyOnWriteArrayList<>();

    private volatile int responseStatus = 200;
    private volatile String responseBody = DEFAULT_RESPONSE;

    /** دنباله پاسخ‌ها؛ خالی یعنی «همیشه {@link #responseBody}». */
    private volatile List<String> responseBodies = List.of();
    private final AtomicInteger responseIndex = new AtomicInteger();

    private EitaaStubServer(HttpServer server) {
        this.server = server;
    }

    static EitaaStubServer start() throws IOException {
        // درگاه صفر یعنی «هر درگاه آزادی» — بدون برخورد با آزمون‌های
        // هم‌زمان و بدون درگاه ثابتی که ممکن است اشغال باشد.
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        EitaaStubServer stub = new EitaaStubServer(server);

        server.createContext("/", stub::handle);
        server.start();

        return stub;
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

        requests.add(new Received(
                exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body, contentType));

        // ⚠️ نمایه پیش از پاسخ مصرف می‌شود تا هر درخواست دقیقاً یک
        // پاسخ از دنباله بگیرد. پس از تمام‌شدن دنباله، آخرین پاسخ
        // تکرار می‌شود (بازه با min محدود می‌شود).
        List<String> sequence = responseBodies;
        String bodyToSend = sequence.isEmpty()
                ? responseBody
                : sequence.get(Math.min(responseIndex.getAndIncrement(), sequence.size() - 1));

        byte[] payload = bodyToSend.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(responseStatus, payload.length);

        try (var out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    // ─────────────────────────── ریشه ───────────────────────────

    /** ریشه مسیر الف — بدون اسلش انتهایی، تا با کد یکی باشد. */
    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api";
    }

    /** ریشه مسیر ب. */
    String appBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/app";
    }

    /** پیکربندی با هر دو توکن، رو به این سرور. */
    EitaaProperties properties() {
        return EitaaTestData.withBaseUrl(EitaaTestData.fullProperties(), baseUrl(), appBaseUrl());
    }

    // ─────────────────────────── پاسخ ───────────────────────────

    void respondWith(int status, String body) {
        this.responseStatus = status;
        this.responseBody = body;
        // پاک‌کردن دنباله: وگرنه یک پاسخ ثابت که پس از یک دنباله
        // تنظیم شود، بی‌صدا نادیده گرفته می‌شود.
        this.responseBodies = List.of();
    }

    void respondOkWith(String resultJson) {
        respondWith(200, "{\"ok\":true,\"result\":" + resultJson + "}");
    }

    /** پاسخ ناموفق در HTTP ۲۰۰ — همان حالت خطرناک. */
    void respondNotOk(String errorCode, String description) {
        respondWith(200, "{\"ok\":false,\"error_code\":\"" + errorCode
                + "\",\"description\":\"" + description + "\"}");
    }

    void resetResponse() {
        respondWith(200, DEFAULT_RESPONSE);
    }

    /**
     * پاسخ‌های پشت‌سرهم — یکی برای هر درخواست.
     *
     * <p>لازم است برای سنجیدن حالت «بخش اول رفت، بخش دوم شکست خورد».
     * با یک پاسخ ثابت، آن حالت قابل ساختن نیست و آزمون مجبور می‌شود
     * چیزی را بسنجد که همان حالت نیست.
     *
     * <p>پس از تمام‌شدن فهرست، آخرین پاسخ تکرار می‌شود.
     */
    void respondSequence(int status, String... bodies) {
        this.responseStatus = status;
        this.responseBodies = List.of(bodies);
        this.responseIndex.set(0);
        this.responseBody = bodies[bodies.length - 1];
    }

    /** دنباله موفقیت برای بخش‌های اول، و شکست برای بقیه. */
    void respondOkThenFail(int okCount, String errorCode, String description) {
        String[] bodies = new String[okCount + 1];
        for (int i = 0; i < okCount; i++) {
            bodies[i] = DEFAULT_RESPONSE;
        }
        bodies[okCount] = "{\"ok\":false,\"error_code\":\"" + errorCode
                + "\",\"description\":\"" + description + "\"}";

        respondSequence(200, bodies);
    }

    // ─────────────────────────── بازرسی ───────────────────────────

    List<Received> requests() {
        return List.copyOf(requests);
    }

    Received lastRequest() {
        return requests.isEmpty() ? null : requests.get(requests.size() - 1);
    }

    int requestCount() {
        return requests.size();
    }

    void clearRequests() {
        requests.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
