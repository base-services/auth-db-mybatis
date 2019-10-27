package net.tokensmith.authorization.oauth2.grant.password;

import net.tokensmith.authorization.exception.ServerException;
import net.tokensmith.authorization.oauth2.grant.token.builder.TokenResponseBuilder;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenGraph;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenResponse;
import net.tokensmith.authorization.oauth2.grant.token.entity.TokenType;
import net.tokensmith.authorization.persistence.entity.*;
import net.tokensmith.authorization.persistence.repository.*;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by tommackenzie on 9/18/16.
 */
@Component
public class IssueTokenPasswordGrant {
    private InsertTokenGraphPasswordGrant insertTokenGraphPasswordGrant;
    private ResourceOwnerTokenRepository resourceOwnerTokenRepository;
    private TokenResponseBuilder tokenResponseBuilder;

    private String issuer;

    public IssueTokenPasswordGrant(InsertTokenGraphPasswordGrant insertTokenGraphPasswordGrant, ResourceOwnerTokenRepository resourceOwnerTokenRepository, TokenResponseBuilder tokenResponseBuilder, String issuer) {
        this.insertTokenGraphPasswordGrant = insertTokenGraphPasswordGrant;
        this.resourceOwnerTokenRepository = resourceOwnerTokenRepository;
        this.tokenResponseBuilder = tokenResponseBuilder;
        this.issuer = issuer;
    }

    public TokenResponse run(UUID clientId, UUID resourceOwnerId, List<Scope> scopes, List<Client> audience) throws ServerException {
        TokenGraph tokenGraph = insertTokenGraphPasswordGrant.insertTokenGraph(clientId, scopes, audience);

        ResourceOwner resourceOwner = new ResourceOwner();
        resourceOwner.setId(resourceOwnerId);
        ResourceOwnerToken resourceOwnerToken = new ResourceOwnerToken();
        resourceOwnerToken.setId(UUID.randomUUID());
        resourceOwnerToken.setResourceOwner(resourceOwner);
        resourceOwnerToken.setToken(tokenGraph.getToken());

        resourceOwnerTokenRepository.insert(resourceOwnerToken);

        // build the response.
        List<String> responseAudience = tokenGraph.getToken().getAudience()
                .stream()
                .map(i->i.getId().toString())
                .collect(Collectors.toList());

        TokenResponse tr = tokenResponseBuilder
                .setAccessToken(tokenGraph.getPlainTextAccessToken())
                .setRefreshAccessToken(tokenGraph.getPlainTextRefreshToken().get())
                .setTokenType(TokenType.BEARER)
                .setExpiresIn(tokenGraph.getToken().getSecondsToExpiration())
                .setExtension(tokenGraph.getExtension())
                .setIssuer(issuer)
                .setAudience(responseAudience)
                .setIssuedAt(OffsetDateTime.now().toEpochSecond())
                .setExpirationTime(tokenGraph.getToken().getExpiresAt().toEpochSecond())
                .setAuthTime(tokenGraph.getToken().getCreatedAt().toEpochSecond())
                .build();
        return tr;
    }
}