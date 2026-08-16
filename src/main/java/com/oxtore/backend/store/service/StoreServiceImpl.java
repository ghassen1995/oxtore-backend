package com.oxtore.backend.store.service;

import com.oxtore.backend.staff.entity.StoreStaff;
import com.oxtore.backend.staff.enums.StaffRole;
import com.oxtore.backend.staff.service.StoreStaffService;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreStaffService storeStaffService;

    public StoreServiceImpl(StoreRepository storeRepository, StoreStaffService storeStaffService) {
        this.storeRepository = storeRepository;
        this.storeStaffService = storeStaffService;
    }

    @Transactional
    @Override
    public Store createStore(Store store, Long ownerId) {
        Store saved = storeRepository.save(store);
        storeStaffService.addStaff(saved.getId(), ownerId, StaffRole.SUPERVISOR, ownerId);
        return saved;
    }

    @Override
    public Optional<Store> getStoreById(Long id) {
        return storeRepository.findById(id);
    }

    @Override
    public List<Store> findStoresByCurrentUser(Long currentUser) {
        return storeRepository.findStoresByCurrentUser(currentUser);
    }

    @Override
    public void deleteStoreById(Long id) {
        storeRepository.deleteById(id);
    }
}
