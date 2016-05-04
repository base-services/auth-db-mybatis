package org.rootservices.authorization.grant.openid.protocol.token.factory;

import org.rootservices.authorization.grant.openid.protocol.token.response.entity.Address;
import org.rootservices.authorization.grant.openid.protocol.token.response.entity.IdToken;
import org.rootservices.authorization.grant.openid.protocol.token.translator.AddrToAddrClaims;
import org.rootservices.authorization.grant.openid.protocol.token.translator.ProfileToIdToken;
import org.rootservices.authorization.persistence.entity.AccessRequestScope;
import org.rootservices.authorization.persistence.entity.Profile;
import org.rootservices.authorization.persistence.entity.TokenScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Created by tommackenzie on 3/19/16.
 */
@Component
public class IdTokenFactoryImpl implements IdTokenFactory {
    private static String PROFILE = "profile";
    private static String EMAIL = "email";
    private static String ADDR = "address";
    private static String PHONE = "phone";

    private ProfileToIdToken profileToIdToken;
    private AddrToAddrClaims addrToAddrClaims;

    @Autowired
    public IdTokenFactoryImpl(ProfileToIdToken profileToIdToken, AddrToAddrClaims addrToAddrClaims) {
        this.profileToIdToken = profileToIdToken;
        this.addrToAddrClaims = addrToAddrClaims;
    }

    @Override
    public IdToken make(List<TokenScope> tokenScopes, Profile profile) {
        IdToken idToken = new IdToken();


        if (hasScope(tokenScopes, PROFILE)) {
            profileToIdToken.toProfileClaims(idToken, profile);
        }

        if (hasScope(tokenScopes, EMAIL)) {
            profileToIdToken.toEmailClaims(idToken, profile.getResourceOwner().getEmail(), profile.getResourceOwner().isEmailVerified());
        }

        if (hasScope(tokenScopes, PHONE)) {
            profileToIdToken.toPhoneClaims(
                idToken,
                profile.getPhoneNumber(),
                profile.isPhoneNumberVerified()
            );
        }

        if (hasScope(tokenScopes, ADDR) && profile.getAddresses().size() > 0) {
            Address address = addrToAddrClaims.to(profile.getAddresses().get(0));
            idToken.setAddress(Optional.of(address));
        } else {
            idToken.setAddress(Optional.empty());
        }

        return idToken;
    }

    protected Boolean hasScope(List<TokenScope> tokenScopes, String scope) {
        for(TokenScope ts: tokenScopes){
            if (scope.equals(ts.getScope().getName())) {
                return true;
            }
        }
        return false;
    }
}
