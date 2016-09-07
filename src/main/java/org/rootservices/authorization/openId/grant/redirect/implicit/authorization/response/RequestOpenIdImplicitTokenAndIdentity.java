package org.rootservices.authorization.openId.grant.redirect.implicit.authorization.response;

import org.rootservices.authorization.authenticate.LoginResourceOwner;
import org.rootservices.authorization.authenticate.exception.UnauthorizedException;
import org.rootservices.authorization.constant.ErrorCode;
import org.rootservices.authorization.oauth2.grant.redirect.implicit.authorization.response.IssueTokenImplicitGrant;
import org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.exception.InformClientException;
import org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.exception.InformResourceOwnerException;
import org.rootservices.authorization.openId.grant.redirect.implicit.authorization.request.ValidateOpenIdIdImplicitGrant;
import org.rootservices.authorization.openId.grant.redirect.implicit.authorization.request.entity.OpenIdImplicitAuthRequest;
import org.rootservices.authorization.openId.grant.redirect.implicit.authorization.response.entity.OpenIdImplicitAccessToken;
import org.rootservices.authorization.openId.identity.MakeImplicitIdentityToken;
import org.rootservices.authorization.openId.identity.exception.IdTokenException;
import org.rootservices.authorization.openId.identity.exception.KeyNotFoundException;
import org.rootservices.authorization.openId.identity.exception.ProfileNotFoundException;
import org.rootservices.authorization.persistence.entity.*;
import org.rootservices.authorization.persistence.repository.ScopeRepository;
import org.rootservices.authorization.security.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Created by tommackenzie on 8/30/16.
 */
@Component
public class RequestOpenIdImplicitTokenAndIdentity {
    private static String MESSAGE = "Failed to create id_token";
    private static String SERVER_ERROR = "server_error";

    private ValidateOpenIdIdImplicitGrant validateOpenIdIdImplicitGrant;
    private LoginResourceOwner loginResourceOwner;
    private RandomString randomString;
    private IssueTokenImplicitGrant issueTokenImplicitGrant;
    private MakeImplicitIdentityToken makeImplicitIdentityToken;

    @Autowired
    public RequestOpenIdImplicitTokenAndIdentity(ValidateOpenIdIdImplicitGrant validateOpenIdIdImplicitGrant, LoginResourceOwner loginResourceOwner, RandomString randomString, IssueTokenImplicitGrant issueTokenImplicitGrant, MakeImplicitIdentityToken makeImplicitIdentityToken) {
        this.validateOpenIdIdImplicitGrant = validateOpenIdIdImplicitGrant;
        this.loginResourceOwner = loginResourceOwner;
        this.randomString = randomString;
        this.issueTokenImplicitGrant = issueTokenImplicitGrant;
        this.makeImplicitIdentityToken = makeImplicitIdentityToken;
    }

    public OpenIdImplicitAccessToken request(String userName, String password, List<String> clientIds, List<String> responseTypes, List<String> redirectUris, List<String> scopes, List<String> states, List<String> nonces) throws InformResourceOwnerException, InformClientException, UnauthorizedException {
        OpenIdImplicitAuthRequest request = validateOpenIdIdImplicitGrant.run(
                clientIds, responseTypes, redirectUris, scopes, states, nonces
        );

        ResourceOwner resourceOwner = loginResourceOwner.run(userName, password);

        String accessToken = randomString.run();
        Token token = issueTokenImplicitGrant.run(resourceOwner, scopes,  accessToken);

        String idToken = null;
        try {
            idToken = makeImplicitIdentityToken.make(
                    accessToken,
                    request.getNonce(),
                    resourceOwner.getUuid(),
                    token.getTokenScopes()
            );
        } catch (ProfileNotFoundException e) {
            ErrorCode ec = ErrorCode.PROFILE_NOT_FOUND;
            throw buildInformClientException(ec, request.getRedirectURI(), request.getState(), e);
        } catch (KeyNotFoundException e) {
            ErrorCode ec = ErrorCode.SIGN_KEY_NOT_FOUND;
            throw buildInformClientException(ec, request.getRedirectURI(), request.getState(), e);
        } catch (IdTokenException e) {
            ErrorCode ec = ErrorCode.JWT_ENCODING_ERROR;
            throw buildInformClientException(ec, request.getRedirectURI(), request.getState(), e);
        }

        OpenIdImplicitAccessToken response = new OpenIdImplicitAccessToken();
        response.setAccessToken(accessToken);
        response.setExpiresIn(token.getSecondsToExpiration());
        response.setIdToken(idToken);
        response.setRedirectUri(request.getRedirectURI());
        response.setNonce(request.getNonce());
        response.setState(request.getState());

        return response;
    }

    protected InformClientException buildInformClientException(ErrorCode ec, URI redirectURI, Optional<String> state, Throwable cause) {
        return new InformClientException(
            MESSAGE, SERVER_ERROR, ec.getDescription(), ec.getCode(), redirectURI, state, cause
        );
    }
}
