package com.mysillydreams.users.controllers;

import com.mysillydreams.users.models.User;
// Assume UserService interface exists in a package like com.mysillydreams.users.services
// For now, this import might be commented out or point to a non-existent class
// import com.mysillydreams.users.services.UserService; 

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed; // For @RolesAllowed
import java.util.List;
import java.util.Optional; // Or however your service layer will return users


// Placeholder for UserService - this interface would be in com.mysillydreams.users.services.UserService
interface UserService {
    Optional<User> findById(String id);
    List<User> findAll();
    // Other methods would be added later
}

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService; // Will be properly imported once created

    // @Autowired // Or constructor injection
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    // @PreAuthorize("hasAuthority('ROLE_ADMIN') or #id == @jwt.subject") // Using subject from Jwt directly
    // Note: For @jwt.subject to work, Jwt needs to be a bean or accessed via SecurityContextHolder.
    // A simpler way for self-check might be to pass Jwt principal and compare its subject.
    // Let's refine this if direct SpEL on @jwt.subject is problematic.
    // Alternative for self-check: requires custom method or passing more principal details.
    // For now, let's assume a simplified version or focus on ADMIN role for this step.
    // To make it robust for self-check:
    // @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Simpler for now, or implement a utility for self-check
    public ResponseEntity<User> getUserById(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        // Manual check for self or admin:
        List<String> roles = jwt.getClaimAsStringList("roles"); // Get roles claim
        boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
        boolean isSelf = jwt.getSubject().equals(id); // Assuming user ID in path is Keycloak subject

        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @RolesAllowed("ROLE_ADMIN") // JSR-250 annotation
    // Or @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Spring Security annotation
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }
}
