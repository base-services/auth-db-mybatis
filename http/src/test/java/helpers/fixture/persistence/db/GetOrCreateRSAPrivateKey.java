package helpers.fixture.persistence.db;

import net.tokensmith.authorization.security.GenerateRSAPrivateKey;
import net.tokensmith.repository.entity.RSAPrivateKey;
import net.tokensmith.repository.exceptions.RecordNotFoundException;
import net.tokensmith.repository.repo.RsaPrivateKeyRepository;

/**
 * Created by tommackenzie on 2/21/16.
 */

public class GetOrCreateRSAPrivateKey {
    private GenerateRSAPrivateKey generateRSAPrivateKey;
    private RsaPrivateKeyRepository rsaPrivateKeyRepository;

    public GetOrCreateRSAPrivateKey(GenerateRSAPrivateKey generateRSAPrivateKey, RsaPrivateKeyRepository rsaPrivateKeyRepository) {
        this.generateRSAPrivateKey = generateRSAPrivateKey;
        this.rsaPrivateKeyRepository = rsaPrivateKeyRepository;
    }

    public RSAPrivateKey run(int keySize) {
        RSAPrivateKey key;
        try {
            key = rsaPrivateKeyRepository.getMostRecentAndActiveForSigning();
        } catch (RecordNotFoundException e) {
            key = generateRSAPrivateKey.generate(keySize);
            key.setActive(true);
            rsaPrivateKeyRepository.insert(key);
        }

        return key;
    }
}
