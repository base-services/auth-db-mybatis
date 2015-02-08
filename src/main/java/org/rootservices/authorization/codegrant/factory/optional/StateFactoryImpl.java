package org.rootservices.authorization.codegrant.factory.optional;

import org.rootservices.authorization.codegrant.constant.ErrorCode;
import org.rootservices.authorization.codegrant.factory.exception.StateException;
import org.rootservices.authorization.codegrant.validator.OptionalParam;
import org.rootservices.authorization.codegrant.validator.exception.EmptyValueError;
import org.rootservices.authorization.codegrant.validator.exception.MoreThanOneItemError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Created by tommackenzie on 1/31/15.
 */
@Component
public class StateFactoryImpl implements StateFactory {

    @Autowired
    OptionalParam optionalParam;

    public StateFactoryImpl() {}

    public StateFactoryImpl(OptionalParam optionalParam) {
        this.optionalParam = optionalParam;
    }

    public Optional<String> makeState(List<String> states) throws StateException {
        try {
            optionalParam.run(states);
        } catch (EmptyValueError e) {
            throw new StateException(ErrorCode.EMPTY_VALUE, e);
        } catch (MoreThanOneItemError e) {
            throw new StateException(ErrorCode.MORE_THAN_ONE_ITEM, e);
        }

        Optional<String> state;
        if( states == null || states.isEmpty()) {
            state = Optional.ofNullable(null);
        }else {
            state = Optional.ofNullable(states.get(0));
        }

        return state;

    }
}
