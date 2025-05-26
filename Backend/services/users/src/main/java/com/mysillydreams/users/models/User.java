package com.mysillydreams.users.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Document
public class User {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Address address;
}
