package com.example.supportticketingsystem.dto.collection;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "ourusers")
@Data
public class OurUsers implements UserDetails {

    public static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "LEVEL-1", "LEVEL-2", "LEVEL-3", "LEVEL-4", "VENDOR", "SUPPORTER", "DEFAULT");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is mandatory")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @Size(min = 6, message = "Password should have at least 6 characters")
    @NotBlank(message = "Password is mandatory")
    private String password;

    @NotBlank(message = "City is mandatory")
    private String city;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles;
    private Set<String> productGroup; // New field

    public void setRoles(Set<String> roles) {
        for (String role : roles) {
            if (!ALLOWED_ROLES.contains(role)) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
        }
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
