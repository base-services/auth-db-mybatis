package integration.ValiateParams.validation.Scopes;

import helper.FixtureFactory;
import helper.ValidateParamsAttributes;
import integration.ValiateParams.BaseTest;
import org.junit.Test;
import org.rootservices.authorization.grant.code.constant.ErrorCode;
import org.rootservices.authorization.grant.code.factory.exception.ResponseTypeException;
import org.rootservices.authorization.grant.code.factory.exception.ScopesException;
import org.rootservices.authorization.grant.code.factory.exception.StateException;
import org.rootservices.authorization.persistence.entity.Client;
import org.rootservices.authorization.persistence.entity.Scope;

import java.net.URISyntaxException;

/**
 * Scenario: Scopes fails validation And Client is found.
 *
 * Given a client, c, exists in the db
 * And client ids has one item that is assigned to c's UUID
 * And response types has one item that is assigned CODE
 * And scopes is [method]
 * When the params are validated
 * Then raise a InformClientException exception, e
 * And expect e's cause to be [expectedDomainCause]
 * And expects e's error code to be [errorCode]
 * And expects e's redirect uri to be c's redirect uri
 */
public class ClientFoundTest extends BaseTest {

    @Test
    public void invalid() throws URISyntaxException, StateException {
        Client c = FixtureFactory.makeClient();
        clientRepository.insert(c);

        ValidateParamsAttributes p = new ValidateParamsAttributes();
        p.clientIds.add(c.getUuid().toString());
        p.responseTypes.add(c.getResponseType().toString());

        p.scopes.add("invalid-scope");

        Exception expectedDomainCause = new ScopesException();
        int expectedErrorCode = ErrorCode.SCOPES_DATA_TYPE.getCode();
        String expectedError = "invalid_scope";

        runExpectInformClientException(p, expectedDomainCause, expectedErrorCode, expectedError, c.getRedirectURI());
    }

    @Test
    public void duplicate() throws URISyntaxException, StateException {
        Client c = FixtureFactory.makeClient();
        clientRepository.insert(c);

        ValidateParamsAttributes p = new ValidateParamsAttributes();
        p.clientIds.add(c.getUuid().toString());
        p.responseTypes.add(c.getResponseType().toString());

        p.scopes.add(Scope.PROFILE.toString());
        p.scopes.add(Scope.PROFILE.toString());

        Exception expectedDomainCause = new ScopesException();
        int expectedErrorCode = ErrorCode.SCOPES_MORE_THAN_ONE_ITEM.getCode();
        String expectedError = "invalid_request";

        runExpectInformClientException(p, expectedDomainCause, expectedErrorCode, expectedError, c.getRedirectURI());
    }

    @Test
    public void emptyValue() throws URISyntaxException, StateException {
        Client c = FixtureFactory.makeClient();
        clientRepository.insert(c);

        ValidateParamsAttributes p = new ValidateParamsAttributes();
        p.clientIds.add(c.getUuid().toString());
        p.responseTypes.add(c.getResponseType().toString());

        p.scopes.add("");

        Exception expectedDomainCause = new ScopesException();
        int expectedErrorCode = ErrorCode.SCOPES_EMPTY_VALUE.getCode();
        String expectedError = "invalid_scope";

        runExpectInformClientException(p, expectedDomainCause, expectedErrorCode, expectedError, c.getRedirectURI());
    }
}