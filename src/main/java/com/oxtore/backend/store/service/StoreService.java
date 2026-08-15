package com.oxtore.backend.store.service;


import com.oxtore.backend.store.entity.Store;

import java.util.List;
import java.util.Optional;

public interface StoreService {
    Store createStore(Store store);
    Optional<Store> getStoreById(Long id);
    List<Store> getAllStoresByOwnerId(Long ownerId);
    void deleteStoreById(Long id);
}
