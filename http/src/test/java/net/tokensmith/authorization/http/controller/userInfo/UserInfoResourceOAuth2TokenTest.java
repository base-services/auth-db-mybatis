package net.tokensmith.authorization.http.controller.userInfo;

import helpers.category.ServletContainerTest;
import helpers.fixture.EntityFactory;
import helpers.fixture.FormFactory;
import helpers.fixture.persistence.FactoryForPersistence;
import helpers.fixture.persistence.client.publik.LoadPublicClientTokenResponseType;
import helpers.fixture.persistence.db.GetOrCreateRSAPrivateKey;
import helpers.fixture.persistence.db.LoadResourceOwner;
import helpers.fixture.persistence.http.GetSessionAndCsrfToken;
import helpers.fixture.persistence.http.Session;
import helpers.suite.IntegrationTestSuite;
import net.tokensmith.authorization.openId.identity.entity.IdToken;
import net.tokensmith.jwt.config.JwtAppFactory;
import net.tokensmith.jwt.entity.jwk.RSAPublicKey;
import net.tokensmith.jwt.entity.jwk.Use;
import net.tokensmith.jwt.entity.jwt.JsonWebToken;
import net.tokensmith.jwt.jws.verifier.VerifySignature;
import net.tokensmith.jwt.serialization.JwtSerde;
import net.tokensmith.otter.QueryStringToMap;
import net.tokensmith.otter.controller.header.ContentType;
import net.tokensmith.otter.controller.header.Header;
import net.tokensmith.repository.entity.Client;
import net.tokensmith.repository.entity.RSAPrivateKey;
import net.tokensmith.repository.entity.ResourceOwner;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Param;
import org.asynchttpclient.Response;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by tommackenzie on 12/29/16.
 */
@Category(ServletContainerTest.class)
public class UserInfoResourceOAuth2TokenTest {
    protected static String baseURI = String.valueOf(IntegrationTestSuite.getServer().getURI());
    protected static String servletURI;
    protected static String authServletURI;

    private static LoadPublicClientTokenResponseType loadPublicClientTokenResponseType;
    private static LoadResourceOwner loadResourceOwner;
    private static GetSessionAndCsrfToken getSessionAndCsrfToken;
    private static GetOrCreateRSAPrivateKey getOrCreateRSAPrivateKey;

    @BeforeClass
    public static void beforeClass() {
        servletURI = baseURI + "api/public/v1/userinfo";
        authServletURI = baseURI + "authorization";

        FactoryForPersistence factoryForPersistence = new FactoryForPersistence(
                IntegrationTestSuite.getContext()
        );

        ApplicationContext ac = IntegrationTestSuite.getContext();
        loadPublicClientTokenResponseType = ac.getBean(LoadPublicClientTokenResponseType.class);
        loadResourceOwner = ac.getBean(LoadResourceOwner.class);
        getSessionAndCsrfToken = factoryForPersistence.makeGetSessionAndCsrfToken();
        getOrCreateRSAPrivateKey = factoryForPersistence.getOrCreateRSAPrivateKey();
    }

    public String makeToken(Client client, ResourceOwner ro, List<String> scopes) throws Exception {

        String authServletUriWithParams = this.authServletURI +
                "?client_id=" + client.getId().toString() +
                "&response_type=TOKEN" +
                "&redirect_uri=" + URLEncoder.encode(client.getRedirectURI().toString(), StandardCharsets.UTF_8.name()) +
                "&nonce=some-nonce" +
                "&scope=";

        for(String scope: scopes) {
            authServletUriWithParams += URLEncoder.encode(scope + " ", StandardCharsets.UTF_8.name());
        }

        Session session = getSessionAndCsrfToken.run(authServletUriWithParams);
        List<Param> postData = FormFactory.makeLoginForm(ro.getEmail(), session.getCsrfToken());

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .preparePost(authServletUriWithParams)
                .setFormParams(postData)
                .setCookies(Arrays.asList(session.getCsrf()))
                .execute();

        Response response = f.get();

        URI location = new URI(response.getHeader("location"));
        QueryStringToMap queryStringToMap = new QueryStringToMap();
        Map<String, List<String>> params = queryStringToMap.run(
                Optional.of(location.getQuery())
        );

        return params.get("access_token").get(0);
    }

