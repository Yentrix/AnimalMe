package com.iax.animalme.service;

import com.iax.animalme.model.Client;
import com.iax.animalme.repository.ClientRepository;
import jakarta.transaction.Transactional;
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
}
