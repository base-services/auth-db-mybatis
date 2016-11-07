package org.rootservices.authorization.oauth2.grant.redirect.code.token;

import org.rootservices.authorization.authenticate.LoginConfidentialClient;
import org.rootservices.authorization.authenticate.exception.UnauthorizedException;
import org.rootservices.authorization.constant.ErrorCode;
import org.rootservices.authorization.oauth2.grant.redirect.code.token.entity.TokenInputCodeGrant;
import org.rootservices.authorization.oauth2.grant.redirect.code.token.factory.TokenInputCodeGrantFactory;
import org.rootservices.authorization.oauth2.grant.token.RequestTokenGrant;
import org.rootservices.authorization.oauth2.grant.token.exception.*;
import org.rootservices.authorization.oauth2.grant.redirect.code.token.exception.CompromisedCodeException;
import org.rootservices.authorization.oauth2.grant.token.entity.Extension;
import org.rootservices.authorization.oauth2.grant.token.entity.TokenResponse;
import org.rootservices.authorization.oauth2.grant.token.entity.TokenType;
import org.rootservices.authorization.persistence.entity.*;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.repository.*;
import org.rootservices.authorization.security.HashTextStaticSalt;
import org.rootservices.authorization.security.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by tommackenzie on 5/24/15.
 */
@Component
public class RequestTokenCodeGrant implements RequestTokenGrant {
    private LoginConfidentialClient loginConfidentialClient;
    private TokenInputCodeGrantFactory tokenInputCodeGrantFactory;
    private BadRequestExceptionBuilder badRequestExceptionBuilder;
    private HashTextStaticSalt hashText;
    private AuthCodeRepository authCodeRepository;
    private IssueTokenCodeGrant issueTokenCodeGrant;

    @Autowired
    public RequestTokenCodeGrant(LoginConfidentialClient loginConfidentialClient, TokenInputCodeGrantFactory tokenInputCodeGrantFactory, BadRequestExceptionBuilder badRequestExceptionBuilder, HashTextStaticSalt hashText, AuthCodeRepository authCodeRepository, IssueTokenCodeGrant issueTokenCodeGrant) {
        this.loginConfidentialClient = loginConfidentialClient;
        this.tokenInputCodeGrantFactory = tokenInputCodeGrantFactory;
        this.badRequestExceptionBuilder = badRequestExceptionBuilder;
        this.hashText = hashText;
        this.authCodeRepository = authCodeRepository;
        this.issueTokenCodeGrant = issueTokenCodeGrant;
    }

    public TokenResponse request(UUID clientId, String clientPassword, Map<String, String> request) throws UnauthorizedException, NotFoundException, BadRequestException {

        // login in a confidential client.
        ConfidentialClient cc = loginConfidentialClient.run(clientId, clientPassword);

        TokenInputCodeGrant input = null;
        try {
            input = tokenInputCodeGrantFactory.run(request);
        } catch (UnknownKeyException e) {
            throw badRequestExceptionBuilder.UnknownKey(e.getKey(), e.getCode(), e).build();
        } catch (InvalidValueException e) {
            throw badRequestExceptionBuilder.InvalidKeyValue(e.getKey(), e.getCode(), e).build();
        } catch (MissingKeyException e) {
            throw badRequestExceptionBuilder.MissingKey(e.getKey(), e).build();
        }

        // fetch auth code
        String hashedCode = hashText.run(input.getCode());
        AuthCode authCode = fetchAndVerifyAuthCode(clientId, hashedCode, input.getRedirectUri());

        UUID resourceOwnerId = authCode.getAccessRequest().getResourceOwnerId();
        List<AccessRequestScope> accessRequestScopes = authCode.getAccessRequest().getAccessRequestScopes();

        TokenResponse tokenResponse;
        try {
            tokenResponse = issueTokenCodeGrant.run(
                    cc.getClient().getId(),
                    authCode.getId(),
                    resourceOwnerId,
                    accessRequestScopes
            );
        } catch (CompromisedCodeException e) {
            throw badRequestExceptionBuilder.CompromisedCode(e.getCode(), e).build();
        }

        return tokenResponse;
    }

    protected AuthCode fetchAndVerifyAuthCode(UUID clientUUID, String hashedCode, Optional<URI> tokenRequestRedirectUri) throws NotFoundException {
        AuthCode authCode;
        try {
            authCode = authCodeRepository.getByClientIdAndAuthCode(clientUUID, hashedCode);
        } catch (RecordNotFoundException e) {
            // TODO: security - could a client be phishing for other client's auth codes?
            throw new NotFoundException(
                    "Access Request was not found",
                    "invalid_grant",
                    ErrorCode.AUTH_CODE_NOT_FOUND.getDescription(),
                    ErrorCode.AUTH_CODE_NOT_FOUND.getCode(),
                    e
            );
        }

        if ( ! doRedirectUrisMatch(tokenRequestRedirectUri, authCode.getAccessRequest().getRedirectURI()) ) {
            throw new NotFoundException(
                    "Access Request was not found",
                    "invalid_grant",
                    ErrorCode.REDIRECT_URI_MISMATCH.getDescription(),
                    ErrorCode.REDIRECT_URI_MISMATCH.getCode(),
                    null
            );
        }
        return authCode;
    }

    protected Boolean doRedirectUrisMatch(Optional<URI> redirectUriA, Optional<URI> redirectUriB) {
        Boolean matches = true;
        if ( redirectUriA.isPresent() && ! redirectUriB.isPresent()) {
            matches = false;
        } else if ( !redirectUriA.isPresent() && redirectUriB.isPresent()) {
            matches = false;
        } else if ( redirectUriA.get().equals(redirectUriB.get())) {
            matches = true;
        }
        return matches;
    }
}
