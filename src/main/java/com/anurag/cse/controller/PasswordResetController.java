package com.anurag.cse.controller;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.anurag.cse.entity.User;
import com.anurag.cse.repository.UserRepository;     // adjust to match AuthController

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Map<String, Otp> otps = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public PasswordResetController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    record Otp(String code, long expiresAt) {}
    record ForgotRequest(String phone) {}
    record ResetRequest(String phone, String otp, String newPassword) {}

    @PostMapping("/forgot-password")
    public Map<String, String> forgot(@RequestBody ForgotRequest r) {
        if (r.phone() == null || !r.phone().matches("\\d{10}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone must be 10 digits");
        if (users.findByPhone(r.phone()).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phone not registered");

        String code = String.format("%06d", random.nextInt(1_000_000));
        otps.put(r.phone(), new Otp(code, System.currentTimeMillis() + 5 * 60 * 1000));
        System.out.println(">>> PASSWORD RESET OTP for " + r.phone() + " = " + code);
        return Map.of("message", "OTP sent");
    }

    @PostMapping("/reset-password")
    public Map<String, String> reset(@RequestBody ResetRequest r) {
        if (r.newPassword() == null || r.newPassword().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");

        Otp o = otps.get(r.phone());
        if (o == null || o.expiresAt() < System.currentTimeMillis() || !o.code().equals(r.otp()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");

        User u = users.findByPhone(r.phone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phone not registered"));
        u.setPassword(encoder.encode(r.newPassword()));
        users.save(u);
        otps.remove(r.phone());
        return Map.of("message", "Password changed");
    }
}
