package org.rootservices.authorization.openId.grant.redirect.implicit.authorization.response;

import helper.fixture.FixtureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rootservices.authorization.authenticate.LoginResourceOwner;
import org.rootservices.authorization.constant.ErrorCode;
import org.rootservices.authorization.oauth2.grant.redirect.implicit.authorization.response.IssueTokenImplicitGrant;
import org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.exception.InformClientException;
import org.rootservices.authorization.openId.grant.redirect.implicit.authorization.request.ValidateOpenIdIdImplicitGrant;
import org.rootservices.authorization.openId.grant.redirect.implicit.authorization.request.entity.OpenIdImplicitAuthRequest;
import org.rootservices.authorization.openId.grant.redirect.implicit.authorization.response.entity.OpenIdImplicitAccessToken;
import org.rootservices.authorization.openId.identity.MakeImplicitIdentityToken;
import org.rootservices.authorization.openId.identity.exception.IdTokenException;
import org.rootservices.authorization.openId.identity.exception.KeyNotFoundException;
import org.rootservices.authorization.openId.identity.exception.ProfileNotFoundException;
import org.rootservices.authorization.persistence.entity.ResourceOwner;
import org.rootservices.authorization.persistence.entity.Token;
import org.rootservices.authorization.security.RandomString;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Created by tommackenzie on 9/6/16.
 */
public class RequestOpenIdImplicitTokenAndIdentityTest {

    private RequestOpenIdImplicitTokenAndIdentity subject;

