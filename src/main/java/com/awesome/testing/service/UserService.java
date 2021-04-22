package com.awesome.testing.service;

import javax.servlet.http.HttpServletRequest;

import com.awesome.testing.dto.LoginDto;
import com.awesome.testing.exception.CustomException;
import com.awesome.testing.repository.UserRepository;
import com.awesome.testing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.awesome.testing.model.User;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public String signIn(LoginDto loginDetails) {
        String username = loginDetails.getUsername();
        String password = loginDetails.getPassword();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            return jwtTokenProvider.createToken(username, userRepository.findByUsername(username).getRoles());
        } catch (AuthenticationException e) {
            throw new CustomException("Invalid username/password supplied", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public String signUp(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new CustomException("Username is already in use", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return jwtTokenProvider.createToken(user.getUsername(), user.getRoles());
    }

    public void delete(String username) {
        userRepository.deleteByUsername(username);
    }

    public User search(String username) {
        return Optional.ofNullable(userRepository.findByUsername(username))
                .orElseThrow(() -> new CustomException("The user doesn't exist", HttpStatus.NOT_FOUND));
    }

    public User whoAmI(HttpServletRequest req) {
        String token = jwtTokenProvider.extractTokenFromRequest(req);
        return userRepository.findByUsername(jwtTokenProvider.getUsername(token));
    }

    public String refresh(String username) {
        return jwtTokenProvider.createToken(username, userRepository.findByUsername(username).getRoles());
    }

}