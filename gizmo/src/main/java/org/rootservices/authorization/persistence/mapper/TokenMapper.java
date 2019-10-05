package org.rootservices.authorization.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.rootservices.authorization.persistence.entity.Token;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;


@Repository
public interface TokenMapper {
    Token getByAuthCodeId(@Param("authCodeId") UUID authCodeId);
    Token getById(@Param("id") UUID id);
    void insert(@Param("token") Token token);
    void revokeByAuthCodeId(@Param("authCodeId") UUID authCodeId);
    void revokeById(@Param("id") UUID id);
    void updateExpiresAtByAccessToken(@Param("expiresAt") OffsetDateTime expiresAt, @Param("accessToken") String accessToken);
    void revokeActive(@Param("resourceOwnerId") UUID resourceOwnerId);
}