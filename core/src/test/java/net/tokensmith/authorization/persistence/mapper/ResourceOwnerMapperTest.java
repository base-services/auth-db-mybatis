package net.tokensmith.authorization.persistence.mapper;

import helper.fixture.FixtureFactory;
import helper.fixture.TestAppConfig;
import net.tokensmith.repository.entity.Address;
import net.tokensmith.repository.entity.Client;
import net.tokensmith.repository.entity.Gender;
import net.tokensmith.repository.entity.GrantType;
import net.tokensmith.repository.entity.LocalToken;
import net.tokensmith.repository.entity.Name;
import net.tokensmith.repository.entity.Profile;
import net.tokensmith.repository.entity.ResourceOwner;
import net.tokensmith.repository.entity.ResourceOwnerToken;
import net.tokensmith.repository.entity.Scope;
import net.tokensmith.repository.entity.Token;
import net.tokensmith.repository.entity.TokenAudience;
import net.tokensmith.repository.entity.TokenLeadToken;
import net.tokensmith.repository.entity.TokenScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes= TestAppConfig.class, loader= AnnotationConfigContextLoader.class)
@Transactional
public class ResourceOwnerMapperTest {
    @Autowired
    private ResourceOwnerMapper subject;
    @Autowired
    private ClientMapper clientMapper;
    @Autowired
    private ResourceOwnerTokenMapper resourceOwnerTokenMapper;
    @Autowired
    private TokenMapper tokenMapper;
    @Autowired
    private TokenLeadTokenMapper tokenLeadTokenMapper;
    @Autowired
    private ScopeMapper scopeMapper;
    @Autowired
    private TokenScopeMapper tokenScopeMapper;
    @Autowired
    private ProfileMapper profileMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private GivenNameMapper givenNameMapper;
    @Autowired
    private FamilyNameMapper familyNameMapper;
    @Autowired
    private TokenAudienceMapper tokenAudienceMapper;
    @Autowired
    private LocalTokenMapper localTokenMapper;

    public ResourceOwner insertResourceOwner(boolean emailVerified) {
        UUID uuid = UUID.randomUUID();
        String password = "plainTextPassword";
        ResourceOwner user = new ResourceOwner(uuid, UUID.randomUUID() + "@tokensmith.net", password);
        user.setEmailVerified(emailVerified);

        subject.insert(user);
        return user;
    }

    public LocalToken insertLocalToken(UUID resourceOwnerId, boolean revoked, OffsetDateTime expiresAt) {
        LocalToken localToken = new LocalToken.Builder()
                .id(UUID.randomUUID())
                .token("local-access-token")
                .resourceOwnerId(resourceOwnerId)
                .revoked(revoked)
                .expiresAt(expiresAt)
                .createdAt(OffsetDateTime.now())
                .build();
        localTokenMapper.insert(localToken);

        return localToken;
    }

    @Test
    public void insert() {
        UUID uuid = UUID.randomUUID();
        String password = "plainTextPassword";
        ResourceOwner user = new ResourceOwner(uuid, "test@tokensmith.net", password);
        subject.insert(user);
    }

    @Test(expected = DuplicateKeyException.class)
    public void insertShouldThrowDuplicateKeyException() {
        UUID id = UUID.randomUUID();
        String password = "plainTextPassword";
        ResourceOwner user = new ResourceOwner(id, "test@tokensmith.net", password);
        subject.insert(user);

        UUID id2 = UUID.randomUUID();
        ResourceOwner user2 = new ResourceOwner(id2, "test@tokensmith.net", password);

        subject.insert(user2);
    }


