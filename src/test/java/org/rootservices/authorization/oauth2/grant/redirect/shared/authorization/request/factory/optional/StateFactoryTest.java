package org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.factory.optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rootservices.authorization.constant.ErrorCode;
import org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.factory.exception.StateException;
import org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.factory.validator.OptionalParam;
import org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.factory.validator.exception.EmptyValueError;
import org.rootservices.authorization.oauth2.grant.redirect.shared.authorization.request.factory.validator.exception.MoreThanOneItemError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by tommackenzie on 2/1/15.
 */
public class StateFactoryTest {

    @Mock
    private OptionalParam mockOptionalParam;

    private StateFactory subject;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new StateFactory(mockOptionalParam);
    }

    @Test
    public void testMakeState() throws MoreThanOneItemError, EmptyValueError, StateException {
        String expectedValue = "state";
        Optional<String> expected = Optional.ofNullable(expectedValue);

        List<String> items = new ArrayList<>();
        items.add(expectedValue);

        when(mockOptionalParam.run(items)).thenReturn(true);
        Optional<String> actual = subject.makeState(items);
        assertThat(actual, is(expected));
    }

    @Test
    public void testMakeStateWhenStatesAreNull() throws MoreThanOneItemError, EmptyValueError, StateException {
        Optional<String> expected = Optional.ofNullable(null);

        List<String> items = null;

        when(mockOptionalParam.run(items)).thenReturn(true);
        Optional<String> actual = subject.makeState(items);
        assertThat(actual, is(expected));
    }

    @Test
    public void testMakeStateEmptyList() throws MoreThanOneItemError, EmptyValueError, StateException {
        Optional<String> expected = Optional.empty();

        List<String> items = new ArrayList<>();

        when(mockOptionalParam.run(items)).thenReturn(true);
        Optional<String> actual = subject.makeState(items);
        assertThat(actual, is(expected));
    }

    @Test
    public void testMakeScopesEmptyValueError() throws MoreThanOneItemError, EmptyValueError {

        List<String> items = new ArrayList<>();
        items.add("");

        when(mockOptionalParam.run(items)).thenThrow(EmptyValueError.class);

        try {
            subject.makeState(items);
            fail("StateException was expected.");
        } catch (StateException e) {
            assertThat(e.getCause(), instanceOf(EmptyValueError.class));
            assertThat(e.getCode(), is(ErrorCode.STATE_EMPTY_VALUE.getCode()));
        }
    }

    @Test
    public void testMakeScopesMoreThanOneItemError() throws MoreThanOneItemError, EmptyValueError {

        List<String> items = new ArrayList<>();
        items.add("Scope1");
        items.add("Scope2");

        when(mockOptionalParam.run(items)).thenThrow(MoreThanOneItemError.class);

        try {
            subject.makeState(items);
            fail("StateException was expected.");
        } catch (StateException e) {
            assertThat(e.getCause(), instanceOf(MoreThanOneItemError.class));;
            assertThat(e.getCode(), is(ErrorCode.STATE_MORE_THAN_ONE_ITEM.getCode()));
        }
    }
}
