package net.tokensmith.authorization.http.controller.resource.api.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import helpers.category.ServletContainerTest;
import helpers.fixture.ModelFactory;
import helpers.fixture.persistence.FactoryForPersistence;
import helpers.fixture.persistence.client.confidential.LoadOpenIdConfClientCodeResponseType;
import helpers.fixture.persistence.db.GetOrCreateRSAPrivateKey;
import helpers.fixture.persistence.db.LoadOpenIdResourceOwner;
import helpers.fixture.persistence.http.PostAuthorizationForm;
import helpers.fixture.persistence.http.Session;
import helpers.fixture.persistence.http.input.AuthEndpointProps;
import helpers.fixture.persistence.http.input.AuthEndpointPropsBuilder;
import helpers.suite.IntegrationTestSuite;
import io.netty.handler.codec.http.cookie.Cookie;
import net.tokensmith.authorization.http.controller.resource.api.site.model.Address;
import net.tokensmith.config.AppConfig;
import net.tokensmith.repository.entity.ConfidentialClient;
import net.tokensmith.repository.entity.ResourceOwner;
import org.apache.commons.httpclient.HttpStatus;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

@Category(ServletContainerTest.class)
public class RestAddressResourceTest {

    protected static String baseURI;
    protected static String servletURI;
    protected static String authServletURI;
    private static GetOrCreateRSAPrivateKey getOrCreateRSAPrivateKey;
    protected static LoadOpenIdResourceOwner loadOpenIdResourceOwner;
    private static LoadOpenIdConfClientCodeResponseType loadOpenIdConfidentialClientWithScopes;
    protected static PostAuthorizationForm postAuthorizationForm;


    @BeforeClass
    public static void beforeClass() {
        ApplicationContext ac = IntegrationTestSuite.getContext();
        loadOpenIdConfidentialClientWithScopes = ac.getBean(LoadOpenIdConfClientCodeResponseType.class);
        loadOpenIdResourceOwner = ac.getBean(LoadOpenIdResourceOwner.class);

        FactoryForPersistence factoryForPersistence = new FactoryForPersistence(
                IntegrationTestSuite.getContext()
        );
        getOrCreateRSAPrivateKey = factoryForPersistence.getOrCreateRSAPrivateKey();
        postAuthorizationForm = factoryForPersistence.makePostAuthorizationForm();

        baseURI = String.valueOf(IntegrationTestSuite.getServer().getURI());

        servletURI = baseURI + "api/site/v1/profile/%s/address%s";
        authServletURI = baseURI + "authorization";
    }

    public String targetURI(UUID profileId, Optional<UUID> addressId) {
        if (addressId.isPresent()) {
            return String.format(servletURI, profileId, "/" + addressId.get());
        } else {
            return String.format(servletURI, profileId, "");
        }
    }

    public Session getSession(ConfidentialClient cc, ResourceOwner ro) throws Exception {
        List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        scopes.add("email");

        AuthEndpointProps props = new AuthEndpointPropsBuilder()
                .confidentialClient(cc)
                .baseURI(authServletURI)
                .scopes(scopes)
                .email(ro.getEmail())
                .build();

        String nextUri = baseURI + "profile";
        return postAuthorizationForm.getSessionForProfile(props, nextUri);
    }

    @Test
    public void postShouldBeCreated() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        List<Cookie> cookies = new ArrayList<>();
        cookies.add(session.getCsrf());
        cookies.add(session.getSession());

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        AppConfig config = new AppConfig();
        ObjectMapper om = config.objectMapper();
        byte[] payload = om.writerFor(Address.class).writeValueAsBytes(address);

