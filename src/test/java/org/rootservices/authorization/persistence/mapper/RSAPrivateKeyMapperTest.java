package org.rootservices.authorization.persistence.mapper;

import helper.fixture.FixtureFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rootservices.authorization.persistence.entity.RSAPrivateKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

/**
 * Created by tommackenzie on 2/15/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value={"classpath:spring-auth-test.xml"})
@Transactional
public class RSAPrivateKeyMapperTest {

    @Autowired
    private RSAPrivateKeyMapper subject;

    @Test
    public void insert() {
        RSAPrivateKey rsaPrivateKey = FixtureFactory.makeRSAPrivateKey();
        subject.insert(rsaPrivateKey);
    }

    @Test
    public void getMostRecentAndActiveForSigningShouldFindRecord() {
        RSAPrivateKey rsaPrivateKeyA = FixtureFactory.makeRSAPrivateKey();
        subject.insert(rsaPrivateKeyA);

        RSAPrivateKey rsaPrivateKeyB = FixtureFactory.makeRSAPrivateKey();
        subject.insert(rsaPrivateKeyB);

        RSAPrivateKey actual = subject.getMostRecentAndActiveForSigning();

        assertThat(actual, is(notNullValue()));
        assertThat(actual.getUuid(), is(rsaPrivateKeyB.getUuid()));
        assertThat(actual.getUse(), is(rsaPrivateKeyB.getUse()));

        assertThat(actual.getModulus(), is(rsaPrivateKeyB.getModulus()));
        assertThat(actual.getPublicExponent(), is(rsaPrivateKeyB.getPublicExponent()));
        assertThat(actual.getPrivateExponent(), is(rsaPrivateKeyB.getPrivateExponent()));
        assertThat(actual.getPrimeP(), is(rsaPrivateKeyB.getPrimeP()));
        assertThat(actual.getPrimeQ(), is(rsaPrivateKeyB.getPrimeQ()));
        assertThat(actual.getPrimeExponentP(), is(rsaPrivateKeyB.getPrimeExponentP()));
        assertThat(actual.getPrimeExponentQ(), is(rsaPrivateKeyB.getPrimeExponentQ()));
        assertThat(actual.getCrtCoefficient(), is(rsaPrivateKeyB.getCrtCoefficient()));

        assertThat(actual.isActive(), is(true));
        assertThat(actual.getCreatedAt(), is(notNullValue()));
        assertThat(actual.getUpdatedAt(), is(notNullValue()));
    }
}