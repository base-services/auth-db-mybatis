package helper.fixture.persistence.openid;

import helper.fixture.FixtureFactory;
import helper.fixture.persistence.LoadConfClientTokenReady;
import net.tokensmith.authorization.security.RandomString;
import net.tokensmith.authorization.security.ciphers.HashToken;
import net.tokensmith.repository.entity.AuthCode;
import net.tokensmith.repository.entity.AuthCodeToken;
import net.tokensmith.repository.entity.GrantType;
import net.tokensmith.repository.entity.RefreshToken;
import net.tokensmith.repository.entity.ResourceOwner;
import net.tokensmith.repository.entity.ResourceOwnerToken;
import net.tokensmith.repository.entity.Scope;
import net.tokensmith.repository.entity.Token;
import net.tokensmith.repository.entity.TokenAudience;
import net.tokensmith.repository.entity.TokenScope;
import net.tokensmith.repository.exceptions.DuplicateRecordException;
import net.tokensmith.repository.repo.AuthCodeTokenRepository;
import net.tokensmith.repository.repo.RefreshTokenRepository;
import net.tokensmith.repository.repo.ResourceOwnerTokenRepository;
import net.tokensmith.repository.repo.TokenAudienceRepository;
import net.tokensmith.repository.repo.TokenRepository;
import net.tokensmith.repository.repo.TokenScopeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by tommackenzie on 1/24/16.
 *
 * Loads all data associated with a confidential client.
 *  - scopes (openid)
 *  - resource owner
 *  - access request
 *  - access request scopes (openid)
 *  - auth code
 *  - token
 */
@Component
public class LoadOpenIdConfClientAll {
    private LoadConfClientTokenReady loadConfClientOpendIdTokenReady;
    private RandomString randomString;
    private TokenRepository tokenRepository;
    private TokenScopeRepository tokenScopeRepository;
    private AuthCodeTokenRepository authCodeTokenRepository;
    private TokenAudienceRepository clientTokenRepository;
    private ResourceOwnerTokenRepository resourceOwnerTokenRepository;
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public LoadOpenIdConfClientAll(LoadConfClientTokenReady loadConfClientOpendIdTokenReady, RandomString randomString, HashToken hashToken, TokenRepository tokenRepository, TokenScopeRepository tokenScopeRepository, AuthCodeTokenRepository authCodeTokenRepository, TokenAudienceRepository clientTokenRepository, ResourceOwnerTokenRepository resourceOwnerTokenRepository, RefreshTokenRepository refreshTokenRepository){
        this.loadConfClientOpendIdTokenReady = loadConfClientOpendIdTokenReady;
        this.randomString = randomString;
        this.tokenRepository = tokenRepository;
        this.tokenScopeRepository = tokenScopeRepository;
        this.authCodeTokenRepository = authCodeTokenRepository;
        this.clientTokenRepository = clientTokenRepository;
        this.resourceOwnerTokenRepository = resourceOwnerTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public AuthCode loadAuthCode(String plainTextAuthCode) throws DuplicateRecordException, URISyntaxException {
        AuthCode authCode = loadConfClientOpendIdTokenReady.run(true, false, plainTextAuthCode);
        return authCode;
    }

    public RefreshToken loadRefreshTokenForResourceOwner(String refreshAccessToken, OffsetDateTime tokenExpiresAt, UUID authCodeId, UUID clientId, UUID resourceOwnerId, List<Scope> scopesForToken) throws DuplicateRecordException {

        String accessToken = randomString.run();
        Token token = FixtureFactory.makeOpenIdToken(accessToken, clientId, new ArrayList<>());
        token.setExpiresAt(tokenExpiresAt);
        token.setGrantType(GrantType.AUTHORIZATION_CODE);
        tokenRepository.insert(token);

        for(Scope scope: scopesForToken) {
            TokenScope ts = new TokenScope();
            ts.setId(UUID.randomUUID());
            ts.setScope(scope);
            ts.setTokenId(token.getId());
            tokenScopeRepository.insert(ts);
        }

        AuthCodeToken authCodeToken = new AuthCodeToken();
        authCodeToken.setId(UUID.randomUUID());
        authCodeToken.setTokenId(token.getId());
        authCodeToken.setAuthCodeId(authCodeId);
        authCodeTokenRepository.insert(authCodeToken);

        TokenAudience clientToken = new TokenAudience();
        clientToken.setId(UUID.randomUUID());
        clientToken.setClientId(clientId);
        clientToken.setTokenId(token.getId());
        clientTokenRepository.insert(clientToken);

        // now onto resource owner token
        ResourceOwner resourceOwner = new ResourceOwner();
        resourceOwner.setId(resourceOwnerId);

        ResourceOwnerToken rot = new ResourceOwnerToken();
        rot.setId(UUID.randomUUID());
        rot.setToken(token);
        rot.setResourceOwner(resourceOwner);
        resourceOwnerTokenRepository.insert(rot);

        RefreshToken refreshToken = FixtureFactory.makeRefreshToken(refreshAccessToken, token);
        refreshTokenRepository.insert(refreshToken);
        return refreshToken;
    }

    public RefreshToken loadRefreshTokenForClient(String refreshAccessToken, OffsetDateTime tokenExpiresAt, UUID authCodeId, UUID clientId, List<Scope> scopesForToken) throws DuplicateRecordException {
        String accessToken = randomString.run();
        Token token = FixtureFactory.makeOpenIdToken(accessToken, clientId, new ArrayList<>());
        token.setToken(accessToken);
        token.setExpiresAt(tokenExpiresAt);

        // TODO: need to change this once client_credentials is done.
        token.setGrantType(GrantType.AUTHORIZATION_CODE);
        tokenRepository.insert(token);

        for(Scope scope: scopesForToken) {
            TokenScope ts = new TokenScope();
            ts.setId(UUID.randomUUID());
            ts.setScope(scope);
            ts.setTokenId(token.getId());
            tokenScopeRepository.insert(ts);
        }

        AuthCodeToken authCodeToken = new AuthCodeToken();
        authCodeToken.setId(UUID.randomUUID());
        authCodeToken.setTokenId(token.getId());
        authCodeToken.setAuthCodeId(authCodeId);
        authCodeTokenRepository.insert(authCodeToken);

        TokenAudience clientToken = new TokenAudience();
        clientToken.setId(UUID.randomUUID());
        clientToken.setClientId(clientId);
        clientToken.setTokenId(token.getId());
        clientTokenRepository.insert(clientToken);

        RefreshToken refreshToken = FixtureFactory.makeRefreshToken(refreshAccessToken, token);
        refreshTokenRepository.insert(refreshToken);
        return refreshToken;
    }
}
