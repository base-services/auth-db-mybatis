package org.rootservices.authorization.persistence.repository;

import org.rootservices.authorization.persistence.entity.Client;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;

import java.util.UUID;

/**
 * Created by tommackenzie on 11/16/14.
 */
public interface ClientRepository {
    Client getById(UUID id) throws RecordNotFoundException;
    void insert(Client client);
}
