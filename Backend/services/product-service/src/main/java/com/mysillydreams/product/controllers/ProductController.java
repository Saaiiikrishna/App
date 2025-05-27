package com.mysillydreams.product.controllers;

import com.mysillydreams.product.dtos.ProductCreateDto;
import com.mysillydreams.product.dtos.ProductUpdateDto;
import com.mysillydreams.product.models.Product;
import com.mysillydreams.product.models.ProductType;
import com.mysillydreams.product.models.FieldDefinition;
import com.mysillydreams.product.models.CustomFieldType;
import com.mysillydreams.product.repositories.ProductRepository;
import com.mysillydreams.product.repositories.ProductTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private final ProductRepository productRepository;
    private final ProductTypeRepository productTypeRepository;

    public ProductController(ProductRepository productRepository, ProductTypeRepository productTypeRepository) {
        this.productRepository = productRepository;
        this.productTypeRepository = productTypeRepository;
    }

    private String validateCustomAttributes(Map<String, Object> customAttributes, List<FieldDefinition> fieldDefinitions) {
        if (customAttributes == null) {
            if (fieldDefinitions.stream().anyMatch(FieldDefinition::isRequired)) {
                return "Required custom attributes map is missing.";
            }
            return null; 
        }
        for (FieldDefinition def : fieldDefinitions) {
            Object value = customAttributes.get(def.getFieldName());
            if (def.isRequired() && value == null) {
                return String.format("Required field '%s' (%s) is missing.", def.getFieldLabel(), def.getFieldName());
            }
            if (value != null) {
                // Placeholder for more sophisticated type validation based on def.getFieldType()
                // e.g., check if Number for CustomFieldType.NUMBER, boolean for BOOLEAN,
                // if value is in def.getOptions() for SELECT/MULTI_SELECT.
            }
        }
        // Optional: Check for extraneous attributes not defined in ProductType
        // for (String key : customAttributes.keySet()) {
        //    if (fieldDefinitions.stream().noneMatch(def -> def.getFieldName().equals(key))) {
        //        return String.format("Extraneous attribute '%s' not defined for this product type.", key);
        //    }
        // }
        return null; // No errors found
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_admin', 'ROLE_vendor')")
    public ResponseEntity<?> createProduct(@RequestBody ProductCreateDto productCreateDto, Authentication authentication) {
        log.info("Request to create product: {}", productCreateDto.getName());

        if (productCreateDto.getProductTypeId() == null) {
            return ResponseEntity.badRequest().body("ProductType ID is required.");
        }
        Optional<ProductType> productTypeOpt = productTypeRepository.findById(productCreateDto.getProductTypeId());
        if (productTypeOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid ProductType ID: " + productCreateDto.getProductTypeId());
        }
        ProductType productType = productTypeOpt.get();

        String validationError = validateCustomAttributes(productCreateDto.getCustomAttributes(), productType.getFieldDefinitions());
        if (validationError != null) {
            return ResponseEntity.badRequest().body("Validation error in custom attributes: " + validationError);
        }

        Product product = new Product();
        product.setName(productCreateDto.getName());
        product.setDescription(productCreateDto.getDescription());
        product.setBasePrice(productCreateDto.getBasePrice());
        product.setProductTypeId(productCreateDto.getProductTypeId());
        product.setCustomAttributes(productCreateDto.getCustomAttributes());

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakUserId = jwt.getSubject();
        boolean isAdmin = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(role -> role.equals("ROLE_admin"));

        if (isAdmin && productCreateDto.getVendorId() != null) {
            product.setVendorId(productCreateDto.getVendorId());
        } else if (authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_vendor"))) {
            product.setVendorId(keycloakUserId); 
        } else if (isAdmin) {
            product.setVendorId("admin_" + keycloakUserId); 
            log.info("Admin creating product without specific vendorId, assigning admin as owner: {}", product.getVendorId());
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User role not permitted to set vendor ID or create product without it.");
        }
        
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_admin') or (hasAuthority('ROLE_vendor') and @productSecurity.isOwner(authentication, #id))")
    public ResponseEntity<?> updateProduct(@PathVariable String id, @RequestBody ProductUpdateDto productUpdateDto, Authentication authentication) {
        log.info("Request to update product with ID: {}", id);
        Optional<Product> productOptional = productRepository.findById(id);
        if (productOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Product existingProduct = productOptional.get();

        Optional<ProductType> productTypeOpt = productTypeRepository.findById(existingProduct.getProductTypeId());
        if (productTypeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Product's type definition not found. This indicates a data inconsistency.");
        }
        ProductType productType = productTypeOpt.get();

        if (productUpdateDto.getCustomAttributes() != null) {
            String validationError = validateCustomAttributes(productUpdateDto.getCustomAttributes(), productType.getFieldDefinitions());
            if (validationError != null) {
                return ResponseEntity.badRequest().body("Validation error in custom attributes: " + validationError);
            }
            existingProduct.setCustomAttributes(productUpdateDto.getCustomAttributes());
        }

        if (productUpdateDto.getName() != null) existingProduct.setName(productUpdateDto.getName());
        if (productUpdateDto.getDescription() != null) existingProduct.setDescription(productUpdateDto.getDescription());
        if (productUpdateDto.getBasePrice() != null) existingProduct.setBasePrice(productUpdateDto.getBasePrice());
        
        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());
        return ResponseEntity.ok(updatedProduct);
    }

    @GetMapping
    @PreAuthorize("permitAll()") // As per SecurityConfig
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Request to list all products");
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()") // As per SecurityConfig
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        log.info("Request to get product by ID: {}", id);
        Optional<Product> product = productRepository.findById(id);
        return product.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("isAuthenticated()") // Or more specific if needed
    public ResponseEntity<List<Product>> getProductsByVendorId(@PathVariable String vendorId) {
        log.info("Request to list products for vendor ID: {}", vendorId);
        List<Product> products = productRepository.findByVendorId(vendorId);
        return ResponseEntity.ok(products);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_admin') or (hasAuthority('ROLE_vendor') and @productSecurity.isOwner(authentication, #id))")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id, Authentication authentication) {
        log.info("Request to delete product with ID: {}", id);
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        log.info("Product with ID: {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}
