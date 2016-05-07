package helper.fixture.persistence;

import helper.fixture.FixtureFactory;
import org.rootservices.authorization.persistence.entity.Client;
import org.rootservices.authorization.persistence.repository.ClientRepository;
import org.rootservices.authorization.persistence.repository.ClientScopesRepository;
import org.rootservices.authorization.persistence.repository.ScopeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

/**
 * Created by tommackenzie on 5/16/15.
 *
 * sets up database with a client that has scopes and the response type, code.
 *
 * Client        ClientScopes            Scope
 * +uuid   --->  +client_uuid     /--->  +uuid
 *               +scope_uuid  ---/
 */
@Component
public class LoadCodeClientWithScopes extends LoadClientWithScopes {

    @Autowired
    public LoadCodeClientWithScopes(ClientRepository clientRepository, ScopeRepository scopeRepository, ClientScopesRepository clientScopesRepository) {
        super(clientRepository, scopeRepository, clientScopesRepository);
    }

    protected Client makeClientWithScopes() throws URISyntaxException {
        return FixtureFactory.makeCodeClientWithScopes();
    }
}