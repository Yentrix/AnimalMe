package com.iax.animalme.infrastructure.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iax.animalme.application.dto.LoginRequestDto;
import com.iax.animalme.application.service.AuthApplicationService;
import com.iax.animalme.domain.model.User;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthApplicationService authAppService;

    public AuthController(
            AuthApplicationService authAppService) {
        this.authAppService = authAppService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        User user = authAppService.login(loginRequestDto);
        return ResponseEntity.ok(user);
    }
    

}
