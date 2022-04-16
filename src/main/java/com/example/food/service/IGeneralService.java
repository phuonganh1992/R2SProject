package com.example.food.service;

import java.util.Optional;
import java.util.UUID;

public interface IGeneralService<T> {
    Iterable<T> findAll();

    Optional<T> findById(UUID id);

    T save(T t);

    void delete(UUID id);
}
