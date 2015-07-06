package org.rootservices.authorization.grant.code.protocol.authorization.request.factory.optional;

import org.rootservices.authorization.grant.code.protocol.authorization.request.factory.exception.ScopesException;

import java.util.List;

/**
 * Created by tommackenzie on 1/31/15.
 */
public interface ScopesFactory {
    public List<String> makeScopes(List<String> items) throws ScopesException;
}