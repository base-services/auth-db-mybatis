package integration.authorization.oauth2.grant.code.request.ValidateParams.validation.State;

import helper.ValidateParamsAttributes;
import integration.authorization.oauth2.grant.code.request.ValidateParams.BaseTest;
import org.junit.Test;
import org.rootservices.authorization.constant.ErrorCode;
import org.rootservices.authorization.oauth2.grant.redirect.authorization.request.buider.exception.StateException;
import org.rootservices.authorization.persistence.entity.Client;
import org.rootservices.authorization.persistence.entity.ResponseType;

import java.net.URISyntaxException;


public class ClientFoundTest extends BaseTest {

    public ValidateParamsAttributes makeValidateParamsAttributes() {
        ValidateParamsAttributes p = new ValidateParamsAttributes();
        p.responseTypes.add(ResponseType.CODE.toString());

        return p;
    }

    @Test
    public void stateHasTwoItemsShouldThrowInformClientException() throws URISyntaxException {
        Client c = loadConfidentialClient();

        ValidateParamsAttributes p = makeValidateParamsAttributes();
        p.clientIds.add(c.getUuid().toString());
        p.states.add("some-state");
        p.states.add("some-state");

        Exception expectedDomainCause = new StateException();
        int expectedErrorCode = ErrorCode.STATE_MORE_THAN_ONE_ITEM.getCode();
        String expectedDescription = "state has more than one value";
        String expectedError = "invalid_request";

        runExpectInformClientException(p, expectedDomainCause, expectedErrorCode, expectedError, expectedDescription, c.getRedirectURI());
    }

    @Test
    public void stateIsBlankStringShouldThrowInformClientException() throws URISyntaxException {
        Client c = loadConfidentialClient();

        ValidateParamsAttributes p = makeValidateParamsAttributes();
        p.clientIds.add(c.getUuid().toString());
        p.states.add("");

        Exception expectedDomainCause = new StateException();
        int expectedErrorCode = ErrorCode.STATE_EMPTY_VALUE.getCode();
        String expectedDescription = "state is blank";
        String expectedError = "invalid_request";

        runExpectInformClientException(p, expectedDomainCause, expectedErrorCode, expectedError, expectedDescription, c.getRedirectURI());

    }
}
