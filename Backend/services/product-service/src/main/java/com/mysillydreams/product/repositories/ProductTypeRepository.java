package com.mysillydreams.product.repositories;
import com.mysillydreams.product.models.ProductType;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ProductTypeRepository extends MongoRepository<ProductType, String> {
    Optional<ProductType> findByName(String name);
}
