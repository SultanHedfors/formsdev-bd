package com.example.demo.util;

import com.example.demo.exception.CurrentUserNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUtil {

    //Preventing class instantiation
    private AuthUtil() {
    }

    public static String userFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            throw new CurrentUserNotFoundException();
        }
        return userDetails.getUsername();
    }
}
