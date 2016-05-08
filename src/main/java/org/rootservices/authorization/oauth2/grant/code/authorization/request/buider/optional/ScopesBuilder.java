package org.rootservices.authorization.oauth2.grant.code.authorization.request.buider.optional;

import org.rootservices.authorization.oauth2.grant.code.authorization.request.buider.exception.ScopesException;

import java.util.List;

/**
 * Created by tommackenzie on 1/31/15.
 */
public interface ScopesBuilder {
    List<String> makeScopes(List<String> items) throws ScopesException;
}