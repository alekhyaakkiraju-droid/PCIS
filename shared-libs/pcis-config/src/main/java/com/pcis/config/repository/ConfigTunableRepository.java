package com.pcis.config.repository;

import com.pcis.config.entity.ConfigTunableEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigTunableRepository extends JpaRepository<ConfigTunableEntity, Long> {

  Optional<ConfigTunableEntity> findByTunableKeyAndVersionNo(String tunableKey, Integer versionNo);

  List<ConfigTunableEntity> findByTunableKeyOrderByVersionNoDesc(String tunableKey);
}
