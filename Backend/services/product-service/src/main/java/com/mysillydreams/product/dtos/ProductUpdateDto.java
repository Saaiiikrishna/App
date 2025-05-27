package com.mysillydreams.product.dtos;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductUpdateDto {
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Map<String, Object> customAttributes;
    // productTypeId and vendorId are typically not updatable directly via this DTO
}
