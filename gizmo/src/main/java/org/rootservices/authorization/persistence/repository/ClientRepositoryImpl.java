package org.rootservices.authorization.persistence.repository;

import org.rootservices.authorization.persistence.entity.Client;
import org.rootservices.authorization.persistence.entity.Scope;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.mapper.ClientMapper;
import org.rootservices.authorization.persistence.mapper.ScopeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Created by tommackenzie on 11/16/14.
 */
@Component
public class ClientRepositoryImpl implements ClientRepository {

    @Autowired
    private ClientMapper clientMapper;

    public ClientRepositoryImpl() {}

    public ClientRepositoryImpl(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

    @Override
    public Client getById(UUID uuid) throws RecordNotFoundException {
        Client client = clientMapper.getById(uuid);
        if (client != null) {
            return client;
        }

        throw new RecordNotFoundException("Client: " + uuid.toString());
    }

    @Override
    public void insert(Client client) {
        clientMapper.insert(client);
    }
}