        String targetURI = targetURI(ro.getProfile().getId(), Optional.empty());

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .preparePost(targetURI)
                .setBody(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setHeader("X-CSRF", session.getCsrfToken())
                .setCookies(cookies)
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_CREATED));

        Address actual = om.readerFor(Address.class).readValue(response.getResponseBody());

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getId(), is(address.getId()));
        assertThat(actual.getProfileId(), is(address.getProfileId()));
        assertThat(actual.getStreetAddress(), is(address.getStreetAddress()));
        assertFalse(actual.getStreetAddress2().isPresent());
        assertThat(actual.getLocality(), is(address.getLocality()));
        assertThat(actual.getRegion(), is(address.getRegion()));
        assertThat(actual.getPostalCode(), is(address.getPostalCode()));
        assertThat(actual.getCountry(), is(address.getCountry()));
    }

    @Test
    public void postWhenNoSessionShouldBeUnauthorized() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        AppConfig config = new AppConfig();
        ObjectMapper om = config.objectMapper();
        byte[] payload = om.writerFor(Address.class).writeValueAsBytes(address);

        String targetURI = targetURI(ro.getProfile().getId(), Optional.empty());

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .preparePost(targetURI)
                .setBody(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setHeader("X-CSRF", session.getCsrfToken())
                .setCookies(Collections.singletonList(session.getCsrf()))
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_UNAUTHORIZED));

        assertThat(response.getResponseBody(), is(""));
    }

    @Test
    public void postWhenNoCsrfShouldBeForbidden() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        AppConfig config = new AppConfig();
        ObjectMapper om = config.objectMapper();
        byte[] payload = om.writerFor(Address.class).writeValueAsBytes(address);

        String targetURI = targetURI(ro.getProfile().getId(), Optional.empty());

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .preparePost(targetURI)
                .setBody(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setCookies(Collections.singletonList(session.getSession()))
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_FORBIDDEN));

        assertThat(response.getResponseBody(), is(""));
    }

    @Test
    public void putShouldBeOk() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        List<Cookie> cookies = new ArrayList<>();
        cookies.add(session.getCsrf());
        cookies.add(session.getSession());

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        AppConfig config = new AppConfig();
        ObjectMapper om = config.objectMapper();
        byte[] payload = om.writerFor(Address.class).writeValueAsBytes(address);

        String targetURI = targetURI(ro.getProfile().getId(), Optional.empty());

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .preparePut(targetURI)
                .setBody(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setHeader("X-CSRF", session.getCsrfToken())
                .setCookies(cookies)
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));

        Address actual = om.readerFor(Address.class).readValue(response.getResponseBody());

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getId(), is(address.getId()));
        assertThat(actual.getProfileId(), is(address.getProfileId()));
        assertThat(actual.getStreetAddress(), is(address.getStreetAddress()));
        assertFalse(actual.getStreetAddress2().isPresent());
        assertThat(actual.getLocality(), is(address.getLocality()));
        assertThat(actual.getRegion(), is(address.getRegion()));
        assertThat(actual.getPostalCode(), is(address.getPostalCode()));
        assertThat(actual.getCountry(), is(address.getCountry()));
    }

    @Test
    public void putWhenNoSessionShouldBeUnauthorized() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        AppConfig config = new AppConfig();
        ObjectMapper om = config.objectMapper();
        byte[] payload = om.writerFor(Address.class).writeValueAsBytes(address);

        String targetURI = targetURI(ro.getProfile().getId(), Optional.empty());

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .preparePut(targetURI)
                .setBody(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setHeader("X-CSRF", session.getCsrfToken())
                .setCookies(Collections.singletonList(session.getCsrf()))
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_UNAUTHORIZED));

        assertThat(response.getResponseBody(), is(""));
    }

    @Test
    public void putWhenNoCsrfShouldBeForbidden() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        AppConfig config = new AppConfig();
        ObjectMapper om = config.objectMapper();
        byte[] payload = om.writerFor(Address.class).writeValueAsBytes(address);

        String targetURI = targetURI(ro.getProfile().getId(), Optional.empty());

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .preparePut(targetURI)
                .setBody(payload)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setCookies(Collections.singletonList(session.getSession()))
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_FORBIDDEN));

        assertThat(response.getResponseBody(), is(""));
    }

    @Test
    public void deleteShouldBeOk() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        List<Cookie> cookies = new ArrayList<>();
        cookies.add(session.getCsrf());
        cookies.add(session.getSession());

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        String targetURI = targetURI(ro.getProfile().getId(), Optional.of(address.getId()));

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .prepareDelete(targetURI)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setHeader("X-CSRF", session.getCsrfToken())
                .setCookies(cookies)
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
    }

    @Test
    public void deleteWhenNoSessionShouldBeUnauthorized() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        String targetURI = targetURI(ro.getProfile().getId(), Optional.of(address.getId()));

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .prepareDelete(targetURI)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setHeader("X-CSRF", session.getCsrfToken())
                .setCookies(Collections.singletonList(session.getCsrf()))
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_UNAUTHORIZED));

        assertThat(response.getResponseBody(), is(""));
    }

    @Test
    public void deleteWhenNoCsrfShouldBeForbidden() throws Exception {
        ConfidentialClient cc = loadOpenIdConfidentialClientWithScopes.run();
        ResourceOwner ro = loadOpenIdResourceOwner.run();

        Session session = getSession(cc, ro);

        Address address = ModelFactory.makeAddress(ro.getProfile().getId());
        String targetURI = targetURI(ro.getProfile().getId(), Optional.of(address.getId()));

        ListenableFuture<Response> f = IntegrationTestSuite.getHttpClient()
                .prepareDelete(targetURI)
                .setHeader("Content-Type", "application/json; charset=utf-8;")
                .setHeader("Accept", "application/json; charset=utf-8;")
                .setCookies(Collections.singletonList(session.getSession()))
                .execute();

        Response response = f.get();

        assertThat(response.getStatusCode(), is(HttpStatus.SC_FORBIDDEN));

        assertThat(response.getResponseBody(), is(""));
    }

}