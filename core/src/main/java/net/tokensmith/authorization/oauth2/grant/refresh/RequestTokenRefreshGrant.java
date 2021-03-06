package net.tokensmith.authorization.oauth2.grant.refresh;

import net.tokensmith.authorization.authenticate.LoginConfidentialClient;
import net.tokensmith.authorization.authenticate.exception.UnauthorizedException;
import net.tokensmith.authorization.constant.ErrorCode;
import net.tokensmith.authorization.exception.BadRequestException;
import net.tokensmith.authorization.exception.ServerException;
import net.tokensmith.authorization.oauth2.grant.refresh.entity.TokenInputRefreshGrant;
import net.tokensmith.authorization.oauth2.grant.refresh.exception.CompromisedRefreshTokenException;
import net.tokensmith.authorization.oauth2.grant.refresh.factory.TokenInputRefreshGrantFactory;
import net.tokensmith.authorization.oauth2.grant.token.RequestTokenGrant;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenResponse;
import net.tokensmith.authorization.oauth2.grant.token.exception.BadRequestExceptionBuilder;
import net.tokensmith.authorization.oauth2.grant.token.exception.InvalidValueException;
import net.tokensmith.authorization.oauth2.grant.token.exception.MissingKeyException;
import net.tokensmith.authorization.oauth2.grant.token.exception.NotFoundException;
import net.tokensmith.authorization.oauth2.grant.token.exception.UnknownKeyException;
import net.tokensmith.authorization.security.ciphers.HashToken;
import net.tokensmith.repository.entity.ConfidentialClient;
import net.tokensmith.repository.entity.GrantType;
import net.tokensmith.repository.entity.RefreshToken;
import net.tokensmith.repository.entity.ResourceOwner;
import net.tokensmith.repository.entity.Scope;
import net.tokensmith.repository.entity.Token;
import net.tokensmith.repository.entity.TokenScope;
import net.tokensmith.repository.exceptions.RecordNotFoundException;
import net.tokensmith.repository.repo.RefreshTokenRepository;
import net.tokensmith.repository.repo.ResourceOwnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by tommackenzie on 10/8/16.
 */
@Component
public class RequestTokenRefreshGrant implements RequestTokenGrant {
    protected static final Logger logger = LoggerFactory.getLogger(RequestTokenRefreshGrant.class);

    private LoginConfidentialClient loginConfidentialClient;
    private TokenInputRefreshGrantFactory tokenInputRefreshGrantFactory;
    private HashToken hashToken;
    private RefreshTokenRepository refreshTokenRepository;
    private ResourceOwnerRepository resourceOwnerRepository;
    private IssueTokenRefreshGrant issueTokenRefreshGrant;

    private static String INVALID_GRANT = "invalid_grant";
    private static String REFRESH_TOKEN_NOT_FOUND = "refresh token was not found";
    private static String RESOURCE_OWNER_NOT_FOUND = "no resource owner was associated to refresh token";

    @Autowired
    public RequestTokenRefreshGrant(LoginConfidentialClient loginConfidentialClient, TokenInputRefreshGrantFactory tokenInputRefreshGrantFactory, HashToken hashToken, RefreshTokenRepository refreshTokenRepository, ResourceOwnerRepository resourceOwnerRepository, IssueTokenRefreshGrant issueTokenRefreshGrant) {
        this.loginConfidentialClient = loginConfidentialClient;
        this.tokenInputRefreshGrantFactory = tokenInputRefreshGrantFactory;
        this.hashToken = hashToken;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resourceOwnerRepository = resourceOwnerRepository;
        this.issueTokenRefreshGrant = issueTokenRefreshGrant;
    }

