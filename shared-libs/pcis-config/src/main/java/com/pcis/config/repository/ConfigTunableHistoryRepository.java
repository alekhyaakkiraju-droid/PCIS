package com.pcis.config.repository;

import com.pcis.config.entity.ConfigTunableHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigTunableHistoryRepository
    extends JpaRepository<ConfigTunableHistoryEntity, Long> {

  List<ConfigTunableHistoryEntity> findByTunableKeyOrderByChangedTimestampDesc(String tunableKey);
}
