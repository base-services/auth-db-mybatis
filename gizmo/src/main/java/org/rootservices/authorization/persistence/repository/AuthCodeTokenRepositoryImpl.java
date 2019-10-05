package org.rootservices.authorization.persistence.repository;


import org.rootservices.authorization.persistence.entity.AuthCodeToken;
import org.rootservices.authorization.persistence.exceptions.DuplicateRecordException;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.mapper.AuthCodeTokenMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created by tommackenzie on 4/16/16.
 */
@Component
public class AuthCodeTokenRepositoryImpl implements AuthCodeTokenRepository {
    private static String DUPLICATE_ERROR_MSG = "Could not insert auth_code_token record.";
    private static String RECORD_NOT_FOUND_MSG = "auth_code_token record was not found";
    private AuthCodeTokenMapper authCodeTokenMapper;

    @Autowired
    public AuthCodeTokenRepositoryImpl(AuthCodeTokenMapper authCodeTokenMapper) {
        this.authCodeTokenMapper = authCodeTokenMapper;
    }

    @Override
    public void insert(AuthCodeToken authCodeToken) throws DuplicateRecordException {
        try {
            authCodeTokenMapper.insert(authCodeToken);
        } catch (DuplicateKeyException e) {
            throw new DuplicateRecordException(DUPLICATE_ERROR_MSG, e);
        }
    }

    @Override
    public AuthCodeToken getByTokenId(UUID tokenId) throws RecordNotFoundException {
        AuthCodeToken authCodeToken = authCodeTokenMapper.getByTokenId(tokenId);

        if (authCodeToken == null) {
            throw new RecordNotFoundException(RECORD_NOT_FOUND_MSG);
        }

        return authCodeToken;
    }
}