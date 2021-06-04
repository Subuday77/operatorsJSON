package com.operatorsJSON.DAO.dbRelated;

import com.operatorsJSON.beans.dbRelated.Operator;
import com.operatorsJSON.repo.dbRelated.OperatorRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class OperatorDAO {
    @Autowired
    OperatorRepo operatorRepo;

    public void addOperator(Operator operator) {
        operatorRepo.save(operator);
    }

    public void deleteOperator(Operator operator) {
        operatorRepo.delete(operator);
    }

    public Optional<Operator> findOperatorByOperatorId(long operatorId) {
        return operatorRepo.findByOperatorId(operatorId);
    }

    public Collection<Operator> getAllOperators() {
        return operatorRepo.findAll();
    }
}
