package net.tokensmith.authorization.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import net.tokensmith.authorization.persistence.entity.ClientScope;
import org.springframework.stereotype.Repository;

/**
 * Created by tommackenzie on 5/12/15.
 */
@Repository
public interface ClientScopesMapper {
    void insert(@Param("clientScope") ClientScope clientScope);
}