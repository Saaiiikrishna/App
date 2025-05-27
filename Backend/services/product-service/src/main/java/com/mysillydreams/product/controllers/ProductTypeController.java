package com.mysillydreams.product.controllers;

import com.mysillydreams.product.models.ProductType;
import com.mysillydreams.product.repositories.ProductTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/product-types")
public class ProductTypeController {

    private static final Logger log = LoggerFactory.getLogger(ProductTypeController.class);
    private final ProductTypeRepository productTypeRepository;

    public ProductTypeController(ProductTypeRepository productTypeRepository) {
        this.productTypeRepository = productTypeRepository;
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ProductType> createProductType(@RequestBody ProductType productType) {
        log.info("Admin request to create product type: {}", productType.getName());
        if (productType.getName() == null || productType.getName().trim().isEmpty()) {
            log.warn("Product type name cannot be empty");
            return ResponseEntity.badRequest().build(); 
        }
        Optional<ProductType> existingType = productTypeRepository.findByName(productType.getName());
        if (existingType.isPresent()) {
            log.warn("Product type with name '{}' already exists", productType.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        ProductType savedProductType = productTypeRepository.save(productType);
        log.info("Product type created successfully with ID: {}", savedProductType.getId());
        return new ResponseEntity<>(savedProductType, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("permitAll()") // As per SecurityConfig, or isAuthenticated()
    public ResponseEntity<List<ProductType>> getAllProductTypes() {
        log.info("Request to list all product types");
        List<ProductType> productTypes = productTypeRepository.findAll();
        return ResponseEntity.ok(productTypes);
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ProductType> getProductTypeById(@PathVariable String id) {
        log.info("Admin request to get product type by ID: {}", id);
        Optional<ProductType> productTypeOptional = productTypeRepository.findById(id);
        return productTypeOptional.map(ResponseEntity::ok)
                                  .orElseGet(() -> {
                                      log.warn("Product type with ID: {} not found", id);
                                      return ResponseEntity.notFound().build();
                                  });
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ProductType> updateProductType(@PathVariable String id, @RequestBody ProductType productTypeDetails) {
        log.info("Admin request to update product type with ID: {}", id);
        Optional<ProductType> existingProductTypeOptional = productTypeRepository.findById(id);
        if (existingProductTypeOptional.isEmpty()) {
            log.warn("Product type with ID: {} not found for update", id);
            return ResponseEntity.notFound().build();
        }

        ProductType existingProductType = existingProductTypeOptional.get();
        if (productTypeDetails.getName() != null && !productTypeDetails.getName().equals(existingProductType.getName())) {
            Optional<ProductType> conflictingType = productTypeRepository.findByName(productTypeDetails.getName());
            if (conflictingType.isPresent() && !conflictingType.get().getId().equals(id)) {
                 log.warn("Product type name '{}' conflicts with an existing type during update", productTypeDetails.getName());
                 return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            existingProductType.setName(productTypeDetails.getName());
        }
        
        existingProductType.setDescription(productTypeDetails.getDescription());
        if (productTypeDetails.getFieldDefinitions() != null) {
            existingProductType.setFieldDefinitions(productTypeDetails.getFieldDefinitions());
        }

        ProductType updatedProductType = productTypeRepository.save(existingProductType);
        log.info("Product type with ID: {} updated successfully", id);
        return ResponseEntity.ok(updatedProductType);
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<Void> deleteProductType(@PathVariable String id) {
        log.info("Admin request to delete product type with ID: {}", id);
        if (!productTypeRepository.existsById(id)) {
            log.warn("Product type with ID: {} not found for deletion", id);
            return ResponseEntity.notFound().build();
        }
        // TODO: Consider implications: prevent deletion if products of this type exist.
        productTypeRepository.deleteById(id);
        log.info("Product type with ID: {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
