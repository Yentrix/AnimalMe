package com.iax.animalme.controller;

import com.iax.animalme.model.Client;
import com.iax.animalme.model.User;
import com.iax.animalme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // GET /api/users
    @GetMapping
    public List<Client> listAllProfiles() {
        return userService.getAllClients();
    }

    // GET /api/users/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // POST /api/users/register/client
    @PostMapping("/register/client")
    public ResponseEntity<Client> register(@RequestBody Client client) {
        return ResponseEntity.ok(userService.registerClient(client));
    }
}
