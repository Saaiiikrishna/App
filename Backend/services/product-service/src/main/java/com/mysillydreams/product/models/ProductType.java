package com.mysillydreams.product.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_types")
public class ProductType {
    @Id
    private String id;
    private String name;
    private String description;
    private List<FieldDefinition> fieldDefinitions;
}
