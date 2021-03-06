package net.tokensmith.authorization.oauth2.grant.redirect.code.token.factory;

import net.tokensmith.authorization.constant.ErrorCode;
import net.tokensmith.authorization.oauth2.grant.redirect.code.token.entity.TokenInputCodeGrant;
import net.tokensmith.authorization.oauth2.grant.token.exception.InvalidValueException;
import net.tokensmith.authorization.oauth2.grant.token.exception.UnknownKeyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by tommackenzie on 9/27/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-auth-test.xml")
public class TokenInputCodeGrantFactoryTest {
    @Autowired
    private TokenInputCodeGrantFactory subject;

    @Test
    public void runShouldBeOk() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("grant_type", "authorization_code");
        request.put("code", "some-code");
        request.put("redirect_uri", "https://tokensmith.net");

        TokenInputCodeGrant actual = subject.run(request);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getCode(), is("some-code"));
        assertThat(actual.getRedirectUri().isPresent(), is(true));
        assertThat(actual.getRedirectUri().get(), is(new URI("https://tokensmith.net")));
    }

    @Test
    public void runWhenRequiredAndOptionalAndUnknownKeyShouldThrowUnknownKeyException() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("grant_type", "authorization_code");
        request.put("code", "some-code");
        request.put("redirect_uri", "https://tokensmith.net");
        request.put("foo", "banana");

        UnknownKeyException actual = null;
        try {
            subject.run(request);
        } catch (UnknownKeyException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getMessage(), is(ErrorCode.UNKNOWN_KEY.getDescription()));
        assertThat(actual.getCode(), is(ErrorCode.UNKNOWN_KEY.getCode()));
        assertThat(actual.getKey(), is("foo"));
    }

    @Test
    public void runWhenRequiredAndUnknownKeyShouldThrowUnknownKeyException() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("grant_type", "authorization_code");
        request.put("code", "some-code");
        request.put("foo", "banana");

        UnknownKeyException actual = null;
        try {
            subject.run(request);
        } catch (UnknownKeyException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getMessage(), is(ErrorCode.UNKNOWN_KEY.getDescription()));
        assertThat(actual.getCode(), is(ErrorCode.UNKNOWN_KEY.getCode()));
        assertThat(actual.getKey(), is("foo"));
    }

    @Test
    public void runWhenRedirectUriMissingShouldBeOk() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("grant_type", "authorization_code");
        request.put("code", "some-code");

        TokenInputCodeGrant actual = subject.run(request);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getCode(), is("some-code"));
        assertThat(actual.getRedirectUri().isPresent(), is(false));
    }

    @Test
    public void runWhenRedirectUriIsEmptyValueShouldThrowInvalidValueException() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("grant_type", "authorization_code");
        request.put("code", "some-code");
        request.put("redirect_uri", "");

        InvalidValueException actual = null;
        try {
            subject.run(request);
        } catch (InvalidValueException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getMessage(), is(ErrorCode.EMPTY_VALUE.getDescription()));
        assertThat(actual.getCode(), is(ErrorCode.EMPTY_VALUE.getCode()));
        assertThat(actual.getKey(), is("redirect_uri"));
        assertThat(actual.getValue(), is(""));
    }

    @Test
    public void runWhenRedirectUriIsInvalidShouldThrowInvalidValueException() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("grant_type", "authorization_code");
        request.put("code", "some-code");
        request.put("redirect_uri", "foo");

        InvalidValueException actual = null;
        try {
            subject.run(request);
        } catch (InvalidValueException e) {
            actual = e;
        }

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getMessage(), is(ErrorCode.REDIRECT_URI_INVALID.getDescription()));
        assertThat(actual.getCode(), is(ErrorCode.REDIRECT_URI_INVALID.getCode()));
        assertThat(actual.getKey(), is("redirect_uri"));
        assertThat(actual.getValue(), is("foo"));
    }
}