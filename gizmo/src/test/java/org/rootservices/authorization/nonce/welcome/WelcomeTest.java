package org.rootservices.authorization.nonce.welcome;

import helper.fixture.FixtureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rootservices.authorization.exception.BadRequestException;
import org.rootservices.authorization.exception.NotFoundException;
import org.rootservices.authorization.nonce.entity.NonceName;
import org.rootservices.authorization.persistence.entity.Nonce;
import org.rootservices.authorization.persistence.entity.NonceType;
import org.rootservices.authorization.persistence.entity.ResourceOwner;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.repository.NonceRepository;
import org.rootservices.authorization.persistence.repository.ResourceOwnerRepository;
import org.rootservices.authorization.security.ciphers.HashTextStaticSalt;
import org.rootservices.authorization.security.entity.NonceClaim;
import org.rootservices.jwt.builder.compact.UnsecureCompactBuilder;
import org.rootservices.jwt.exception.InvalidJWT;


import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WelcomeTest {

    @Mock
    private HashTextStaticSalt mockHashTextStaticSalt;
    @Mock
    private NonceRepository mockNonceRepository;
    @Mock
    private ResourceOwnerRepository mockResourceOwnerRepository;

    private Welcome subject;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        subject = new Welcome(mockHashTextStaticSalt, mockNonceRepository, mockResourceOwnerRepository);
    }

    @Test
    public void markEmailVerifiedShouldBeOK() throws Exception {
        // make a jwt for the test.
        UnsecureCompactBuilder compactBuilder = new UnsecureCompactBuilder();

        NonceClaim nonceClaim = new NonceClaim();
        nonceClaim.setNonce("nonce");

        String jwt = compactBuilder.claims(nonceClaim).build().toString();

        NonceType nonceType = new NonceType();
        nonceType.setName("welcome");

        Nonce nonce = new Nonce();
        nonce.setId(UUID.randomUUID());
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);
        nonce.setNonceType(nonceType);

        when(mockHashTextStaticSalt.run("nonce")).thenReturn("hashedNonce");
        when(mockNonceRepository.getByTypeAndNonce(NonceName.WELCOME, "hashedNonce")).thenReturn(nonce);

        subject.markEmailVerified(jwt);

        verify(mockResourceOwnerRepository).setEmailVerified(nonce.getResourceOwner().getId());
        verify(mockNonceRepository).setSpent(nonce.getId());
        verify(mockNonceRepository).revokeUnSpent(nonce.getNonceType().getName(), nonce.getResourceOwner().getId());
    }

    @Test
    public void markEmailVerifiedWhenMangledNonceShouldThrowBadRequestExceptionException() throws Exception {

        BadRequestException actual = null;
        try {
            subject.markEmailVerified("notAJwt");
        } catch(BadRequestException e) {
            actual = e;
        }
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getCause(), instanceOf(InvalidJWT.class));

        verify(mockResourceOwnerRepository, never()).setEmailVerified(any(UUID.class));
        verify(mockNonceRepository, never()).setSpent(any(UUID.class));
    }

    @Test
    public void markEmailVerifiedWhenNonceNotFoundShouldThrowNotFoundExceptionException() throws Exception {
        // make a jwt for the test.
        UnsecureCompactBuilder compactBuilder = new UnsecureCompactBuilder();

        NonceClaim nonceClaim = new NonceClaim();
        nonceClaim.setNonce("nonce");

        String jwt = compactBuilder.claims(nonceClaim).build().toString();


        when(mockHashTextStaticSalt.run("nonce")).thenReturn("hashedNonce");
        when(mockNonceRepository.getByTypeAndNonce(NonceName.WELCOME, "hashedNonce")).thenThrow(RecordNotFoundException.class);

        NotFoundException actual = null;
        try {
            subject.markEmailVerified(jwt);
        } catch(NotFoundException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getCause(), instanceOf(RecordNotFoundException.class));

        verify(mockResourceOwnerRepository, never()).setEmailVerified(any(UUID.class));
        verify(mockNonceRepository, never()).setSpent(any(UUID.class));
    }
}