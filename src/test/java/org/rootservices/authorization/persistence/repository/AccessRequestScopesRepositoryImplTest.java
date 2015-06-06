package org.rootservices.authorization.persistence.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.rootservices.authorization.persistence.entity.AccessRequestScope;
import org.rootservices.authorization.persistence.mapper.AccessRequestScopesMapper;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by tommackenzie on 5/19/15.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessRequestScopesRepositoryImplTest {

    @Mock
    private AccessRequestScopesMapper mockAccessRequestScopesMapper;

    private AccessRequestScopesRepository subject;

    @Before
    public void setUp() {
        subject = new AccessRequestScopesRepositoryImpl(mockAccessRequestScopesMapper);
    }
    @Test
    public void testInsert() throws Exception {
        AccessRequestScope accessRequestScope = new AccessRequestScope(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );

        subject.insert(accessRequestScope);
        verify(mockAccessRequestScopesMapper, times(1)).insert(accessRequestScope);
    }
}