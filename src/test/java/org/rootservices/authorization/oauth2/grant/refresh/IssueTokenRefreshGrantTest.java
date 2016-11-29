package org.rootservices.authorization.oauth2.grant.refresh;

import helper.fixture.FixtureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rootservices.authorization.oauth2.grant.refresh.exception.CompromisedRefreshTokenException;
import org.rootservices.authorization.oauth2.grant.token.MakeBearerToken;
import org.rootservices.authorization.oauth2.grant.token.MakeRefreshToken;
import org.rootservices.authorization.oauth2.grant.token.builder.TokenResponseBuilder;
import org.rootservices.authorization.oauth2.grant.token.entity.Extension;
import org.rootservices.authorization.oauth2.grant.token.entity.TokenGraph;
import org.rootservices.authorization.oauth2.grant.token.entity.TokenResponse;
import org.rootservices.authorization.oauth2.grant.token.entity.TokenType;
import org.rootservices.authorization.persistence.entity.*;
import org.rootservices.authorization.persistence.exceptions.DuplicateRecordException;
import org.rootservices.authorization.persistence.repository.*;
import org.rootservices.authorization.security.RandomString;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by tommackenzie on 10/8/16.
 */
public class IssueTokenRefreshGrantTest {
    private IssueTokenRefreshGrant subject;

    @Mock
    private InsertTokenGraphRefreshGrant mockInsertTokenGraphRefreshGrant;
    @Mock
    private TokenChainRepository mockTokenChainRepository;
    @Mock
    private ResourceOwnerTokenRepository mockResourceOwnerTokenRepository;
    @Mock
    private ClientTokenRepository mockClientTokenRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        subject = new IssueTokenRefreshGrant(
                mockInsertTokenGraphRefreshGrant,
                mockTokenChainRepository,
                mockResourceOwnerTokenRepository,
                mockClientTokenRepository,
                new TokenResponseBuilder(),
                "https://sso.rootservices.org"
        );
    }

    @Test
    public void runShouldBeOk() throws Exception {
        UUID clientId = UUID.randomUUID();
        ResourceOwner resourceOwner = FixtureFactory.makeResourceOwner();
        UUID previousTokenId = UUID.randomUUID();
        UUID refreshTokenId = UUID.randomUUID();

        List<Scope> scopes = FixtureFactory.makeOpenIdScopes();

        String headAccessToken = "head-access-token";
        Token headToken = FixtureFactory.makeOpenIdToken(headAccessToken, clientId);
        headToken.setCreatedAt(OffsetDateTime.now().minusDays(1));

        TokenGraph tokenGraph = FixtureFactory.makeTokenGraph(clientId);
        when(mockInsertTokenGraphRefreshGrant.insertTokenGraph(scopes, headToken)).thenReturn(tokenGraph);

        ArgumentCaptor<TokenChain> tokenChainCaptor = ArgumentCaptor.forClass(TokenChain.class);
        ArgumentCaptor<ResourceOwnerToken> resourceOwnerTokenCaptor = ArgumentCaptor.forClass(ResourceOwnerToken.class);
        ArgumentCaptor<ClientToken> clientTokenArgumentCaptor = ArgumentCaptor.forClass(ClientToken.class);

        TokenResponse actual = subject.run(clientId, resourceOwner.getId(), previousTokenId, refreshTokenId, headToken, scopes);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getAccessToken(), is(tokenGraph.getPlainTextAccessToken()));
        assertThat(actual.getRefreshAccessToken(), is(tokenGraph.getPlainTextRefreshToken().get()));
        assertThat(actual.getExpiresIn(), is(3600L));
        assertThat(actual.getTokenType(), is(TokenType.BEARER));
        assertThat(actual.getExtension(), is(Extension.IDENTITY));

        assertThat(actual.getTokenClaims(), is(notNullValue()));
        assertThat(actual.getTokenClaims().getIssuer(), is(notNullValue()));
        assertThat(actual.getTokenClaims().getIssuer(), is("https://sso.rootservices.org"));
        assertThat(actual.getTokenClaims().getAudience(), is(notNullValue()));
        assertThat(actual.getTokenClaims().getAudience().size(), is(1));
        assertThat(actual.getTokenClaims().getAudience().get(0), is(clientId.toString()));
        assertThat(actual.getTokenClaims().getIssuedAt(), is(notNullValue()));
        assertThat(actual.getTokenClaims().getExpirationTime(), is(notNullValue()));
        assertThat(actual.getTokenClaims().getAuthTime(), is(headToken.getCreatedAt().toEpochSecond()));

        verify(mockTokenChainRepository, times(1)).insert(tokenChainCaptor.capture());
        TokenChain tokenChain = tokenChainCaptor.getValue();
        assertThat(tokenChain, is(notNullValue()));
        assertThat(tokenChain.getId(), is(notNullValue()));
        assertThat(tokenChain.getToken().getId(), is(tokenGraph.getToken().getId()));
        assertThat(tokenChain.getPreviousToken().getId(), is(previousTokenId));
        assertThat(tokenChain.getRefreshToken().getId(), is(refreshTokenId));

        verify(mockResourceOwnerTokenRepository, times(1)).insert(resourceOwnerTokenCaptor.capture());
        ResourceOwnerToken actualRot = resourceOwnerTokenCaptor.getValue();
        assertThat(actualRot.getId(), is(notNullValue()));
        assertThat(actualRot.getToken(), is(tokenGraph.getToken()));
        assertThat(actualRot.getResourceOwner().getId(), is(resourceOwner.getId()));

        verify(mockClientTokenRepository, times(1)).insert(clientTokenArgumentCaptor.capture());
        ClientToken actualCt = clientTokenArgumentCaptor.getValue();
        assertThat(actualCt.getId(), is(notNullValue()));
        assertThat(actualCt.getTokenId(), is(tokenGraph.getToken().getId()));
        assertThat(actualCt.getClientId(), is(clientId));
    }

    @Test
    public void runWhenRefreshTokenUsedShouldThrowCompromisedRefreshTokenException() throws Exception {
        UUID clientId = UUID.randomUUID();
        ResourceOwner resourceOwner = FixtureFactory.makeResourceOwner();
        UUID previousTokenId = UUID.randomUUID();
        UUID refreshTokenId = UUID.randomUUID();

        List<Scope> scopes = FixtureFactory.makeOpenIdScopes();

        String headAccessToken = "head-access-token";
        Token headToken = FixtureFactory.makeOpenIdToken(headAccessToken, clientId);

        TokenGraph tokenGraph = FixtureFactory.makeTokenGraph(clientId);
        when(mockInsertTokenGraphRefreshGrant.insertTokenGraph(scopes, headToken)).thenReturn(tokenGraph);

        DuplicateRecordException dre = new DuplicateRecordException("", null);
        doThrow(dre).when(mockTokenChainRepository).insert(any(TokenChain.class));

        CompromisedRefreshTokenException actual = null;
        try {
            subject.run(clientId, resourceOwner.getId(), previousTokenId, refreshTokenId, headToken, scopes);
        } catch (CompromisedRefreshTokenException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getCause(), is(dre));
        assertThat(actual.getMessage(), is("refresh token was already used"));
    }

}