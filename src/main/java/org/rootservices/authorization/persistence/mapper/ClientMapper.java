package org.rootservices.authorization.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.rootservices.authorization.persistence.entity.Client;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Created by tommackenzie on 11/15/14.
 */
@Repository
public interface ClientMapper {
    Client getByUUID(@Param("uuid") UUID uuid);
    void insert(@Param("client") Client client);
    Client getByCredentialsAndAuthCode(@Param("uuid") UUID uuid, @Param("password") byte[] password, @Param("code") byte[] code);

}

