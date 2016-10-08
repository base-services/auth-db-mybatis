package org.rootservices.authorization.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Created by tommackenzie on 10/8/16.
 */
public class TokenChain {
    private UUID id;
    private Token nextToken;
    private Token previousToken;
    private RefreshToken refreshToken;
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Token getNextToken() {
        return nextToken;
    }

    public void setNextToken(Token nextToken) {
        this.nextToken = nextToken;
    }

    public Token getPreviousToken() {
        return previousToken;
    }

    public void setPreviousToken(Token previousToken) {
        this.previousToken = previousToken;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
