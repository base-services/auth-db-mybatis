package net.tokensmith.authorization.http.controller.resource.api.publik;

import net.tokensmith.authorization.authenticate.exception.UnauthorizedException;
import net.tokensmith.authorization.exception.BadRequestException;
import net.tokensmith.authorization.exception.ServerException;
import net.tokensmith.authorization.http.controller.security.APIUser;
import net.tokensmith.authorization.http.controller.security.TokenSession;
import net.tokensmith.authorization.http.factory.MakeToken;
import net.tokensmith.authorization.http.factory.MakeTokenFactory;
import net.tokensmith.authorization.http.factory.exception.TokenException;
import net.tokensmith.authorization.http.response.Error;
import net.tokensmith.authorization.http.response.Token;
import net.tokensmith.authorization.oauth2.grant.token.RequestToken;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenResponse;
import net.tokensmith.authorization.oauth2.grant.token.exception.BadRequestExceptionBuilder;
import net.tokensmith.authorization.oauth2.grant.token.exception.NotFoundException;
import net.tokensmith.otter.authentication.HttpBasicEntity;
import net.tokensmith.otter.authentication.ParseHttpBasic;
import net.tokensmith.otter.authentication.exception.HttpBasicException;
import net.tokensmith.otter.controller.Resource;
import net.tokensmith.otter.controller.entity.StatusCode;
import net.tokensmith.otter.controller.entity.request.Request;
import net.tokensmith.otter.controller.entity.response.Response;
import net.tokensmith.otter.controller.header.ContentType;
import net.tokensmith.otter.controller.header.Header;
import net.tokensmith.otter.controller.header.HeaderValue;
import net.tokensmith.otter.translator.JsonTranslator;
import net.tokensmith.otter.translator.exception.ToJsonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@Component
public class TokenResource extends Resource<TokenSession, APIUser> {
    private static final Logger logger = LoggerFactory.getLogger(TokenResource.class);
    public static String URL = "/api/public/v1/token(?!/).*";

    private JsonTranslator<Error> errorTranslator;
    private JsonTranslator<Token> tokenTranslator;
    private RequestToken requestToken;
    private ParseHttpBasic parseHttpBasic;
    private MakeTokenFactory makeTokenFactory;

    private static String UNHANDLED_ERROR_DESC = "Unhandled Server Exception";

    @Autowired
    public TokenResource(JsonTranslator<Error> errorTranslator, JsonTranslator<Token> tokenTranslator, RequestToken requestToken, ParseHttpBasic parseHttpBasic, MakeTokenFactory makeTokenFactory) {
        this.errorTranslator = errorTranslator;
        this.tokenTranslator = tokenTranslator;
        this.requestToken = requestToken;
        this.parseHttpBasic = parseHttpBasic;
        this.makeTokenFactory = makeTokenFactory;
    }

    @Override
    public Response<TokenSession> post(Request<TokenSession, APIUser> request, Response<TokenSession> response) {
        setDefaultHeaders(response);

        String authenticationHeader = request.getHeaders().get("Authorization");
        HttpBasicEntity httpBasic;
        try {
            httpBasic = parseHttpBasic.run(authenticationHeader);
        } catch (HttpBasicException e) {
            logger.debug(e.getMessage(), e);
            preparedUnAuthorizedErrorResponse(response);
            return response;
        }

        // validate form.
        Map<String, String> tokenRequest;
        try {
            tokenRequest = formDataToMap(request.getFormData());
        } catch (BadRequestException e) {
            logger.debug(e.getMessage(), e);
            Error error = new Error(e.getError(), e.getDescription());
            prepareErrorResponse(response, error, StatusCode.BAD_REQUEST);
            return response;
        }

        TokenResponse tokenResponse;
        try {
            tokenResponse = requestToken.request(httpBasic.getUser(), httpBasic.getPassword(), tokenRequest);
        } catch(UnauthorizedException e) {
            logger.debug(e.getMessage(), e);
            preparedUnAuthorizedErrorResponse(response);
            return response;
        } catch (NotFoundException e) {
            logger.debug(e.getMessage(), e);
            Error error = new Error(e.getError(), null);
            prepareErrorResponse(response, error, StatusCode.BAD_REQUEST);
            return response;
        } catch (BadRequestException e) {
            logger.debug(e.getMessage(), e);
            Error error = new Error(e.getError(), e.getDescription());
            prepareErrorResponse(response, error, StatusCode.BAD_REQUEST);
            return response;
        } catch (ServerException e) {
            logger.error(e.getMessage(), e);
            Error error = new Error(UNHANDLED_ERROR_DESC, UNHANDLED_ERROR_DESC);
            prepareErrorResponse(response, error, StatusCode.SERVER_ERROR);
            return response;
        }

        // which token should be created? OAuth or OpenId
        MakeToken makeToken = makeTokenFactory.make(tokenResponse.getExtension());
        Token token;
        try {
            token = makeToken.make(tokenResponse);
        } catch (TokenException e) {
            logger.error(e.getMessage(), e);
            Error error = new Error(UNHANDLED_ERROR_DESC, UNHANDLED_ERROR_DESC);
            prepareErrorResponse(response, error, StatusCode.SERVER_ERROR);
            return response;
        }

        prepareTokenResponse(response, token, StatusCode.OK);
        return response;
    }

    protected Map<String, String> formDataToMap(Map<String, List<String>> formData) throws BadRequestException {
        Map<String, String> tokenRequest = new HashMap<>();
        for(Map.Entry<String, List<String>> formElement: formData.entrySet()) {
            if (formElement.getValue().size() == 1) {
                tokenRequest.put(formElement.getKey(), formElement.getValue().get(0));
            } else {
                throw new BadRequestExceptionBuilder()
                        .DuplicateKey(formElement.getKey())
                        .build();
            }
        }
        return tokenRequest;
    }

    protected void preparedUnAuthorizedErrorResponse(Response<TokenSession> response) {
        Error error = new Error("invalid_client", null);
        response.getHeaders().put("WWW-Authenticate", "Basic");
        Optional<byte[]> payload = Optional.empty();

        try {
            payload = Optional.of(errorTranslator.to(error));
        } catch (ToJsonException e) {
            logger.error(e.getMessage(), e);
        }

        response.setPayload(payload);
        response.setStatusCode(StatusCode.UNAUTHORIZED);
    }

    protected void prepareErrorResponse(Response<TokenSession> response, Error error, StatusCode statusCode) {
        Optional<byte[]> payload = Optional.empty();

        try {
            payload = Optional.of(errorTranslator.to(error));
        } catch (ToJsonException e) {
            logger.error(e.getMessage(), e);
        }

        response.setPayload(payload);
        response.setStatusCode(statusCode);
    }

    protected void prepareTokenResponse(Response<TokenSession> response, Token token, StatusCode statusCode) {
        Optional<byte[]> payload = Optional.empty();

        try {
            payload = Optional.of(tokenTranslator.to(token));
        } catch (ToJsonException e) {
            logger.error(e.getMessage(), e);
        }

        response.setPayload(payload);
        response.setStatusCode(statusCode);
    }

    protected void setDefaultHeaders(Response<TokenSession> response) {
        Map<String, String> headers = new HashMap<>();

        response.getHeaders().put(Header.CONTENT_TYPE.getValue(), ContentType.JSON_UTF_8.getValue());
        headers.put(Header.CACHE_CONTROL.getValue(), HeaderValue.NO_STORE.getValue());
        headers.put(Header.PRAGMA.getValue(), HeaderValue.NO_CACHE.getValue());

        response.getHeaders().putAll(headers);
    }
}