    @Override
    public TokenResponse request(UUID clientId, String clientPassword, Map<String, String> request) throws BadRequestException, NotFoundException, UnauthorizedException, ServerException {
        // login in a confidential client.
        ConfidentialClient cc = loginConfidentialClient.run(clientId, clientPassword);

        TokenInputRefreshGrant input;
        try {
            input = tokenInputRefreshGrantFactory.run(request);
        } catch (MissingKeyException e) {
            throw new BadRequestExceptionBuilder().MissingKey(e.getKey(), e).build();
        } catch (InvalidValueException e) {
            throw new BadRequestExceptionBuilder().InvalidKeyValue(e.getKey(), e.getCode(), e).build();
        } catch (UnknownKeyException e) {
            throw new BadRequestExceptionBuilder().UnknownKey(e.getKey(), e.getCode(), e).build();
        }

        String hashedRefreshToken = hashToken.run(input.getRefreshToken());
        RefreshToken refreshToken = getRefreshToken(cc.getClient().getId(), hashedRefreshToken);
        List<Scope> scopes = matchScopes(input.getScopes(), refreshToken.getToken().getTokenScopes());

        String accessToken = refreshToken.getToken().getToken();
        UUID resourceOwnerId = getResourceOwnerId(accessToken);

        Token leadToken;
        if (GrantType.REFRESSH.equals(refreshToken.getToken().getGrantType())) {
            // must have a lead token since it's token is a refresh grant.
            // the first refresh grant inserts a lead token.
            leadToken = refreshToken.getToken().getLeadToken();
        } else {
            // first time using refresh so no lead token is available.
            // it's token was the original token issued.
            leadToken = refreshToken.getToken();
        }

        TokenResponse tokenResponse = null;
        try {
            tokenResponse = issueTokenRefreshGrant.run(
                cc.getClient().getId(),
                resourceOwnerId,
                refreshToken.getToken().getId(),
                refreshToken.getId(),
                leadToken,
                scopes,
                refreshToken.getToken().getAudience()
            );
        } catch (CompromisedRefreshTokenException e) {
            logger.warn(e.getMessage(), e);
            throw new BadRequestExceptionBuilder().CompromisedRefreshToken(
                ErrorCode.COMPROMISED_REFRESH_TOKEN.getCode(),
                e
            ).build();
        } catch (ServerException e) {
            throw e;
        }

        return tokenResponse;
    }

    protected RefreshToken getRefreshToken(UUID clientId, String hashedRefreshToken) throws NotFoundException {
        RefreshToken refreshToken;

        try {
            refreshToken = refreshTokenRepository.getByClientIdAndAccessToken(clientId, hashedRefreshToken);
        } catch (RecordNotFoundException e) {
            throw new NotFoundException(
                REFRESH_TOKEN_NOT_FOUND,
                INVALID_GRANT,
                ErrorCode.REFRESH_TOKEN_NOT_FOUND.getDescription(),
                ErrorCode.REFRESH_TOKEN_NOT_FOUND.getCode(),
                e
            );
        }
        return refreshToken;
    }

    protected List<Scope> matchScopes(List<String> inputScopes, List<TokenScope> tokenScopes) throws BadRequestException {

        List<Scope> matchedScopes;

        if (inputScopes.size() > 0) {
            matchedScopes = tokenScopes.stream()
                    .map(item -> item.getScope())
                    .filter(item -> inputScopes.contains(item.getName()))
                    .collect(Collectors.toList());

            // matched scopes should be the same size as inputScopes.
            if (matchedScopes.size() != inputScopes.size()) {
                throw new BadRequestExceptionBuilder().InvalidScope(ErrorCode.SCOPES_NOT_SUPPORTED.getCode()).build();
            }
        } else {
            matchedScopes = tokenScopes.stream()
                    .map(item -> item.getScope())
                    .collect(Collectors.toList());
        }

        return matchedScopes;
    }

    protected UUID getResourceOwnerId(String accessToken) throws NotFoundException {
        ResourceOwner resourceOwner;
        try {
            resourceOwner = resourceOwnerRepository.getByAccessToken(accessToken);
        } catch (RecordNotFoundException e) {
            throw new NotFoundException(
                RESOURCE_OWNER_NOT_FOUND,
                INVALID_GRANT,
                ErrorCode.REFRESH_TOKEN_NOT_FOUND.getDescription(),
                ErrorCode.REFRESH_TOKEN_NOT_FOUND.getCode(),
                e
            );
        }
        return resourceOwner.getId();
    }
}
