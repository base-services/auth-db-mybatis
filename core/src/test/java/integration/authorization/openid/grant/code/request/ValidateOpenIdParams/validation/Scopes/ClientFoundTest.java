package integration.authorization.openid.grant.code.request.ValidateOpenIdParams.validation.Scopes;


import integration.authorization.openid.grant.code.request.ValidateOpenIdParams.BaseTest;
import net.tokensmith.authorization.constant.ErrorCode;
import net.tokensmith.parser.exception.OptionalException;
import net.tokensmith.repository.entity.Client;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ClientFoundTest extends BaseTest {

    public Map<String, List<String>> makeParams(UUID clientId, URI redirectUri) {
        Map<String, List<String>> p = super.makeParams();

        p.get("client_id").add(clientId.toString());
        p.get("redirect_uri").add(redirectUri.toString());
        p.get("state").add("some-state");
        p.get("response_type").add("CODE");

        return p;
    }

    @Test
    public void scopeIsInvalidShouldThrowInformClientException() throws Exception {
        Client c = loadConfidentialClient();

        Map<String, List<String>> p = makeParams(c.getId(), c.getRedirectURI());
        p.get("scope").add("invalid-scope");

        int expectedErrorCode = ErrorCode.SCOPES_NOT_SUPPORTED.getCode();
        String expectedDescription = ErrorCode.SCOPES_NOT_SUPPORTED.getDescription();
        String expectedError = "invalid_scope";

        runExpectInformClientExceptionWithStateNoCause(p, expectedErrorCode, expectedError, expectedDescription, c.getRedirectURI());
    }

    @Test
    public void scopesHasTwoItemsShouldThrowInformClientException() throws Exception {
        Client c = loadConfidentialClient();

        Map<String, List<String>> p = makeParams(c.getId(), c.getRedirectURI());
        p.get("scope").add("profile");
        p.get("scope").add("profile");

        Exception cause = new OptionalException();
        int expectedErrorCode = 1;
        String expectedDescription = ErrorCode.SCOPES_MORE_THAN_ONE_ITEM.getDescription();
        String expectedError = "invalid_request";

        runExpectInformClientExceptionWithState(p, cause, expectedErrorCode, expectedError, expectedDescription, c.getRedirectURI());
    }

    @Test
    public void scopeIsBlankStringShouldThrowInformClientException() throws Exception {
        Client c = loadConfidentialClient();

        Map<String, List<String>> p = makeParams(c.getId(), c.getRedirectURI());
        p.get("scope").add("");

        Exception cause = new OptionalException();
        int expectedErrorCode = 1;
        String expectedDescription = ErrorCode.SCOPES_EMPTY_VALUE.getDescription();
        String expectedError = "invalid_scope";

        runExpectInformClientExceptionWithState(p, cause, expectedErrorCode, expectedError, expectedDescription, c.getRedirectURI());
    }
}
