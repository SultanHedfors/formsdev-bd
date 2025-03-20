package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "EMPLOYEE")
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMPLOYEE_ID")
    private Long id;

    @Column(unique = true,name = "EMPLOYEE_USERNAME")
    private String username;

    @Column(name = "EMPLOYEE_POSCE")
    private String password;

    @Column(name = "EMPLOYEE_KODKASJERA")
    private String employeeCode;

    @Column(name = "EMPLOYEE_LOYALTY_PIN")
    private Integer loyaltyPin;

    @ManyToMany
    @JoinTable(
            name = "stanzabieg",
            joinColumns = @JoinColumn(name = "stanowisko_id"),
            inverseJoinColumns = @JoinColumn(name = "zabieg_id")
    )
    private Set<ProcedureEntity> procedures = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "USER_ROLES",
            joinColumns = @JoinColumn(name = "user_id") // Ensure correct column name
    )
    @Column(name = "ROLE")
    private List<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
