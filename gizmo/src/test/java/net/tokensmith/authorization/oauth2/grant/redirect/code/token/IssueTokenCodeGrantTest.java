package net.tokensmith.authorization.oauth2.grant.redirect.code.token;

import helper.fixture.FixtureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import net.tokensmith.authorization.constant.ErrorCode;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenGraph;
import net.tokensmith.authorization.oauth2.grant.redirect.code.token.exception.CompromisedCodeException;
import net.tokensmith.authorization.oauth2.grant.token.builder.TokenResponseBuilder;
import net.tokensmith.authorization.oauth2.grant.token.entity.Extension;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenResponse;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenType;
import net.tokensmith.authorization.persistence.entity.*;
import net.tokensmith.authorization.persistence.exceptions.DuplicateRecordException;
import net.tokensmith.authorization.persistence.repository.*;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * Created by tommackenzie on 8/28/16.
 */
public class IssueTokenCodeGrantTest {
    private IssueTokenCodeGrant subject;

    @Mock
    private InsertTokenGraphCodeGrant mockInsertTokenGraph;
    @Mock
    private TokenRepository mockTokenRepository;
    @Mock
    private RefreshTokenRepository mockRefreshTokenRepository;
    @Mock
    private AuthCodeTokenRepository mockAuthCodeTokenRepository;
    @Mock
    private ResourceOwnerTokenRepository mockResourceOwnerTokenRepository;
    @Mock
    private AuthCodeRepository mockAuthCodeRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new IssueTokenCodeGrant(
                mockInsertTokenGraph,
                mockTokenRepository,
                mockRefreshTokenRepository,
                mockAuthCodeTokenRepository,
                mockResourceOwnerTokenRepository,
                mockAuthCodeRepository,
                new TokenResponseBuilder(),
                "https://sso.rootservices.org"
        );
    }

    @Test
    public void runShouldReturnTokenResponse() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID authCodeId = UUID.randomUUID();
        UUID resourceOwnerId = UUID.randomUUID();

        List<Scope> scopes = FixtureFactory.makeScopes();

        List<Client> audience = FixtureFactory.makeAudience(clientId);
        TokenGraph tokenGraph = FixtureFactory.makeTokenGraph(clientId, audience);
        when(mockInsertTokenGraph.insertTokenGraph(clientId, scopes, audience)).thenReturn(tokenGraph);

        TokenResponse actual = subject.run(clientId, authCodeId, resourceOwnerId, scopes,audience);

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
        assertThat(actual.getTokenClaims().getAuthTime(), is(tokenGraph.getToken().getCreatedAt().toEpochSecond()));

        // should insert a authCodeToken
        ArgumentCaptor<AuthCodeToken> authCodeTokenCaptor = ArgumentCaptor.forClass(AuthCodeToken.class);
        verify(mockAuthCodeTokenRepository).insert(authCodeTokenCaptor.capture());

        AuthCodeToken actualACT = authCodeTokenCaptor.getValue();
        assertThat(actualACT.getId(), is(notNullValue()));
        assertThat(actualACT.getTokenId(), is(tokenGraph.getToken().getId()));
        assertThat(actualACT.getAuthCodeId(), is(authCodeId));

        // should insert a resourceOwnerToken
        ArgumentCaptor<ResourceOwnerToken> resourceOwnerTokenCaptor = ArgumentCaptor.forClass(ResourceOwnerToken.class);
        verify(mockResourceOwnerTokenRepository).insert(resourceOwnerTokenCaptor.capture());

        ResourceOwnerToken actualROT = resourceOwnerTokenCaptor.getValue();
        assertThat(actualROT.getResourceOwner(), is(notNullValue()));
        assertThat(actualROT.getResourceOwner().getId(), is(resourceOwnerId));

        assertThat(actualROT.getId(), is(notNullValue()));
        assertThat(actualROT.getToken(), is(tokenGraph.getToken()));
    }

    @Test
    public void runShouldThrowCompromisedCodeException() throws Exception{
        UUID clientId = UUID.randomUUID();
        UUID authCodeId = UUID.randomUUID();
        UUID resourceOwnerId = UUID.randomUUID();
        List<Scope> scopes = FixtureFactory.makeScopes();

        List<Client> audience = FixtureFactory.makeAudience(clientId);
        TokenGraph tokenGraph = FixtureFactory.makeTokenGraph(clientId, audience);
        when(mockInsertTokenGraph.insertTokenGraph(clientId, scopes, audience)).thenReturn(tokenGraph);

        DuplicateRecordException duplicateRecordException = new DuplicateRecordException("", null);
        doThrow(duplicateRecordException).when(mockAuthCodeTokenRepository).insert(any(AuthCodeToken.class));

        CompromisedCodeException expected = null;

        try {
            subject.run(clientId, authCodeId, resourceOwnerId, scopes, audience);
        } catch (CompromisedCodeException e) {
            expected = e;
            assertThat(expected.getError(), is("invalid_grant"));
            assertThat(expected.getCode(), is(ErrorCode.COMPROMISED_AUTH_CODE.getCode()));
            assertThat(expected.getMessage(), is(ErrorCode.COMPROMISED_AUTH_CODE.getDescription()));
        }

        assertThat(expected, is(notNullValue()));

        // should have attempted to insert a authCodeToken
        ArgumentCaptor<AuthCodeToken> authCodeTokenCaptor = ArgumentCaptor.forClass(AuthCodeToken.class);
        verify(mockAuthCodeTokenRepository).insert(authCodeTokenCaptor.capture());

        AuthCodeToken actualACT = authCodeTokenCaptor.getValue();
        assertThat(actualACT.getId(), is(notNullValue()));
        assertThat(actualACT.getTokenId(), is(tokenGraph.getToken().getId()));
        assertThat(actualACT.getAuthCodeId(), is(authCodeId));

        // should have rejected previous tokens.
        verify(mockTokenRepository).revokeByAuthCodeId(authCodeId);
        verify(mockAuthCodeRepository).revokeById(authCodeId);
        verify(mockRefreshTokenRepository).revokeByAuthCodeId(authCodeId);

        // should have rejected tokens just inserted.
        verify(mockTokenRepository).revokeById(tokenGraph.getToken().getId());
        verify(mockRefreshTokenRepository).revokeByTokenId(tokenGraph.getToken().getId());
    }
}