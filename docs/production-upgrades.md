# Production-grade upgrades included

1. **Transactional artifact + outbox write**
   - Prevents an artifact from being stored without its matching outbox event.

2. **Download cache validators**
   - ETag and Last-Modified allow efficient re-downloads and browser/client validation.

3. **Audit revision endpoint**
   - Makes immutable history easy to inspect without downloading every artifact.

4. **Payload size persistence**
   - Allows exact byte-length audit validation and stable Content-Length behavior.

5. **Outbox claim + retry model**
   - Adds retry attempts, next retry time, last error, and claim ownership to reduce duplicate publishes in multi-instance deployments.

6. **Safer Redis lock behavior**
   - Generation now respects lock acquisition results instead of continuing blindly.

7. **Parameter name retention in compilation**
   - Adds `-parameters` to make repository named parameters safer.

8. **Prometheus registry dependency**
   - Matches the exposed Prometheus actuator endpoint.

9. **Modern Flyway PostgreSQL module**
   - Adds the PostgreSQL-specific Flyway module required by modern Flyway packaging.
