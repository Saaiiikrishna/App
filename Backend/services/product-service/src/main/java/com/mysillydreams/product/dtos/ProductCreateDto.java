package com.mysillydreams.product.dtos;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class ProductCreateDto {
    private String name;
    private String description;
    private BigDecimal basePrice;
    private String productTypeId;
    private String vendorId; // For admin to specify; for vendors, it's derived from token
    private Map<String, Object> customAttributes;
}
