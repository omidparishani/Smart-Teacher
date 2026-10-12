package ir.omid.smartteacher.eitaa.internal.application;

import ir.omid.smartteacher.eitaa.internal.api.dto.MessageCreditResponse;
import ir.omid.smartteacher.eitaa.internal.domain.MessageCredit;
import ir.omid.smartteacher.eitaa.internal.domain.MessageCreditRepository;
import ir.omid.smartteacher.tenant.TenantContext;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * اعتبار پیام مسیر ب (برنامه) — ۵۰٬۰۰۰ رایگان به‌ازای هر مستأجر.
 */
@Service
public class MessageCreditService {

    private static final Logger log = LoggerFactory.getLogger(MessageCreditService.class);

    public static final int DEFAULT_BALANCE = 50_000;

    private final MessageCreditRepository repository;

    public MessageCreditService(MessageCreditRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public MessageCreditResponse currentBalance() {
        MessageCredit credit = getOrCreateReadOnly();
        return toResponse(credit);
    }

    /**
     * یک اعتبار پس از ارسال موفق مسیر کاربر (app.sendMessage) کم می‌شود.
     */
    @Transactional
    public void decrementOnSuccessfulUserSend() {
        UUID tenantId = TenantContext.requireTenantId();
        MessageCredit credit = repository.findByTenantId(tenantId)
                .orElseGet(() -> repository.save(new MessageCredit(DEFAULT_BALANCE, DEFAULT_BALANCE)));
        if (credit.getBalance() <= 0) {
            log.warn("اعتبار پیام مستأجر {} تمام شده است", tenantId);
            return;
        }
        credit.setBalance(credit.getBalance() - 1);
        repository.save(credit);
    }

    private MessageCredit getOrCreateReadOnly() {
        return repository.findByTenantId(TenantContext.requireTenantId())
                .orElse(new MessageCredit(DEFAULT_BALANCE, DEFAULT_BALANCE));
    }

    private static MessageCreditResponse toResponse(MessageCredit credit) {
        return new MessageCreditResponse(credit.getBalance(), credit.getFreeQuota());
    }
}
