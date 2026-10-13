package com.anurag.cse.controller;
import com.anurag.cse.dto.Dtos.*;
import com.anurag.cse.entity.User;
import com.anurag.cse.repository.UserRepository;
import com.anurag.cse.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        if (users.findByPhone(r.phone()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        User u = new User();
        u.setName(r.name().trim()); u.setPhone(r.phone()); u.setPassword(encoder.encode(r.password()));
        u.setRole(r.role()); u.setLanguage(r.language() == null ? "en" : r.language()); u.setAddress(r.address());
        users.save(u);
        return new AuthResponse(jwt.generate(u.getPhone(), u.getRole().name()), u.getRole().name(), u.getName(), u.getLanguage());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByPhone(r.phone())
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong phone or password"));
        return new AuthResponse(jwt.generate(u.getPhone(), u.getRole().name()), u.getRole().name(), u.getName(), u.getLanguage());
    }
}