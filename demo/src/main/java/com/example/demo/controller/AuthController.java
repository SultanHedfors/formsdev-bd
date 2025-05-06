package com.example.demo.controller;

import com.example.demo.dto.AuthRequest;
import com.example.demo.dto.AuthResponse;
import com.example.demo.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;



    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(), authRequest.getPassword())
            );
            String token = jwtUtil.generateToken((UserDetails) authentication.getPrincipal());
            String role = jwtUtil.extractRole(token);
            String employeeCode = jwtUtil.extractUsername(token);
            return ResponseEntity.ok(new AuthResponse(token, role, employeeCode));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credentials");
        }
    }
}
