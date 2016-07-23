package org.rootservices.authorization.oauth2.grant.redirect.code.authorization.response.factory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rootservices.authorization.constant.ErrorCode;
import org.rootservices.authorization.oauth2.grant.redirect.authorization.request.exception.InformResourceOwnerException;
import org.rootservices.authorization.oauth2.grant.redirect.code.authorization.response.AuthResponse;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.repository.ClientRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


/**
 * Created by tommackenzie on 4/29/15.
 */
public class AuthResponseFactoryTest {

    @Mock
    private ClientRepository clientRepository;

    private AuthResponseFactory subject;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new AuthResponseFactory(clientRepository);
    }

    @Test
    public void redirectUriIsPresent() throws InformResourceOwnerException, URISyntaxException {
        UUID clientUUID = UUID.randomUUID();
        String authCode = "authorization-code";
        Optional<String> state = Optional.of("csrf");
        Optional<URI> redirectUri = Optional.of(new URI("https://rootservices.org"));

        AuthResponse actual = subject.makeAuthResponse(
                clientUUID,
                authCode,
                state,
                redirectUri
        );

        assertThat(actual.getState()).isEqualTo(state);
        assertThat(actual.getRedirectUri()).isEqualTo(redirectUri.get());
        assertThat(actual.getCode()).isEqualTo(authCode);
    }

    @Test
    public void redirectUriIsNotPresentClientNotFound() throws URISyntaxException, RecordNotFoundException {
        UUID clientUUID = UUID.randomUUID();
        String authCode = "authorization-code";
        Optional<String> state = Optional.of("csrf");
        Optional<URI> redirectUri = Optional.empty();

        when(clientRepository.getById(clientUUID)).thenThrow(RecordNotFoundException.class);

        AuthResponse actual = null;
        try {
            actual = subject.makeAuthResponse(
                    clientUUID,
                    authCode,
                    state,
                    redirectUri
            );
        } catch (InformResourceOwnerException e) {
            assertThat(e.getCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND.getCode());
            assertThat(e.getDomainCause()).isInstanceOf(RecordNotFoundException.class);
        }

        assertThat(actual).isNull();
    }
}