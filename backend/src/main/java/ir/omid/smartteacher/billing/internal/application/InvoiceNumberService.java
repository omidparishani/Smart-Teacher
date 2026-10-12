package ir.omid.smartteacher.billing.internal.application;

import java.time.Year;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * شماره‌گذاری اتمیک فاکتور — {@code INV-YYYY-NNNNNN}.
 *
 * <p>با {@code INSERT ... ON CONFLICT} + {@code UPDATE ... RETURNING}
 * روی {@code invoice_sequences} بدون race روی شماره یکتا.
 */
@Service
public class InvoiceNumberService {

    private final JdbcTemplate jdbc;

    public InvoiceNumberService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public String nextInvoiceNumber() {
        int year = Year.now(ZoneOffset.UTC).getValue();
        jdbc.update("""
                INSERT INTO invoice_sequences (year_key, next_value, updated_at)
                VALUES (?, 1, now())
                ON CONFLICT (year_key) DO NOTHING
                """, year);

        Long value = jdbc.queryForObject("""
                UPDATE invoice_sequences
                   SET next_value = next_value + 1,
                       updated_at = now()
                 WHERE year_key = ?
             RETURNING next_value - 1
                """, Long.class, year);

        long seq = value == null ? 1L : value;
        return "INV-" + year + "-" + String.format("%06d", seq);
    }
}
