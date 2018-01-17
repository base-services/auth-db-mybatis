package org.rootservices.authorization.oauth2.grant.password;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.rootservices.authorization.authenticate.LoginConfidentialClient;
import org.rootservices.authorization.authenticate.LoginResourceOwner;
import org.rootservices.authorization.authenticate.exception.UnauthorizedException;
import org.rootservices.authorization.constant.ErrorCode;
import org.rootservices.authorization.exception.ServerException;
import org.rootservices.authorization.oauth2.grant.token.RequestTokenGrant;
import org.rootservices.authorization.oauth2.grant.password.entity.TokenInputPasswordGrant;
import org.rootservices.authorization.oauth2.grant.password.factory.TokenInputPasswordGrantFactory;
import org.rootservices.authorization.exception.BadRequestException;
import org.rootservices.authorization.oauth2.grant.token.exception.BadRequestExceptionBuilder;
import org.rootservices.authorization.oauth2.grant.token.exception.UnknownKeyException;
import org.rootservices.authorization.oauth2.grant.token.entity.TokenResponse;
import org.rootservices.authorization.oauth2.grant.token.exception.InvalidValueException;
import org.rootservices.authorization.oauth2.grant.token.exception.MissingKeyException;
import org.rootservices.authorization.persistence.entity.*;
import org.rootservices.authorization.security.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class RequestTokenPasswordGrant implements RequestTokenGrant {
    private LoginConfidentialClient loginConfidentialClient;
    private TokenInputPasswordGrantFactory tokenInputPasswordGrantFactory;
    private LoginResourceOwner loginResourceOwner;
    private RandomString randomString;
    private IssueTokenPasswordGrant issueTokenPasswordGrant;

    @Autowired
    public RequestTokenPasswordGrant(LoginConfidentialClient loginConfidentialClient, TokenInputPasswordGrantFactory tokenInputPasswordGrantFactory, LoginResourceOwner loginResourceOwner, RandomString randomString, IssueTokenPasswordGrant issueTokenPasswordGrant) {
        this.loginConfidentialClient = loginConfidentialClient;
        this.tokenInputPasswordGrantFactory = tokenInputPasswordGrantFactory;
        this.loginResourceOwner = loginResourceOwner;
        this.randomString = randomString;
        this.issueTokenPasswordGrant = issueTokenPasswordGrant;
    }

    @Override
    public TokenResponse request(UUID clientId, String clientPassword, Map<String, String> request) throws BadRequestException, UnauthorizedException, ServerException {

        ConfidentialClient cc = loginConfidentialClient.run(clientId, clientPassword);

        TokenInputPasswordGrant input;
        try {
            input = tokenInputPasswordGrantFactory.run(request);
        } catch (UnknownKeyException e) {
            throw new BadRequestExceptionBuilder().UnknownKey(e.getKey(), e.getCode(), e).build();
        } catch (InvalidValueException e) {
            throw new BadRequestExceptionBuilder().InvalidKeyValue(e.getKey(), e.getCode(), e).build();
        } catch (MissingKeyException e) {
            throw new BadRequestExceptionBuilder().MissingKey(e.getKey(), e).build();
        }

        ResourceOwner resourceOwner = loginResourceOwner.run(input.getUserName(), input.getPassword());

        List<Scope> scopes = matchScopes(cc.getClient().getScopes(), input.getScopes());

        List<Client> audience = new ArrayList<>();
        audience.add(cc.getClient());

        TokenResponse tokenResponse;
        try {
            tokenResponse = issueTokenPasswordGrant.run(cc.getClient().getId(), resourceOwner.getId(), scopes, audience);
        } catch (ServerException e) {
            throw e;
        }
        return tokenResponse;
    }

    /**
     * Checks if each item in input scopes is in the scopes list. If a item in the input scopes is not
     * in the scopes list, then a BadRequestException is thrown.
     *
     * @param scopes
     * @param inputScope
     * @return a list of the matches
     * @throws BadRequestException
     */
    protected List<Scope> matchScopes(List<Scope> scopes, List<String> inputScope) throws BadRequestException {

        List<Scope> matches = new ArrayList<>();
        for(String scope: inputScope) {
            Optional<Scope> match = scopes.stream()
                    .filter(o -> o.getName().equals(scope))
                    .findFirst();

            if (!match.isPresent()) {
                throw new BadRequestExceptionBuilder().InvalidScope(ErrorCode.SCOPES_NOT_SUPPORTED.getCode()).build();
            }
            matches.add(match.get());
        }
        return matches;
    }
}
