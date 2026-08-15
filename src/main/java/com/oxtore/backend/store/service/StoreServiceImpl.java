package com.oxtore.backend.store.service;

import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;

    public StoreServiceImpl(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    @Override
    public Store createStore(Store store) {
        Store saved = storeRepository.save(store);
        return saved;
    }

    @Override
    public Optional<Store> getStoreById(Long id) {
        return storeRepository.findById(id);
    }

    @Override
    public List<Store> getAllStoresByOwnerId(Long ownerId) {
        return storeRepository.findAllByOwnerId(ownerId);
    }

    @Override
    public void deleteStoreById(Long id) {
        storeRepository.deleteById(id);
    }
}
