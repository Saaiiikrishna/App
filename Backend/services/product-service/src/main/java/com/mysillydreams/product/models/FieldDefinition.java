package com.mysillydreams.product.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {
    private String fieldName;
    private String fieldLabel;
    private CustomFieldType fieldType;
    private boolean isRequired;
    private List<String> options; // Used for SELECT, MULTI_SELECT, CHECKBOX (group)
}
