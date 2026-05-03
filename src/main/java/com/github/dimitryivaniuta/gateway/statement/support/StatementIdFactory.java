package com.github.dimitryivaniuta.gateway.statement.support;

import java.time.YearMonth;
import org.springframework.stereotype.Component;

/**
 * Builds deterministic statement identifiers from business-stable input.
 */
@Component
public class StatementIdFactory {

    /**
     * Creates the statement identifier.
     *
     * @param accountId account identifier.
     * @param month statement month.
     * @param sourceChecksum source checksum.
     * @return deterministic statement identifier.
     */
    public String create(final String accountId, final YearMonth month, final String sourceChecksum) {
        String seed = accountId + "|" + month + "|" + sourceChecksum + "|statement-v1";
        return "stmt_" + Sha256Support.hex(seed).substring(0, 24);
    }
}
