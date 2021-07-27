package com.operatorsJSON.repo.dbRelated;

import com.operatorsJSON.beans.dbRelated.OperatorsDynamicConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorsDynamicConfigRepo extends JpaRepository <OperatorsDynamicConfig, Long> {

}
