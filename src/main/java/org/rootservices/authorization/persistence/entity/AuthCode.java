package org.rootservices.authorization.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Created by tommackenzie on 4/8/15.
 */
public class AuthCode {
    private UUID uuid;
    private byte[] code;
    private Boolean revoked;
    private AccessRequest accessRequest;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;

    public AuthCode() {}

    public AuthCode(UUID uuid, byte[] code, AccessRequest accessRequest, OffsetDateTime expiresAt) {
        this.uuid = uuid;
        this.code = code;
        this.revoked = false;
        this.accessRequest = accessRequest;
        this.expiresAt = expiresAt;
    }

    public AuthCode(UUID uuid, byte[] code, Boolean revoked, AccessRequest accessRequest, OffsetDateTime expiresAt) {
        this.uuid = uuid;
        this.code = code;
        this.revoked = revoked;
        this.accessRequest = accessRequest;
        this.expiresAt = expiresAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public byte[] getCode() {
        return code;
    }

    public void setCode(byte[] code) {
        this.code = code;
    }

    public Boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public AccessRequest getAccessRequest() {
        return accessRequest;
    }

    public void setAccessRequest(AccessRequest accessRequest) {
        this.accessRequest = accessRequest;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