    @Test
    public void getById() {
        ResourceOwner expectedUser = insertResourceOwner(false);
        ResourceOwner actual = subject.getById(expectedUser.getId());

        assertThat(actual.getId(), is(expectedUser.getId()));
        assertThat(actual.getEmail(), is(expectedUser.getEmail()));
        assertThat(actual.getPassword(), is(expectedUser.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void getByIdAuthUserNotFound() {
        ResourceOwner actual = subject.getById(UUID.randomUUID());

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void getByEmail() {
        ResourceOwner expectedUser = insertResourceOwner(false);
        ResourceOwner actual = subject.getByEmail(expectedUser.getEmail());

        assertThat(actual.getId(), is(expectedUser.getId()));
        assertThat(actual.getEmail(), is(expectedUser.getEmail()));
        assertThat(actual.getPassword(), is(expectedUser.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));
        assertThat(actual.getTokens().size(), is(0));
        assertThat(actual.getLocalTokens().size(), is(0));
    }

    @Test
    public void getByAccessToken() throws Exception {
        // prepare data for test.
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner expectedUser = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(token);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(expectedUser);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);
        // end prepare

        String hashedAccessToken = new String(token.getToken());
        ResourceOwner actual = subject.getByAccessToken(hashedAccessToken);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getEmail(), is(expectedUser.getEmail()));
        assertThat(actual.getPassword(), is(expectedUser.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));
        assertThat(actual.getProfile(), is(nullValue()));
        assertThat(actual.getTokens(), is(notNullValue()));
        assertThat(actual.getTokens().size(), is(0));
        assertThat(actual.getLocalTokens().size(), is(0));
    }

    @Test
    public void getByAccessTokenWithProfileAndTokensShouldBeOk() throws Exception {

        // prepare database for the test
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner ro = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        token.setGrantType(GrantType.REFRESSH);
        tokenMapper.insert(token);

        TokenAudience tokenAudience = new TokenAudience();
        tokenAudience.setId(UUID.randomUUID());
        tokenAudience.setTokenId(token.getId());
        tokenAudience.setClientId(client.getId());
        tokenAudienceMapper.insert(tokenAudience);

        String leadAccessToken = "lead-access-token";
        Token leadToken = FixtureFactory.makeOpenIdToken(leadAccessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(leadToken);

        TokenLeadToken tlt = new TokenLeadToken();
        tlt.setId(UUID.randomUUID());
        tlt.setTokenId(token.getId());
        tlt.setLeadTokenId(leadToken.getId());
        tokenLeadTokenMapper.insert(tlt);

        Scope scope = FixtureFactory.makeScope();
        scope.setName("address");
        scopeMapper.insert(scope);

        TokenScope tokenScope = new TokenScope();
        tokenScope.setId(UUID.randomUUID());
        tokenScope.setTokenId(token.getId());
        tokenScope.setScope(scope);
        tokenScopeMapper.insert(tokenScope);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(ro);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);

        Profile profile = FixtureFactory.makeProfile(ro.getId());
        profileMapper.insert(profile);

        Name givenName = FixtureFactory.makeGivenName(profile.getId());
        givenNameMapper.insert(givenName);

        Name familyName = FixtureFactory.makeFamilyName(profile.getId());
        familyNameMapper.insert(familyName);

        Address address = FixtureFactory.makeAddress(profile.getId());
        addressMapper.insert(address);
        // end: prepare database for the test

        String hashedAccessToken = new String(token.getToken());
        ResourceOwner actual = subject.getByAccessTokenWithProfileAndTokens(hashedAccessToken);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getEmail(), is(ro.getEmail()));
        assertThat(actual.getPassword(), is(ro.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));


        assertThat(actual.getTokens(), is(notNullValue()));
        assertThat(actual.getTokens().size(), is(1));
        assertThat(actual.getTokens().get(0).getId(), is(token.getId()));
        assertThat(actual.getTokens().get(0).isRevoked(), is(false));
        assertThat(actual.getTokens().get(0).getGrantType(), is(GrantType.REFRESSH));
        assertThat(actual.getTokens().get(0).getClientId(), is(client.getId()));
        assertThat(actual.getTokens().get(0).getLeadAuthTime(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getCreatedAt(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getExpiresAt(), is(notNullValue()));

        assertThat(actual.getTokens().get(0).getAudience(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getAudience().size(), is(1));
        assertThat(actual.getTokens().get(0).getAudience().get(0), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getAudience().get(0).getId(), is(client.getId()));
        assertThat(actual.getTokens().get(0).getAudience().get(0).getRedirectURI(), is(client.getRedirectURI()));
        assertThat(actual.getTokens().get(0).getAudience().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getTokens().get(0).getLeadToken(), is(nullValue()));

        assertThat(actual.getTokens().get(0).getTokenScopes(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getTokenScopes().size(), is(1));
        assertThat(actual.getTokens().get(0).getTokenScopes().get(0).getScope().getName(), is("address"));

        assertThat(actual.getLocalTokens().size(), is(0));

        assertThat(actual.getProfile(), is(notNullValue()));
        assertThat(actual.getProfile().getName().isPresent(), is(true));
        assertThat(actual.getProfile().getName().get(), is("Obi-Wan Kenobi"));
        assertThat(actual.getProfile().getMiddleName().isPresent(), is(false));
        assertThat(actual.getProfile().getNickName().isPresent(), is(true));
        assertThat(actual.getProfile().getNickName().get(), is("Ben"));
        assertThat(actual.getProfile().getPreferredUserName().isPresent(), is(true));
        assertThat(actual.getProfile().getPreferredUserName().get(), is("Ben Kenobi"));
        assertThat(actual.getProfile().getProfile().isPresent(), is(true));
        assertThat(actual.getProfile().getProfile().get().toString(), is("http://starwars.wikia.com/wiki/Obi-Wan_Kenobi"));
        assertThat(actual.getProfile().getPicture().isPresent(), is(true));
        assertThat(actual.getProfile().getPicture().get().toString(), is("http://vignette1.wikia.nocookie.net/starwars/images/2/25/Kenobi_Maul_clash.png/revision/latest?cb=20130120033039"));
        assertThat(actual.getProfile().getWebsite().isPresent(), is(true));
        assertThat(actual.getProfile().getWebsite().get().toString(), is("http://starwars.wikia.com"));
        assertThat(actual.getProfile().getGender().isPresent(), is(true));
        assertThat(actual.getProfile().getGender().get(), is(Gender.MALE));
        assertThat(actual.getProfile().getBirthDate().isPresent(), is(false));
        assertThat(actual.getProfile().getZoneInfo().isPresent(), is(false));
        assertThat(actual.getProfile().getLocale().isPresent(), is(false));
        assertThat(actual.getProfile().getPhoneNumber().isPresent(), is(false));
        assertThat(actual.getProfile().isPhoneNumberVerified(), is(false));

        assertThat(actual.getProfile().getAddresses(), is(notNullValue()));
        assertThat(actual.getProfile().getAddresses().size(), is(1));
        assertThat(actual.getProfile().getAddresses().get(0).getId(), is(address.getId()));
        assertThat(actual.getProfile().getAddresses().get(0).getStreetAddress(), is(address.getStreetAddress()));
        assertThat(actual.getProfile().getAddresses().get(0).getStreetAddress2(), is(address.getStreetAddress2()));
        assertThat(actual.getProfile().getAddresses().get(0).getLocality(), is(address.getLocality()));
        assertThat(actual.getProfile().getAddresses().get(0).getRegion(), is(address.getRegion()));
        assertThat(actual.getProfile().getAddresses().get(0).getPostalCode(), is(address.getPostalCode()));
        assertThat(actual.getProfile().getAddresses().get(0).getCountry(), is(address.getCountry()));
        assertThat(actual.getProfile().getAddresses().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getAddresses().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getGivenNames(), is(notNullValue()));
        assertThat(actual.getProfile().getGivenNames().size(), is(1));
        assertThat(actual.getProfile().getGivenNames().get(0).getId(), is(givenName.getId()));
        assertThat(actual.getProfile().getGivenNames().get(0).getName(), is(givenName.getName()));
        assertThat(actual.getProfile().getGivenNames().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getGivenNames().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getFamilyNames(), is(notNullValue()));
        assertThat(actual.getProfile().getFamilyNames().size(), is(1));
        assertThat(actual.getProfile().getFamilyNames().get(0).getId(), is(familyName.getId()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getName(), is(familyName.getName()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void getByAccessTokenWithProfileAndTokensWhenNoProfileShouldBeOk() throws Exception {

        // prepare database for the test
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner ro = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        token.setGrantType(GrantType.REFRESSH);
        tokenMapper.insert(token);

        TokenAudience tokenAudience = new TokenAudience();
        tokenAudience.setId(UUID.randomUUID());
        tokenAudience.setTokenId(token.getId());
        tokenAudience.setClientId(client.getId());
        tokenAudienceMapper.insert(tokenAudience);

        String leadAccessToken = "lead-access-token";
        Token leadToken = FixtureFactory.makeOpenIdToken(leadAccessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(leadToken);

        TokenLeadToken tlt = new TokenLeadToken();
        tlt.setId(UUID.randomUUID());
        tlt.setTokenId(token.getId());
        tlt.setLeadTokenId(leadToken.getId());
        tokenLeadTokenMapper.insert(tlt);

        Scope scope = FixtureFactory.makeScope();
        scope.setName("address");
        scopeMapper.insert(scope);

        TokenScope tokenScope = new TokenScope();
        tokenScope.setId(UUID.randomUUID());
        tokenScope.setTokenId(token.getId());
        tokenScope.setScope(scope);
        tokenScopeMapper.insert(tokenScope);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(ro);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);
        // end: prepare database for the test


        ResourceOwner actual = subject.getByAccessTokenWithProfileAndTokens(token.getToken());

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getEmail(), is(ro.getEmail()));
        assertThat(actual.getPassword(), is(ro.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));


        assertThat(actual.getTokens(), is(notNullValue()));
        assertThat(actual.getTokens().size(), is(1));
        assertThat(actual.getTokens().get(0).getId(), is(token.getId()));
        assertThat(actual.getTokens().get(0).isRevoked(), is(false));
        assertThat(actual.getTokens().get(0).getGrantType(), is(GrantType.REFRESSH));
        assertThat(actual.getTokens().get(0).getClientId(), is(client.getId()));
        assertThat(actual.getTokens().get(0).getLeadAuthTime(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getCreatedAt(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getExpiresAt(), is(notNullValue()));

        assertThat(actual.getTokens().get(0).getAudience(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getAudience().size(), is(1));
        assertThat(actual.getTokens().get(0).getAudience().get(0), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getAudience().get(0).getId(), is(client.getId()));
        assertThat(actual.getTokens().get(0).getAudience().get(0).getRedirectURI(), is(client.getRedirectURI()));
        assertThat(actual.getTokens().get(0).getAudience().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getTokens().get(0).getLeadToken(), is(nullValue()));

        assertThat(actual.getTokens().get(0).getTokenScopes(), is(notNullValue()));
        assertThat(actual.getTokens().get(0).getTokenScopes().size(), is(1));
        assertThat(actual.getTokens().get(0).getTokenScopes().get(0).getScope().getName(), is("address"));

        assertThat(actual.getLocalTokens().size(), is(0));

        assertThat(actual.getProfile(), is(nullValue()));
    }

    @Test
    public void getByAccessTokenWithProfileAndTokensWhenTokenExpiredShouldBeNull() throws Exception {

        // prepare database for the test
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner ro = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        token.setGrantType(GrantType.REFRESSH);
        token.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        // Expired! ^
        tokenMapper.insert(token);

        TokenAudience tokenAudience = new TokenAudience();
        tokenAudience.setId(UUID.randomUUID());
        tokenAudience.setTokenId(token.getId());
        tokenAudience.setClientId(client.getId());
        tokenAudienceMapper.insert(tokenAudience);

        String leadAccessToken = "lead-access-token";
        Token leadToken = FixtureFactory.makeOpenIdToken(leadAccessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(leadToken);

        TokenLeadToken tlt = new TokenLeadToken();
        tlt.setId(UUID.randomUUID());
        tlt.setTokenId(token.getId());
        tlt.setLeadTokenId(leadToken.getId());
        tokenLeadTokenMapper.insert(tlt);

        Scope scope = FixtureFactory.makeScope();
        scope.setName("address");
        scopeMapper.insert(scope);

        TokenScope tokenScope = new TokenScope();
        tokenScope.setId(UUID.randomUUID());
        tokenScope.setTokenId(token.getId());
        tokenScope.setScope(scope);
        tokenScopeMapper.insert(tokenScope);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(ro);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);
        // end: prepare database for the test

        String hashedAccessToken = new String(token.getToken());
        ResourceOwner actual = subject.getByAccessTokenWithProfileAndTokens(hashedAccessToken);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void getByAccessTokenWithProfileAndTokensWhenTokenRevokedShouldBeNull() throws Exception {

        // prepare database for the test
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner ro = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        token.setGrantType(GrantType.REFRESSH);
        token.setRevoked(true);
        // Revoked! ^
        tokenMapper.insert(token);

        TokenAudience tokenAudience = new TokenAudience();
        tokenAudience.setId(UUID.randomUUID());
        tokenAudience.setTokenId(token.getId());
        tokenAudience.setClientId(client.getId());
        tokenAudienceMapper.insert(tokenAudience);

        String leadAccessToken = "lead-access-token";
        Token leadToken = FixtureFactory.makeOpenIdToken(leadAccessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(leadToken);

        TokenLeadToken tlt = new TokenLeadToken();
        tlt.setId(UUID.randomUUID());
        tlt.setTokenId(token.getId());
        tlt.setLeadTokenId(leadToken.getId());
        tokenLeadTokenMapper.insert(tlt);

        Scope scope = FixtureFactory.makeScope();
        scope.setName("address");
        scopeMapper.insert(scope);

        TokenScope tokenScope = new TokenScope();
        tokenScope.setId(UUID.randomUUID());
        tokenScope.setTokenId(token.getId());
        tokenScope.setScope(scope);
        tokenScopeMapper.insert(tokenScope);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(ro);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);
        // end: prepare database for the test

        String hashedAccessToken = new String(token.getToken());
        ResourceOwner actual = subject.getByAccessTokenWithProfileAndTokens(hashedAccessToken);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void getByIdWithProfileShouldBeOk() throws Exception {

        // prepare database for the test
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner ro = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        token.setGrantType(GrantType.REFRESSH);
        tokenMapper.insert(token);

        TokenAudience tokenAudience = new TokenAudience();
        tokenAudience.setId(UUID.randomUUID());
        tokenAudience.setTokenId(token.getId());
        tokenAudience.setClientId(client.getId());
        tokenAudienceMapper.insert(tokenAudience);

        String leadAccessToken = "lead-access-token";
        Token leadToken = FixtureFactory.makeOpenIdToken(leadAccessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(leadToken);

        TokenLeadToken tlt = new TokenLeadToken();
        tlt.setId(UUID.randomUUID());
        tlt.setTokenId(token.getId());
        tlt.setLeadTokenId(leadToken.getId());
        tokenLeadTokenMapper.insert(tlt);

        Scope scope = FixtureFactory.makeScope();
        scope.setName("address");
        scopeMapper.insert(scope);

        TokenScope tokenScope = new TokenScope();
        tokenScope.setId(UUID.randomUUID());
        tokenScope.setTokenId(token.getId());
        tokenScope.setScope(scope);
        tokenScopeMapper.insert(tokenScope);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(ro);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);

        Profile profile = FixtureFactory.makeProfile(ro.getId());
        profileMapper.insert(profile);

        Name givenName = FixtureFactory.makeGivenName(profile.getId());
        givenNameMapper.insert(givenName);

        Name familyName = FixtureFactory.makeFamilyName(profile.getId());
        familyNameMapper.insert(familyName);

        Address address = FixtureFactory.makeAddress(profile.getId());
        addressMapper.insert(address);
        // end: prepare database for the test


        ResourceOwner actual = subject.getByIdWithProfile(ro.getId());

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getEmail(), is(ro.getEmail()));
        assertThat(actual.getPassword(), is(ro.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));


        assertThat(actual.getTokens(), is(notNullValue()));
        assertThat(actual.getTokens().size(), is(0));

        assertThat(actual.getProfile(), is(notNullValue()));
        assertThat(actual.getProfile().getName().isPresent(), is(true));
        assertThat(actual.getProfile().getName().get(), is("Obi-Wan Kenobi"));
        assertThat(actual.getProfile().getMiddleName().isPresent(), is(false));
        assertThat(actual.getProfile().getNickName().isPresent(), is(true));
        assertThat(actual.getProfile().getNickName().get(), is("Ben"));
        assertThat(actual.getProfile().getPreferredUserName().isPresent(), is(true));
        assertThat(actual.getProfile().getPreferredUserName().get(), is("Ben Kenobi"));
        assertThat(actual.getProfile().getProfile().isPresent(), is(true));
        assertThat(actual.getProfile().getProfile().get().toString(), is("http://starwars.wikia.com/wiki/Obi-Wan_Kenobi"));
        assertThat(actual.getProfile().getPicture().isPresent(), is(true));
        assertThat(actual.getProfile().getPicture().get().toString(), is("http://vignette1.wikia.nocookie.net/starwars/images/2/25/Kenobi_Maul_clash.png/revision/latest?cb=20130120033039"));
        assertThat(actual.getProfile().getWebsite().isPresent(), is(true));
        assertThat(actual.getProfile().getWebsite().get().toString(), is("http://starwars.wikia.com"));
        assertThat(actual.getProfile().getGender().isPresent(), is(true));
        assertThat(actual.getProfile().getGender().get(), is(Gender.MALE));
        assertThat(actual.getProfile().getBirthDate().isPresent(), is(false));
        assertThat(actual.getProfile().getZoneInfo().isPresent(), is(false));
        assertThat(actual.getProfile().getLocale().isPresent(), is(false));
        assertThat(actual.getProfile().getPhoneNumber().isPresent(), is(false));
        assertThat(actual.getProfile().isPhoneNumberVerified(), is(false));

        assertThat(actual.getProfile().getAddresses(), is(notNullValue()));
        assertThat(actual.getProfile().getAddresses().size(), is(1));
        assertThat(actual.getProfile().getAddresses().get(0).getId(), is(address.getId()));
        assertThat(actual.getProfile().getAddresses().get(0).getStreetAddress(), is(address.getStreetAddress()));
        assertThat(actual.getProfile().getAddresses().get(0).getStreetAddress2(), is(address.getStreetAddress2()));
        assertThat(actual.getProfile().getAddresses().get(0).getLocality(), is(address.getLocality()));
        assertThat(actual.getProfile().getAddresses().get(0).getRegion(), is(address.getRegion()));
        assertThat(actual.getProfile().getAddresses().get(0).getPostalCode(), is(address.getPostalCode()));
        assertThat(actual.getProfile().getAddresses().get(0).getCountry(), is(address.getCountry()));
        assertThat(actual.getProfile().getAddresses().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getAddresses().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getGivenNames(), is(notNullValue()));
        assertThat(actual.getProfile().getGivenNames().size(), is(1));
        assertThat(actual.getProfile().getGivenNames().get(0).getId(), is(givenName.getId()));
        assertThat(actual.getProfile().getGivenNames().get(0).getName(), is(givenName.getName()));
        assertThat(actual.getProfile().getGivenNames().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getGivenNames().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getFamilyNames(), is(notNullValue()));
        assertThat(actual.getProfile().getFamilyNames().size(), is(1));
        assertThat(actual.getProfile().getFamilyNames().get(0).getId(), is(familyName.getId()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getName(), is(familyName.getName()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void getByIdWithProfileWhenNoProfileShouldBeOk() throws Exception {

        // prepare database for the test
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner ro = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        token.setGrantType(GrantType.REFRESSH);
        tokenMapper.insert(token);

        TokenAudience tokenAudience = new TokenAudience();
        tokenAudience.setId(UUID.randomUUID());
        tokenAudience.setTokenId(token.getId());
        tokenAudience.setClientId(client.getId());
        tokenAudienceMapper.insert(tokenAudience);

        String leadAccessToken = "lead-access-token";
        Token leadToken = FixtureFactory.makeOpenIdToken(leadAccessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(leadToken);

        TokenLeadToken tlt = new TokenLeadToken();
        tlt.setId(UUID.randomUUID());
        tlt.setTokenId(token.getId());
        tlt.setLeadTokenId(leadToken.getId());
        tokenLeadTokenMapper.insert(tlt);

        Scope scope = FixtureFactory.makeScope();
        scope.setName("address");
        scopeMapper.insert(scope);

        TokenScope tokenScope = new TokenScope();
        tokenScope.setId(UUID.randomUUID());
        tokenScope.setTokenId(token.getId());
        tokenScope.setScope(scope);
        tokenScopeMapper.insert(tokenScope);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(ro);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);
        // end: prepare database for the test


        ResourceOwner actual = subject.getByIdWithProfile(ro.getId());

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getEmail(), is(ro.getEmail()));
        assertThat(actual.getPassword(), is(ro.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));


        assertThat(actual.getTokens(), is(notNullValue()));
        assertThat(actual.getTokens().size(), is(0));
        assertThat(actual.getLocalTokens().size(), is(0));

        assertThat(actual.getProfile(), is(nullValue()));
    }

    @Test
    public void getByIdWithProfileWhenNoAddressShouldBeOk() throws Exception {

        // prepare database for the test
        Client client = FixtureFactory.makeCodeClientWithOpenIdScopes();
        clientMapper.insert(client);

        ResourceOwner ro = insertResourceOwner(false);

        String accessToken = "access-token";
        Token token = FixtureFactory.makeOpenIdToken(accessToken, client.getId(), new ArrayList<>());
        token.setGrantType(GrantType.REFRESSH);
        tokenMapper.insert(token);

        TokenAudience tokenAudience = new TokenAudience();
        tokenAudience.setId(UUID.randomUUID());
        tokenAudience.setTokenId(token.getId());
        tokenAudience.setClientId(client.getId());
        tokenAudienceMapper.insert(tokenAudience);

        String leadAccessToken = "lead-access-token";
        Token leadToken = FixtureFactory.makeOpenIdToken(leadAccessToken, client.getId(), new ArrayList<>());
        tokenMapper.insert(leadToken);

        TokenLeadToken tlt = new TokenLeadToken();
        tlt.setId(UUID.randomUUID());
        tlt.setTokenId(token.getId());
        tlt.setLeadTokenId(leadToken.getId());
        tokenLeadTokenMapper.insert(tlt);

        Scope scope = FixtureFactory.makeScope();
        scope.setName("address");
        scopeMapper.insert(scope);

        TokenScope tokenScope = new TokenScope();
        tokenScope.setId(UUID.randomUUID());
        tokenScope.setTokenId(token.getId());
        tokenScope.setScope(scope);
        tokenScopeMapper.insert(tokenScope);

        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(ro);
        resourceOwnerToken.setToken(token);
        resourceOwnerTokenMapper.insert(resourceOwnerToken);

        Profile profile = FixtureFactory.makeProfile(ro.getId());
        profileMapper.insert(profile);

        Name givenName = FixtureFactory.makeGivenName(profile.getId());
        givenNameMapper.insert(givenName);

        Name familyName = FixtureFactory.makeFamilyName(profile.getId());
        familyNameMapper.insert(familyName);

        // end: prepare database for the test

        ResourceOwner actual = subject.getByIdWithProfile(ro.getId());

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getEmail(), is(ro.getEmail()));
        assertThat(actual.getPassword(), is(ro.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));

        assertThat(actual.getTokens(), is(notNullValue()));
        assertThat(actual.getTokens().size(), is(0));

        assertThat(actual.getProfile(), is(notNullValue()));
        assertThat(actual.getProfile().getName().isPresent(), is(true));
        assertThat(actual.getProfile().getName().get(), is("Obi-Wan Kenobi"));
        assertThat(actual.getProfile().getMiddleName().isPresent(), is(false));
        assertThat(actual.getProfile().getNickName().isPresent(), is(true));
        assertThat(actual.getProfile().getNickName().get(), is("Ben"));
        assertThat(actual.getProfile().getPreferredUserName().isPresent(), is(true));
        assertThat(actual.getProfile().getPreferredUserName().get(), is("Ben Kenobi"));
        assertThat(actual.getProfile().getProfile().isPresent(), is(true));
        assertThat(actual.getProfile().getProfile().get().toString(), is("http://starwars.wikia.com/wiki/Obi-Wan_Kenobi"));
        assertThat(actual.getProfile().getPicture().isPresent(), is(true));
        assertThat(actual.getProfile().getPicture().get().toString(), is("http://vignette1.wikia.nocookie.net/starwars/images/2/25/Kenobi_Maul_clash.png/revision/latest?cb=20130120033039"));
        assertThat(actual.getProfile().getWebsite().isPresent(), is(true));
        assertThat(actual.getProfile().getWebsite().get().toString(), is("http://starwars.wikia.com"));
        assertThat(actual.getProfile().getGender().isPresent(), is(true));
        assertThat(actual.getProfile().getGender().get(), is(Gender.MALE));
        assertThat(actual.getProfile().getBirthDate().isPresent(), is(false));
        assertThat(actual.getProfile().getZoneInfo().isPresent(), is(false));
        assertThat(actual.getProfile().getLocale().isPresent(), is(false));
        assertThat(actual.getProfile().getPhoneNumber().isPresent(), is(false));
        assertThat(actual.getProfile().isPhoneNumberVerified(), is(false));

        assertThat(actual.getProfile().getAddresses(), is(notNullValue()));
        assertThat(actual.getProfile().getAddresses().size(), is(0));

        assertThat(actual.getProfile().getGivenNames(), is(notNullValue()));
        assertThat(actual.getProfile().getGivenNames().size(), is(1));
        assertThat(actual.getProfile().getGivenNames().get(0).getId(), is(givenName.getId()));
        assertThat(actual.getProfile().getGivenNames().get(0).getName(), is(givenName.getName()));
        assertThat(actual.getProfile().getGivenNames().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getGivenNames().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getFamilyNames(), is(notNullValue()));
        assertThat(actual.getProfile().getFamilyNames().size(), is(1));
        assertThat(actual.getProfile().getFamilyNames().get(0).getId(), is(familyName.getId()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getName(), is(familyName.getName()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getFamilyNames().get(0).getCreatedAt(), is(notNullValue()));

        assertThat(actual.getProfile().getUpdatedAt(), is(notNullValue()));
        assertThat(actual.getProfile().getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void getByLocalTokenShouldBeOk() {
        ResourceOwner expectedUser = insertResourceOwner(false);
        LocalToken localToken = insertLocalToken(expectedUser.getId(), false, OffsetDateTime.now().plusDays(1));

        ResourceOwner actual = subject.getByLocalToken(localToken.getToken());

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getId(), is(expectedUser.getId()));
        assertThat(actual.getEmail(), is(expectedUser.getEmail()));
        assertThat(actual.getPassword(), is(expectedUser.getPassword()));
        assertThat(actual.isEmailVerified(), is(false));
        assertThat(actual.getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void getByLocalTokenWhenRevokedShouldBeNull() {
        ResourceOwner expectedUser = insertResourceOwner(false);
        LocalToken localToken = insertLocalToken(expectedUser.getId(), true, OffsetDateTime.now().plusDays(1));

        ResourceOwner actual = subject.getByLocalToken(localToken.getToken());

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void getByLocalTokenWhenExpiredShouldBeNull() {
        ResourceOwner expectedUser = insertResourceOwner(false);
        LocalToken localToken = insertLocalToken(expectedUser.getId(), false, OffsetDateTime.now().minusDays(1));

        ResourceOwner actual = subject.getByLocalToken(localToken.getToken());

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void setEmailVerifiedShouldSetToTrue() {
        ResourceOwner user = insertResourceOwner(false);

        // should be false for email verified.
        ResourceOwner insertedUser = subject.getById(user.getId());
        assertThat(insertedUser.isEmailVerified(), is(false));

        subject.setEmailVerified(user.getId());
        ResourceOwner actual = subject.getById(user.getId());

        assertThat(actual.getId(), is(user.getId()));
        assertThat(actual.getEmail(), is(user.getEmail()));
        assertThat(actual.getPassword(), is(user.getPassword()));

        // should be true for email verified.
        assertThat(actual.isEmailVerified(), is(true));
        assertThat(actual.getCreatedAt(), is(notNullValue()));
    }

    @Test
    public void updatePasswordShouldBeOk() {
        ResourceOwner user = insertResourceOwner(false);
        ResourceOwner userToUpdate = insertResourceOwner(false);
        String password = "plainTextPassword123";

        subject.updatePassword(userToUpdate.getId(), password);

        // fetch it to make sure it was updated.
        ResourceOwner actual = subject.getById(userToUpdate.getId());
        assertThat(actual.getPassword(), is(password));

        // should not have updated others passwords.
        ResourceOwner otherUser = subject.getById(user.getId());
        assertThat(otherUser.getPassword(), is(not(password.getBytes())));
    }


    @Test
    public void updateEmailShouldBeOk() {
        ResourceOwner user = insertResourceOwner(false);
        ResourceOwner userToUpdate = insertResourceOwner(true);
        String email = "obi-wan@tokensmith.net";

        subject.updateEmail(userToUpdate.getId(), email);

        // fetch it to make sure it was updated.
        ResourceOwner actual = subject.getById(userToUpdate.getId());
        assertThat(actual.getEmail(), is(email));
        assertThat(actual.isEmailVerified(), is(false));

        // should not have updated others emails.
        ResourceOwner otherUser = subject.getById(user.getId());
        assertThat(otherUser.getEmail(), is(not(email.getBytes())));
    }
}
