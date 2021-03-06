package net.tokensmith.authorization.persistence.mapper;

import helper.fixture.FixtureFactory;
import helper.fixture.TestAppConfig;
import helper.fixture.persistence.LoadCodeClientWithScopes;
import net.tokensmith.repository.entity.Client;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.annotation.Transactional;

import java.net.URISyntaxException;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by tommackenzie on 11/15/14.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes= TestAppConfig.class, loader= AnnotationConfigContextLoader.class)
@Transactional
public class ClientMapperTest {

    @Autowired
    private LoadCodeClientWithScopes loadCodeClientWithScopes;
    @Autowired
    private ClientMapper subject;

    @Test
    public void insert() throws URISyntaxException {
        Client client = FixtureFactory.makeCodeClientWithScopes();
        subject.insert(client);
    }

    @Test
    public void getByUUID() throws URISyntaxException {
        Client expectedClient = loadCodeClientWithScopes.run();

        Client actual = subject.getById(expectedClient.getId());

        assertThat(actual.getId(), is(expectedClient.getId()));

        // respnse types
        assertThat(actual.getResponseTypes(), is(notNullValue()));
        assertThat(actual.getResponseTypes().size(), is(1));
        assertThat(actual.getResponseTypes().get(0).getId(), is(notNullValue()));
        assertThat(actual.getResponseTypes().get(0).getName(), is("CODE"));
        assertThat(actual.getResponseTypes().get(0).getCreatedAt(), is(notNullValue()));
        assertThat(actual.getResponseTypes().get(0).getUpdatedAt(), is(notNullValue()));

        assertThat(actual.getCreatedAt(), is(notNullValue()));
        assertThat(actual.getScopes().size(), is(1));
        assertThat(actual.getScopes().get(0).getId(), is(expectedClient.getScopes().get(0).getId()));
        assertThat(actual.getScopes().get(0).getName(), is("profile"));
    }

    @Test
    public void getByUUIDNotFound() {
        Client actual = subject.getById(UUID.randomUUID());

        assertThat(actual, is(nullValue()));
    }
}
