package com.iax.animalme.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "clients")
public class Client extends User{
    private String address;
    private String contactInfo;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Pet> pets;

    @OneToMany(mappedBy = "client")
    private List<AdoptionPost> posts;
}
