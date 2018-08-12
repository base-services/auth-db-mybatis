package org.rootservices.authorization.openId.identity;

import org.rootservices.authorization.oauth2.grant.token.entity.TokenClaims;
import org.rootservices.authorization.openId.identity.entity.IdToken;
import org.rootservices.authorization.openId.identity.exception.IdTokenException;
import org.rootservices.authorization.openId.identity.exception.KeyNotFoundException;
import org.rootservices.authorization.openId.identity.exception.ProfileNotFoundException;
import org.rootservices.authorization.openId.identity.factory.IdTokenFactory;
import org.rootservices.authorization.openId.identity.translator.PrivateKeyTranslator;
import org.rootservices.authorization.persistence.entity.*;
import org.rootservices.authorization.persistence.exceptions.RecordNotFoundException;
import org.rootservices.authorization.persistence.repository.ProfileRepository;
import org.rootservices.authorization.persistence.repository.RsaPrivateKeyRepository;
import org.rootservices.jwt.builder.compact.SecureCompactBuilder;
import org.rootservices.jwt.builder.exception.CompactException;
import org.rootservices.jwt.config.JwtAppFactory;
import org.rootservices.jwt.entity.jwk.RSAKeyPair;
import org.rootservices.jwt.entity.jwt.header.Algorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by tommackenzie on 8/31/16.
 */
@Component
public class MakeImplicitIdentityToken {
    private static String PROFILE_ERROR_MESSAGE = "Profile was not found";
    private static String KEY_ERROR_MESSAGE = "No key available to sign id token";
    private static String ID_TOKEN_ERROR_MSG = "Could not create id token";


    private ProfileRepository profileRepository;
    private MakeAccessTokenHash makeAccessTokenHash;
    private IdTokenFactory idTokenFactory;
    private RsaPrivateKeyRepository rsaPrivateKeyRepository;
    private PrivateKeyTranslator privateKeyTranslator;
    private JwtAppFactory jwtAppFactory;

    @Autowired
    public MakeImplicitIdentityToken(ProfileRepository profileRepository, MakeAccessTokenHash makeAccessTokenHash, IdTokenFactory idTokenFactory, RsaPrivateKeyRepository rsaPrivateKeyRepository, PrivateKeyTranslator privateKeyTranslator, JwtAppFactory jwtAppFactory) {
        this.profileRepository = profileRepository;
        this.makeAccessTokenHash = makeAccessTokenHash;
        this.idTokenFactory = idTokenFactory;
        this.rsaPrivateKeyRepository = rsaPrivateKeyRepository;
        this.privateKeyTranslator = privateKeyTranslator;
        this.jwtAppFactory = jwtAppFactory;
    }

    /**
     * Creates a id token for the implicit grant flow, "token id_token".
     * http://openid.net/specs/openid-connect-core-1_0.html#ImplicitFlowAuth
     */
    public String makeForAccessToken(String plainTextAccessToken, String nonce, TokenClaims tokenClaims, ResourceOwner resourceOwner, List<String> scopesForIdToken) throws ProfileNotFoundException, KeyNotFoundException, IdTokenException {

        Profile profile = null;
        try {
            profile = profileRepository.getByResourceOwnerId(resourceOwner.getId());
        } catch (RecordNotFoundException e) {
            throw new ProfileNotFoundException(PROFILE_ERROR_MESSAGE, e);
        }
        resourceOwner.setProfile(profile);

        String accessTokenHash = makeAccessTokenHash.makeEncodedHash(plainTextAccessToken);
        IdToken idToken = idTokenFactory.make(accessTokenHash, nonce, tokenClaims, scopesForIdToken, resourceOwner);

        RSAPrivateKey key = null;
        try {
            key = rsaPrivateKeyRepository.getMostRecentAndActiveForSigning();
        } catch (RecordNotFoundException e) {
            throw new KeyNotFoundException(KEY_ERROR_MESSAGE, e);
        }

        RSAKeyPair rsaKeyPair = privateKeyTranslator.from(key);
        String encodedJwt = translateIdTokenToEncodedJwt(rsaKeyPair, idToken);

        return encodedJwt;
    }

    /**
     * Creates a id token for the implicit grant flow, "id_token".
     * http://openid.net/specs/openid-connect-core-1_0.html#ImplicitFlowAuth
     */
    public String makeIdentityOnly(String nonce, TokenClaims tokenClaim, ResourceOwner resourceOwner, List<String> scopes) throws ProfileNotFoundException, KeyNotFoundException, IdTokenException {

        Profile profile = null;
        try {
            profile = profileRepository.getByResourceOwnerId(resourceOwner.getId());
        } catch (RecordNotFoundException e) {
            throw new ProfileNotFoundException(PROFILE_ERROR_MESSAGE, e);
        }
        resourceOwner.setProfile(profile);
        IdToken idToken = idTokenFactory.make(nonce, tokenClaim, scopes, resourceOwner);

        RSAPrivateKey key = null;
        try {
            key = rsaPrivateKeyRepository.getMostRecentAndActiveForSigning();
        } catch (RecordNotFoundException e) {
            throw new KeyNotFoundException(KEY_ERROR_MESSAGE, e);
        }

        RSAKeyPair rsaKeyPair = privateKeyTranslator.from(key);
        String encodedJwt = translateIdTokenToEncodedJwt(rsaKeyPair, idToken);

        return encodedJwt;
    }

    protected String translateIdTokenToEncodedJwt(RSAKeyPair rsaKeyPair, IdToken idToken) throws IdTokenException {

        String encodedJwt;
        SecureCompactBuilder compactBuilder = new SecureCompactBuilder();
        try {
            encodedJwt = compactBuilder.alg(Algorithm.RS256)
                    .key(rsaKeyPair)
                    .claims(idToken)
                    .build().toString();
        } catch (CompactException e) {
            throw new IdTokenException(ID_TOKEN_ERROR_MSG, e);
        }

        return encodedJwt;
    }
}
