package org.rootservices.authorization.persistence.mapper;

import org.rootservices.authorization.persistence.entity.ResourceOwner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by tommackenzie on 9/25/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(value={"classpath:spring-auth-test.xml"})
public class ResourceOwnerMapperTest {

    @Autowired
    ResourceOwnerMapper subject;

    public ResourceOwner insertAuthUser() {
        UUID uuid = UUID.randomUUID();
        byte [] password = "plainTextPassword".getBytes();
        ResourceOwner user = new ResourceOwner(uuid, "test@rootservices.com", password);

        subject.insert(user);
        return user;
    }

    @Test
    @Transactional
    public void insert() {
        UUID uuid = UUID.randomUUID();
        byte [] password = "plainTextPassword".getBytes();
        ResourceOwner user = new ResourceOwner(uuid, "test@rootservices.com", password);
        subject.insert(user);
    }

    @Test
    @Transactional
    public void getByUUID() {
        ResourceOwner expectedUser = insertAuthUser();
        ResourceOwner actualUser = subject.getByUUID(expectedUser.getUuid());

        assertThat(actualUser.getUuid()).isEqualTo(expectedUser.getUuid());
        assertThat(actualUser.getEmail()).isEqualTo(expectedUser.getEmail());
        assertThat(actualUser.getPassword()).isEqualTo(expectedUser.getPassword());
        assertThat(actualUser.getCreatedAt()).isNotNull();
        assertThat(actualUser.getCreatedAt()).isInstanceOf(Date.class);
    }

    @Test
    @Transactional
    public void getByUUIDAuthUserNotFound() {

        ResourceOwner actualUser = subject.getByUUID(UUID.randomUUID());

        assertThat(actualUser).isEqualTo(null);
    }

    @Test
    @Transactional
    public void getByEmailAndPassword() {

        ResourceOwner expectedUser = insertAuthUser();
        ResourceOwner actualUser = subject.getByEmailAndPassword(expectedUser.getEmail(), expectedUser.getPassword());

        assertThat(actualUser.getUuid()).isEqualTo(expectedUser.getUuid());
        assertThat(actualUser.getEmail()).isEqualTo(expectedUser.getEmail());
        assertThat(actualUser.getPassword()).isEqualTo(expectedUser.getPassword());
        assertThat(actualUser.getCreatedAt()).isNotNull();
        assertThat(actualUser.getCreatedAt()).isInstanceOf(Date.class);

    }

}