    @Mock
    private ValidateOpenIdIdImplicitGrant mockValidateOpenIdIdImplicitGrant;
    @Mock
    private LoginResourceOwner mockLoginResourceOwner;
    @Mock
    private RandomString mockRandomString;
    @Mock
    private IssueTokenImplicitGrant mockIssueTokenImplicitGrant;
    @Mock
    private MakeImplicitIdentityToken mockMakeImplicitIdentityToken;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new RequestOpenIdImplicitTokenAndIdentity(
                mockValidateOpenIdIdImplicitGrant,
                mockLoginResourceOwner,
                mockRandomString,
                mockIssueTokenImplicitGrant,
                mockMakeImplicitIdentityToken
        );
    }

    @Test
    public void requestShouldReturnToken() throws Exception {
        String userName = "userName";
        String password = "password";

        List<String> clientIds = new ArrayList<>();
        List<String> responseTypes = new ArrayList<>();
        List<String> redirectUris = new ArrayList<>();
        List<String> scopes = new ArrayList<>();
        List<String> states = new ArrayList<>();
        List<String> nonces = new ArrayList<>();

        OpenIdImplicitAuthRequest request = new OpenIdImplicitAuthRequest();
        request.setRedirectURI(new URI(FixtureFactory.SECURE_REDIRECT_URI));
        request.setNonce("nonce");
        request.setState(Optional.of("state"));

        ResourceOwner resourceOwner = FixtureFactory.makeResourceOwner();
        String accessToken = "access-token";
        Token token = FixtureFactory.makeToken();
        token.setSecondsToExpiration(3600L);
        String idToken = "encoded-jwt";

        when(mockValidateOpenIdIdImplicitGrant.run(
                clientIds, responseTypes, redirectUris, scopes, states, nonces
        )).thenReturn(request);
        when(mockLoginResourceOwner.run(userName, password)).thenReturn(resourceOwner);
        when(mockRandomString.run()).thenReturn(accessToken);
        when(mockIssueTokenImplicitGrant.run(resourceOwner, scopes, accessToken)).thenReturn(token);
        when(mockMakeImplicitIdentityToken.make(
                accessToken, request.getNonce(), resourceOwner.getUuid(), token.getTokenScopes())
        ).thenReturn(idToken);

        OpenIdImplicitAccessToken actual = subject.request(
                userName, password, clientIds, responseTypes, redirectUris, scopes, states, nonces
        );

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getAccessToken(), is(accessToken));
        assertThat(actual.getExpiresIn(), is(token.getSecondsToExpiration()));
        assertThat(actual.getIdToken(), is(idToken));
        assertThat(actual.getRedirectUri(), is(request.getRedirectURI()));
        assertThat(actual.getNonce(), is(request.getNonce()));
        assertThat(actual.getState(), is(request.getState()));
    }

    @Test
    public void requestWhenProfileNotFoundShouldThrowInformClientException() throws Exception {
        String userName = "userName";
        String password = "password";

        List<String> clientIds = new ArrayList<>();
        List<String> responseTypes = new ArrayList<>();
        List<String> redirectUris = new ArrayList<>();
        List<String> scopes = new ArrayList<>();
        List<String> states = new ArrayList<>();
        List<String> nonces = new ArrayList<>();

        OpenIdImplicitAuthRequest request = new OpenIdImplicitAuthRequest();
        request.setRedirectURI(new URI(FixtureFactory.SECURE_REDIRECT_URI));
        request.setNonce("nonce");
        request.setState(Optional.of("state"));

        ResourceOwner resourceOwner = FixtureFactory.makeResourceOwner();
        String accessToken = "access-token";
        Token token = FixtureFactory.makeToken();
        token.setSecondsToExpiration(3600L);

        ProfileNotFoundException pnfe = new ProfileNotFoundException("", null);

        when(mockValidateOpenIdIdImplicitGrant.run(
                clientIds, responseTypes, redirectUris, scopes, states, nonces
        )).thenReturn(request);
        when(mockLoginResourceOwner.run(userName, password)).thenReturn(resourceOwner);
        when(mockRandomString.run()).thenReturn(accessToken);
        when(mockIssueTokenImplicitGrant.run(resourceOwner, scopes, accessToken)).thenReturn(token);
        when(mockMakeImplicitIdentityToken.make(
                accessToken, request.getNonce(), resourceOwner.getUuid(), token.getTokenScopes())
        ).thenThrow(pnfe);

        InformClientException expected = null;
        try {
            subject.request(
                    userName, password, clientIds, responseTypes, redirectUris, scopes, states, nonces
            );
        } catch (InformClientException actual) {
            expected = actual;
        }

        assertThat(expected, is(notNullValue()));
        assertThat(expected.getError(), is("server_error"));
        assertThat(expected.getDescription(), is(ErrorCode.PROFILE_NOT_FOUND.getDescription()));
        assertThat(expected.getCode(), is(ErrorCode.PROFILE_NOT_FOUND.getCode()));
        assertThat(expected.getRedirectURI(), is(request.getRedirectURI()));
        assertThat(expected.getState(), is(request.getState()));
        assertThat(expected.getDomainCause(), instanceOf(ProfileNotFoundException.class));
    }

    @Test
    public void requestWhenKeyNotFoundShouldThrowInformClientException() throws Exception {
        String userName = "userName";
        String password = "password";

        List<String> clientIds = new ArrayList<>();
        List<String> responseTypes = new ArrayList<>();
        List<String> redirectUris = new ArrayList<>();
        List<String> scopes = new ArrayList<>();
        List<String> states = new ArrayList<>();
        List<String> nonces = new ArrayList<>();

        OpenIdImplicitAuthRequest request = new OpenIdImplicitAuthRequest();
        request.setRedirectURI(new URI(FixtureFactory.SECURE_REDIRECT_URI));
        request.setNonce("nonce");
        request.setState(Optional.of("state"));

        ResourceOwner resourceOwner = FixtureFactory.makeResourceOwner();
        String accessToken = "access-token";
        Token token = FixtureFactory.makeToken();
        token.setSecondsToExpiration(3600L);

        KeyNotFoundException knfe = new KeyNotFoundException("", null);

        when(mockValidateOpenIdIdImplicitGrant.run(
                clientIds, responseTypes, redirectUris, scopes, states, nonces
        )).thenReturn(request);
        when(mockLoginResourceOwner.run(userName, password)).thenReturn(resourceOwner);
        when(mockRandomString.run()).thenReturn(accessToken);
        when(mockIssueTokenImplicitGrant.run(resourceOwner, scopes, accessToken)).thenReturn(token);
        when(mockMakeImplicitIdentityToken.make(
                accessToken, request.getNonce(), resourceOwner.getUuid(), token.getTokenScopes())
        ).thenThrow(knfe);

        InformClientException expected = null;
        try {
            subject.request(
                    userName, password, clientIds, responseTypes, redirectUris, scopes, states, nonces
            );
        } catch (InformClientException actual) {
            expected = actual;
        }

        assertThat(expected, is(notNullValue()));
        assertThat(expected.getError(), is("server_error"));
        assertThat(expected.getDescription(), is(ErrorCode.SIGN_KEY_NOT_FOUND.getDescription()));
        assertThat(expected.getCode(), is(ErrorCode.SIGN_KEY_NOT_FOUND.getCode()));
        assertThat(expected.getRedirectURI(), is(request.getRedirectURI()));
        assertThat(expected.getState(), is(request.getState()));
        assertThat(expected.getDomainCause(), instanceOf(KeyNotFoundException.class));
    }

    @Test
    public void requestWhenJwtEncodingErrorShouldThrowInformClientException() throws Exception {
        String userName = "userName";
        String password = "password";

        List<String> clientIds = new ArrayList<>();
        List<String> responseTypes = new ArrayList<>();
        List<String> redirectUris = new ArrayList<>();
        List<String> scopes = new ArrayList<>();
        List<String> states = new ArrayList<>();
        List<String> nonces = new ArrayList<>();

        OpenIdImplicitAuthRequest request = new OpenIdImplicitAuthRequest();
        request.setRedirectURI(new URI(FixtureFactory.SECURE_REDIRECT_URI));
        request.setNonce("nonce");
        request.setState(Optional.of("state"));

        ResourceOwner resourceOwner = FixtureFactory.makeResourceOwner();
        String accessToken = "access-token";
        Token token = FixtureFactory.makeToken();
        token.setSecondsToExpiration(3600L);

        IdTokenException ide = new IdTokenException("", null);

        when(mockValidateOpenIdIdImplicitGrant.run(
                clientIds, responseTypes, redirectUris, scopes, states, nonces
        )).thenReturn(request);
        when(mockLoginResourceOwner.run(userName, password)).thenReturn(resourceOwner);
        when(mockRandomString.run()).thenReturn(accessToken);
        when(mockIssueTokenImplicitGrant.run(resourceOwner, scopes, accessToken)).thenReturn(token);
        when(mockMakeImplicitIdentityToken.make(
                accessToken, request.getNonce(), resourceOwner.getUuid(), token.getTokenScopes())
        ).thenThrow(ide);


        InformClientException expected = null;
        try {
            subject.request(
                    userName, password, clientIds, responseTypes, redirectUris, scopes, states, nonces
            );
        } catch (InformClientException actual) {
            expected = actual;
        }

        assertThat(expected, is(notNullValue()));
        assertThat(expected.getError(), is("server_error"));
        assertThat(expected.getDescription(), is(ErrorCode.JWT_ENCODING_ERROR.getDescription()));
        assertThat(expected.getCode(), is(ErrorCode.JWT_ENCODING_ERROR.getCode()));
        assertThat(expected.getRedirectURI(), is(request.getRedirectURI()));
        assertThat(expected.getState(), is(request.getState()));
        assertThat(expected.getDomainCause(), instanceOf(IdTokenException.class));
    }
}