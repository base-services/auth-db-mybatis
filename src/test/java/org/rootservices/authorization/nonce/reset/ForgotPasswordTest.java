package org.rootservices.authorization.nonce.reset;

import helper.fixture.FixtureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rootservices.authorization.exception.BadRequestException;
import org.rootservices.authorization.exception.NotFoundException;
import org.rootservices.authorization.nonce.InsertNonce;
import org.rootservices.authorization.nonce.SpendNonce;
import org.rootservices.authorization.nonce.entity.NonceName;
import org.rootservices.authorization.persistence.entity.Nonce;
import org.rootservices.authorization.persistence.entity.ResourceOwner;
import org.rootservices.authorization.persistence.repository.RefreshTokenRepository;
import org.rootservices.authorization.persistence.repository.ResourceOwnerRepository;
import org.rootservices.authorization.persistence.repository.TokenRepository;
import org.rootservices.authorization.register.exception.NonceException;
import org.rootservices.authorization.security.ciphers.HashTextRandomSalt;
import org.rootservices.pelican.Publish;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ForgotPasswordTest {
    @Mock
    private InsertNonce mockInsertNonce;
    @Mock
    private Publish mockPublish;
    private String issuer;
    @Mock
    private SpendNonce mockSpendNonce;
    @Mock
    private HashTextRandomSalt mockHashTextRandomSalt;
    @Mock
    private ResourceOwnerRepository mockResourceOwnerRepository;
    @Mock
    private TokenRepository mockTokenRepository;
    @Mock
    private RefreshTokenRepository mockRefreshTokenRepository;
    private ForgotPassword subject;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.issuer = "sso.rootservices.org";
        subject = new ForgotPassword(mockInsertNonce, mockPublish, issuer, mockSpendNonce, mockHashTextRandomSalt, mockResourceOwnerRepository, mockTokenRepository, mockRefreshTokenRepository);
    }

    @Test
    public void sendMessageShouldBeOk() throws Exception {
        String email = "obi-wan@rootservices.org";
        String plainTextNonce = "plainTextNonce";

        when(mockInsertNonce.insert(email, NonceName.RESET_PASSWORD)).thenReturn(plainTextNonce);

        subject.sendMessage(email);

        ArgumentCaptor<Map> messageCaptor = ArgumentCaptor.forClass(HashMap.class);
        verify(mockPublish).send(eq("mailer"), messageCaptor.capture());

        assertThat(messageCaptor.getValue().size(), is(4));
        assertThat(messageCaptor.getValue().get("type"), is("forgot_password"));
        assertThat(messageCaptor.getValue().get("recipient"), is(email));
        assertThat(messageCaptor.getValue().get("base_link"), is(issuer + "/update-password?nonce="));
        assertThat(messageCaptor.getValue().get("nonce"), is(plainTextNonce));
    }

    @Test
    public void sendMessageWhenEmailEmptyShouldThrowBadRequestException() throws Exception {
        String email = "";

        BadRequestException actual = null;
        try {
            subject.sendMessage(email);
        } catch (BadRequestException e) {
            actual = e;
        }
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("email"));
        assertThat(actual.getDescription(), is("Email is required"));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void sendMessageWhenEmailWhiteSpaceShouldThrowBadRequestException() throws Exception {
        String email = " ";

        BadRequestException actual = null;
        try {
            subject.sendMessage(email);
        } catch (BadRequestException e) {
            actual = e;
        }
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("email"));
        assertThat(actual.getDescription(), is("Email is required"));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void sendMessageWhenEmailNullShouldThrowBadRequestException() throws Exception {
        String email = null;

        BadRequestException actual = null;
        try {
            subject.sendMessage(email);
        } catch (BadRequestException e) {
            actual = e;
        }
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("email"));
        assertThat(actual.getDescription(), is("Email is required"));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void sendMessageShouldThrowNonceException() throws Exception {
        String email = "obi-wan@rootservices.org";

        when(mockInsertNonce.insert(email, NonceName.RESET_PASSWORD)).thenThrow(NonceException.class);

        NonceException actual = null;
        try {
            subject.sendMessage(email);
        } catch (NonceException e) {
            actual = e;
        }
        assertThat(actual, is(notNullValue()));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetShouldBeOk() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = "plainTextPassword";
        String repeatPassword = "plainTextPassword";
        String hashedPassword = "hashedPassword";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);
        when(mockHashTextRandomSalt.run(password)).thenReturn(hashedPassword);

        subject.reset(jwt, password, repeatPassword);

        verify(mockResourceOwnerRepository).updatePassword(nonce.getResourceOwner().getId(), hashedPassword.getBytes());
        verify(mockTokenRepository).revokeActive(nonce.getResourceOwner().getId());
        verify(mockRefreshTokenRepository).revokeActive(nonce.getResourceOwner().getId());

        ArgumentCaptor<Map> messageCaptor = ArgumentCaptor.forClass(HashMap.class);
        verify(mockPublish).send(eq("mailer"), messageCaptor.capture());

        assertThat(messageCaptor.getValue().size(), is(2));
        assertThat(messageCaptor.getValue().get("type"), is("password_was_reset"));
        assertThat(messageCaptor.getValue().get("recipient"), is(nonce.getResourceOwner().getEmail()));
    }

    @Test
    public void resetWhenJwtEmptyShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "";
        String password = "plainTextPassword";
        String repeatPassword = "plainTextPassword";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("nonce"));
        assertThat(actual.getDescription(), is("Nonce is required"));

        verify(mockSpendNonce, never()).spend(jwt, NonceName.RESET_PASSWORD);
        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetWhenJwtNullShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = null;
        String password = "plainTextPassword";
        String repeatPassword = "plainTextPassword";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("nonce"));
        assertThat(actual.getDescription(), is("Nonce is required"));

        verify(mockSpendNonce, never()).spend(jwt, NonceName.RESET_PASSWORD);
        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetWhenBadNonceShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = "plainTextPassword";
        String repeatPassword = "plainTextPassword";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenThrow(BadRequestException.class);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));

        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetWhenPasswordEmptyShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = "";
        String repeatPassword = "plainTextPassword";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("password"));
        assertThat(actual.getDescription(), is("Password is required"));

        verify(mockSpendNonce, never()).spend(jwt, NonceName.RESET_PASSWORD);
        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetWhenPasswordNullShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = null;
        String repeatPassword = "plainTextPassword";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("password"));
        assertThat(actual.getDescription(), is("Password is required"));

        verify(mockSpendNonce, never()).spend(jwt, NonceName.RESET_PASSWORD);
        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetWhenRepeatPasswordEmptyShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = "plainTextPassword";
        String repeatPassword = "";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("repeatPassword"));
        assertThat(actual.getDescription(), is("Repeat Password is required"));

        verify(mockSpendNonce, never()).spend(jwt, NonceName.RESET_PASSWORD);
        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetWhenRepeatPasswordNullShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = "plainTextPassword";
        String repeatPassword = null;

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("repeatPassword"));
        assertThat(actual.getDescription(), is("Repeat Password is required"));

        verify(mockSpendNonce, never()).spend(jwt, NonceName.RESET_PASSWORD);
        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

    @Test
    public void resetWhenPasswordsDontMatchShouldThrowBadRequestException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = "password1";
        String repeatPassword = "password2";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenReturn(nonce);

        BadRequestException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (BadRequestException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getField(), is("password"));
        assertThat(actual.getDescription(), is("Passwords do not match"));

        verify(mockSpendNonce, never()).spend(jwt, NonceName.RESET_PASSWORD);
        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }


    @Test
    public void resetShouldThrowNotFoundException() throws Exception {
        Nonce nonce = new Nonce();
        ResourceOwner ro = FixtureFactory.makeResourceOwner();
        nonce.setResourceOwner(ro);

        String jwt = "some.jwt";
        String password = "plainTextPassword";
        String repeatPassword = "plainTextPassword";

        when(mockSpendNonce.spend(jwt, NonceName.RESET_PASSWORD)).thenThrow(NotFoundException.class);

        NotFoundException actual = null;
        try {
            subject.reset(jwt, password, repeatPassword);
        } catch (NotFoundException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));

        verify(mockHashTextRandomSalt, never()).run(password);
        verify(mockResourceOwnerRepository, never()).updatePassword(any(UUID.class), any(byte[].class));
        verify(mockTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockRefreshTokenRepository, never()).revokeActive(any(UUID.class));
        verify(mockPublish, never()).send(eq("mailer"), any(HashMap.class));
    }

}