    @Test
    public void getWhenNoProfileShouldReturn200() throws Exception {
        RSAPrivateKey key = getOrCreateRSAPrivateKey.run(2048);
        Client client = loadPublicClientTokenResponseType.run();
        ResourceOwner ro = loadResourceOwner.run();

        List<String> scopes = new ArrayList<>();
        scopes.add("profile");
        scopes.add("email");
        String token = makeToken(client, ro, scopes);

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .prepareGet(servletURI)
                .setHeader("Accept", "application/jwt")
                .setHeader(Header.CONTENT_TYPE.getValue(), ContentType.JSON_UTF_8.getValue())
                .setHeader("Authorization", "Bearer " + token)
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpServletResponse.SC_OK));
        assertThat(response.getContentType(), is(ContentType.JWT_UTF_8.getValue()));
        assertThat(response.getHeader("Cache-Control"), is("no-store"));
        assertThat(response.getHeader("Pragma"), is("no-cache"));

        // verify id token
        JwtAppFactory appFactory = new JwtAppFactory();
        JwtSerde jwtSerde = appFactory.jwtSerde();

        JsonWebToken<IdToken> jwt = jwtSerde.stringToJwt(response.getResponseBody(), IdToken.class);

        RSAPublicKey publicKey = new RSAPublicKey(
                Optional.of(key.getId().toString()),
                Use.SIGNATURE,
                key.getModulus(),
                key.getPublicExponent()
        );

        VerifySignature verifySignature = appFactory.verifySignature(jwt.getHeader().getAlgorithm(), publicKey);
        Boolean signatureVerified = verifySignature.run(jwt);

        assertThat(signatureVerified, is(true));
        // email claims
        IdToken claims = jwt.getClaims();
        assertThat(claims.getEmail().isPresent(), is(true));
        assertThat(claims.getEmail().get(), is(ro.getEmail()));
        assertThat(claims.getEmailVerified().isPresent(), is(true));
        assertThat(claims.getEmailVerified().get(), is(false));

        // profile claims should be empty.
        assertThat(claims.getLastName().isPresent(), is(false));
        assertThat(claims.getFirstName().isPresent(), is(false));
        assertThat(claims.getMiddleName().isPresent(), is(false));
        assertThat(claims.getNickName().isPresent(), is(false));
        assertThat(claims.getPreferredUsername().isPresent(), is(false));
        assertThat(claims.getProfile().isPresent(), is(false));
        assertThat(claims.getPicture().isPresent(), is(false));
        assertThat(claims.getWebsite().isPresent(), is(false));
        assertThat(claims.getGender().isPresent(), is(false));
        assertThat(claims.getBirthdate().isPresent(), is(false));
        assertThat(claims.getZoneInfo().isPresent(), is(false));
        assertThat(claims.getLocale().isPresent(), is(false));
        assertThat(claims.getUpdatedAt().isPresent(), is(false));

        // required claims.
        assertThat(claims.getIssuer().isPresent(), is(true));
        assertThat(claims.getIssuer().get(), is(EntityFactory.ISSUER));
        assertThat(claims.getAudience(), is(notNullValue()));
        assertThat(claims.getAudience().size(), is(1));
        assertThat(claims.getAudience().get(0), is(client.getId().toString()));
        assertThat(claims.getExpirationTime().isPresent(), is(true));
        assertThat(claims.getIssuedAt().isPresent(), is(true));
        assertThat(claims.getAuthenticationTime(), is(notNullValue()));
    }
}
