package org.rootservices.authorization.codegrant.validator;

import org.junit.Before;
import org.junit.Test;
import org.rootservices.authorization.codegrant.validator.exception.EmptyValueError;
import org.rootservices.authorization.codegrant.validator.exception.MoreThanOneItemError;
import org.rootservices.authorization.codegrant.validator.exception.NoItemsError;
import org.rootservices.authorization.codegrant.validator.exception.ParamIsNullError;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by tommackenzie on 1/31/15.
 */
public class RequiredParamImplTest {

    private RequiredParam subject;

    @Before
    public void setUp() {
        subject = new RequiredParamImpl();
    }

    private List<String> makeItems() {
        List<String> items = new ArrayList<>();
        items.add("item1");
        return items;
    }

    @Test
    public void runIsOk() throws EmptyValueError, MoreThanOneItemError, NoItemsError, ParamIsNullError {
        List<String> items = makeItems();

        boolean actual = subject.run(items);
        assertThat(actual).isEqualTo(true);
    }

    @Test(expected=ParamIsNullError.class)
    public void runIsNull() throws EmptyValueError, MoreThanOneItemError, NoItemsError, ParamIsNullError {
        List<String> items = null;

        subject.run(items);
    }

    @Test(expected=NoItemsError.class)
    public void runHasNoItems() throws EmptyValueError, MoreThanOneItemError, NoItemsError, ParamIsNullError {
        List<String> items = new ArrayList<>();

        subject.run(items);
    }

    @Test(expected=EmptyValueError.class)
    public void runHasOneItemIsEmpty() throws EmptyValueError, MoreThanOneItemError, NoItemsError, ParamIsNullError {
        List<String> items = new ArrayList<>();
        items.add("");

        subject.run(items);
    }

    @Test(expected=MoreThanOneItemError.class)
    public void runHasTwoItems() throws EmptyValueError, MoreThanOneItemError, NoItemsError, ParamIsNullError {
        List<String> items = makeItems();
        items.add("item2");

        subject.run(items);
    }
}
