package com.operatorsJSON.repo.dbRelated;

import com.operatorsJSON.beans.dbRelated.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperatorRepo extends JpaRepository<Operator, Long> {

    Optional<Operator> findByOperatorId(long operatorId);
}
