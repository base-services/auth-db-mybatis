package net.tokensmith.authorization.openId.grant.redirect.implicit.authorization.response.builder;

import net.tokensmith.authorization.oauth2.grant.token.entity.TokenType;
import net.tokensmith.authorization.openId.grant.redirect.implicit.authorization.response.entity.OpenIdImplicitAccessToken;

import java.net.URI;
import java.util.Optional;

/**
 * Created by tommackenzie on 10/21/16.
 */
public class OpenIdImplicitAccessTokenBuilder {
    private URI redirectUri;
    private String accessToken;
    private TokenType tokenType;
    private String idToken;
    private Long expiresIn;
    private Optional<String> scope;
    private Optional<String> state;
    // used to let the user update their profile via local token
    private String sessionToken;
    private Long sessionTokenIssuedAt;



    public OpenIdImplicitAccessTokenBuilder setRedirectUri(URI redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setIdToken(String idToken) {
        this.idToken = idToken;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setScope(Optional<String> scope) {
        this.scope = scope;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setState(Optional<String> state) {
        this.state = state;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
        return this;
    }

    public OpenIdImplicitAccessTokenBuilder setSessionTokenIssuedAt(Long sessionTokenIssuedAt) {
        this.sessionTokenIssuedAt = sessionTokenIssuedAt;
        return this;
    }

    public OpenIdImplicitAccessToken build() {
        OpenIdImplicitAccessToken token = new OpenIdImplicitAccessToken();
        token.setRedirectUri(this.redirectUri);
        token.setAccessToken(this.accessToken);
        token.setTokenType(this.tokenType);
        token.setIdToken(this.idToken);
        token.setExpiresIn(this.expiresIn);
        token.setScope(this.scope);
        token.setState(this.state);
        token.setSessionToken(this.sessionToken);
        token.setSessionTokenIssuedAt(this.sessionTokenIssuedAt);
        return token;
    }
}
