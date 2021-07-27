package com.operatorsJSON.DAO.dbRelated;


import com.operatorsJSON.beans.dbRelated.OperatorsDynamicConfig;
import com.operatorsJSON.repo.dbRelated.OperatorsDynamicConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class OperatorsDynamicConfigDAO {
    @Autowired
    OperatorsDynamicConfigRepo dynamicConfigRepo;

    public void addDynamicConfig(OperatorsDynamicConfig dynamicConfig) {
        dynamicConfigRepo.save(dynamicConfig);
    }

    public void deleteDynamicConfig(OperatorsDynamicConfig dynamicConfig) {
        dynamicConfigRepo.delete(dynamicConfig);
    }

    public Optional<OperatorsDynamicConfig> findDynamicCondigById(Long id) {
        return dynamicConfigRepo.findById(id);
    }
}
