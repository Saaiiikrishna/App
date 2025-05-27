package com.mysillydreams.users.repositories;

import com.mysillydreams.users.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // Basic CRUD methods like findById, findAll, save, deleteById are inherited
    // Custom query methods can be added here if needed later
}
