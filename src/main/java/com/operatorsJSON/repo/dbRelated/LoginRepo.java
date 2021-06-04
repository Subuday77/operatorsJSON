package com.operatorsJSON.repo.dbRelated;

import com.operatorsJSON.beans.dbRelated.Login;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoginRepo extends JpaRepository<Login, Long> {

    Optional<Login> findByUserNameIgnoreCase(String userName);

}
