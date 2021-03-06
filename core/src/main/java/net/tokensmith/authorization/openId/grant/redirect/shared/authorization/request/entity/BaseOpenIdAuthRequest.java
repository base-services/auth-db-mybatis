package net.tokensmith.authorization.openId.grant.redirect.shared.authorization.request.entity;


import net.tokensmith.parser.Parameter;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class BaseOpenIdAuthRequest {
    @Parameter(name = "client_id")
    protected UUID clientId;

    @Parameter(name = "redirect_uri")
    protected URI redirectURI;

    @Parameter(name = "state", required = false)
    protected Optional<String> state = Optional.empty();

    @Parameter(name = "response_type", expected = {"CODE", "TOKEN", "ID_TOKEN"}, parsable = true)
    protected List<String> responseTypes;

    @Parameter(name = "scope", required = false, parsable = true)
    protected List<String> scopes;

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public URI getRedirectURI() {
        return redirectURI;
    }

    public void setRedirectURI(URI redirectURI) {
        this.redirectURI = redirectURI;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public Optional<String> getState() {
        return state;
    }

    public void setState(Optional<String> state) {
        this.state = state;
    }
}
