package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

import static com.example.demo.service.CustomUserDetailsService.Role.ADMIN;
import static com.example.demo.service.CustomUserDetailsService.Role.DEFAULT;
import static com.example.demo.service.CustomUserDetailsService.Role.REASSIGN_ALLOWED;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    public enum Role { ADMIN, REASSIGN_ALLOWED, DEFAULT }
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String employeeCode) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with code: " + employeeCode));

        String role = retrieveRole(user).name();
        return new UserDto(
                user.getId(),
                user.getEmployeeCode(),
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority(role))
        );
    }

    private Role retrieveRole(UserEntity user){
        Integer loyaltyPin = user.getLoyaltyPin();
        System.out.println("loyalty pin: "+ loyaltyPin) ;
        return switch (loyaltyPin) {
            case 1 -> REASSIGN_ALLOWED;
            case 2 -> ADMIN;
            default -> DEFAULT;
        };
    }
}
