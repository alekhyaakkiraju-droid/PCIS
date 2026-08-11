package com.pcis.config.repository;

import com.pcis.config.entity.ConfigRuleSetEntity;
import com.pcis.config.entity.ConfigRuleSetEntityId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRuleSetRepository
    extends JpaRepository<ConfigRuleSetEntity, ConfigRuleSetEntityId> {

  Optional<ConfigRuleSetEntity> findByRuleSetKeyAndVersionNo(String ruleSetKey, Integer versionNo);

  List<ConfigRuleSetEntity> findByRuleSetKeyOrderByVersionNoDesc(String ruleSetKey);
}
