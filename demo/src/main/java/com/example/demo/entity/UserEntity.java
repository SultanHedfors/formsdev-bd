package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "EMPLOYEE")
@ToString(exclude = {"procedures", "activities"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMPLOYEE_ID")
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(unique = true, name = "EMPLOYEE_USERNAME")
    private String username;

    @Column(unique = true, name = "EMPLOYEE_FULLNAME")
    private String fullName;

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
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "ROLE")
    private List<String> roles;

    @ManyToMany(mappedBy = "employees")
    private Set<ActivityEntity> activities = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
