package org.rootservices.authorization.openId.jwk;

import helper.fixture.FixtureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rootservices.authorization.openId.jwk.exception.NotFoundException;
import org.rootservices.authorization.openId.jwk.translator.RSAPublicKeyTranslator;
import org.rootservices.authorization.persistence.entity.RSAPrivateKey;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.repository.RsaPrivateKeyRepository;
import org.rootservices.jwt.entity.jwk.KeyType;
import org.rootservices.jwt.entity.jwk.RSAPublicKey;
import org.rootservices.jwt.entity.jwk.Use;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by tommackenzie on 1/4/17.
 */
public class GetKeysTest {
    private GetKeys subject;
    @Mock
    private RsaPrivateKeyRepository mockRsaPrivateKeyRepository;
    @Mock
    private RSAPublicKeyTranslator mockRsaPublicKeyTranslator;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        subject = new GetKeys(mockRsaPublicKeyTranslator, mockRsaPrivateKeyRepository);
    }

    @Test
    public void getPublicKeyByIdShouldBeOk() throws Exception {
        UUID id = UUID.randomUUID();

        RSAPrivateKey rsaPrivateKey = FixtureFactory.makeRSAPrivateKey();
        when(mockRsaPrivateKeyRepository.getByIdActiveSign(id)).thenReturn(rsaPrivateKey);
        RSAPublicKey rsaPublicKey = new RSAPublicKey(Optional.of("test"), KeyType.RSA, Use.SIGNATURE, new BigInteger("1"), new BigInteger("1"));
        when(mockRsaPublicKeyTranslator.to(rsaPrivateKey)).thenReturn(rsaPublicKey);

        RSAPublicKey actual = subject.getPublicKeyById(id);

        assertThat(actual, is(notNullValue()));
        assertThat(actual, is(rsaPublicKey));
    }

    @Test(expected = NotFoundException.class)
    public void getPublicKeyByIdWhenRecordNotFoundThenThrowNotFoundException() throws Exception {
        UUID id = UUID.randomUUID();

        RecordNotFoundException rnfe = new RecordNotFoundException();
        when(mockRsaPrivateKeyRepository.getByIdActiveSign(id)).thenThrow(rnfe);

        subject.getPublicKeyById(id);
    }

    @Test
    public void getPublicKeysPage1ShouldBeOk() {
        List<RSAPrivateKey> rsaPrivateKeys = new ArrayList<>();
        for(int i = 0; i < 20; i++) {
            rsaPrivateKeys.add(FixtureFactory.makeRSAPrivateKey());
        }
        RSAPublicKey rsaPublicKey = new RSAPublicKey(Optional.of("test"), KeyType.RSA, Use.SIGNATURE, new BigInteger("1"), new BigInteger("1"));

        when(mockRsaPrivateKeyRepository.getWhereActiveAndUseIsSign(20, 0)).thenReturn(rsaPrivateKeys);
        when(mockRsaPublicKeyTranslator.to(any(RSAPrivateKey.class))).thenReturn(rsaPublicKey);

        List<RSAPublicKey> actual = subject.getPublicKeys(1);
        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(20));
    }

    @Test
    public void calculateOffsetWhenPage1ShouldBe0() {
        Integer actual = subject.calculateOffset(1);

        assertThat(actual, is(0));
    }

    @Test
    public void calculateOffsetWhenPage2ShouldBe20() {
        Integer actual = subject.calculateOffset(2);

        assertThat(actual, is(20));
    }

    @Test
    public void calculateOffsetWhenPage3ShouldBe40() {
        Integer actual = subject.calculateOffset(3);

        assertThat(actual, is(40));
    }
}