package net.tokensmith.authorization.openId.grant.redirect.code.authorization.request;

import org.apache.commons.validator.routines.UrlValidator;
import net.tokensmith.authorization.openId.grant.redirect.code.authorization.request.context.GetOpenIdConfidentialClientRedirectUri;
import net.tokensmith.authorization.openId.grant.redirect.code.authorization.request.entity.OpenIdAuthRequest;
import net.tokensmith.authorization.openId.grant.redirect.shared.authorization.request.ValidateOpenIdRequest;

import net.tokensmith.authorization.parse.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Created by tommackenzie on 10/10/15.
 */
@Component
public class ValidateOpenIdCodeResponseType extends ValidateOpenIdRequest<OpenIdAuthRequest> {

    @Autowired
    public ValidateOpenIdCodeResponseType(Parser<OpenIdAuthRequest> parser, UrlValidator urlValidator, GetOpenIdConfidentialClientRedirectUri getOpenIdConfidentialClientRedirectUri, CompareConfidentialClientToOpenIdAuthRequest compareConfidentialClientToOpenIdAuthRequest) {
        super(parser, OpenIdAuthRequest.class, urlValidator, getOpenIdConfidentialClientRedirectUri, compareConfidentialClientToOpenIdAuthRequest);
    }
}
