package ir.omid.smartteacher.eitaa;

import ir.omid.smartteacher.eitaa.internal.application.MessageCreditService;
import org.mockito.Mockito;

/** اعتبار پیام ساختگی برای تست‌های واحد آداپتر mock. */
final class EitaaTestCredits {

    private EitaaTestCredits() {
    }

    static MessageCreditService noop() {
        return Mockito.mock(MessageCreditService.class);
    }
}
