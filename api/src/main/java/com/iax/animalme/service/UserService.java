package com.iax.animalme.service;

import com.iax.animalme.model.Client;
import com.iax.animalme.repository.ClientRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {
    private ClientRepository clientRepository;

    public UserService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client registerClient(Client client) {
        // Encode password
        return clientRepository.save(client);
    }

    public Optional<Client> getUserById(@PathVariable("id") Long id) {
        return clientRepository.findById(id);
    }

    public List<Client> getAllClients() {
        return clientRepository.findAll();
    }

    public Optional<Client> login(Client client) {
        try {
            boolean isValid = authenticate(client.getEmail(), client.getPassword());

            if (isValid) {
                return clientRepository.findByEmail(client.getEmail());
            } else {
                throw new RuntimeException("Email o clave incorrectos");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean authenticate(String email, String password){
        return clientRepository.findByEmail(email)
                .map(client -> password.equals(client.getPassword()))
                .orElse(false);
    }
}
