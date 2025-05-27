package com.mysillydreams.product.dtos;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductResponseDto {
    private String id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private String productTypeId;
    private String vendorId;
    private Map<String, Object> customAttributes;
    // Consider adding productTypeName or other derived fields if useful for frontend
}
