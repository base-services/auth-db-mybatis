package org.rootservices.authorization.openId.grant.redirect.code.authorization.request;

import org.rootservices.authorization.openId.grant.redirect.shared.authorization.request.CompareClientToOpenIdAuthRequest;
import org.rootservices.authorization.persistence.entity.Client;
import org.rootservices.authorization.persistence.entity.ConfidentialClient;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.repository.ConfidentialClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created by tommackenzie on 9/30/15.
 */
@Component
public class CompareConfidentialClientToOpenIdAuthRequest extends CompareClientToOpenIdAuthRequest {
    @Autowired
    private ConfidentialClientRepository confidentialClientRepository;

    public CompareConfidentialClientToOpenIdAuthRequest() {
    }

    public CompareConfidentialClientToOpenIdAuthRequest(ConfidentialClientRepository confidentialClientRepository) {
        this.confidentialClientRepository = confidentialClientRepository;
    }

    @Override
    protected Client getClient(UUID clientId) throws RecordNotFoundException {
        ConfidentialClient confidentialClient = confidentialClientRepository.getByClientId(clientId);
        return confidentialClient.getClient();
    }
}