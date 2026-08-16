package com.oxtore.backend.store.controller;

import com.oxtore.backend.store.DTO.CreateStoreRequest;
import com.oxtore.backend.store.DTO.StoreResponse;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.mapper.StoreMapper;
import com.oxtore.backend.store.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
public class StoreController {
    private final StoreService storeService;
    private final StoreMapper storeMapper;

    public StoreController(StoreService storeService, StoreMapper storeMapper) {
        this.storeService = storeService;
        this.storeMapper = storeMapper;
    }

    @PostMapping()
    public ResponseEntity<StoreResponse> createStore(@Valid @RequestBody CreateStoreRequest request, HttpServletRequest httpRequest) {
        Long ownerId = Long.valueOf(httpRequest.getAttribute("userId").toString());
        Store store = storeMapper.toEntity(request);
        Store saved = storeService.createStore(store, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(storeMapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStoreById(@PathVariable Long id) {
        return storeService.getStoreById(id)
                .map(store -> ResponseEntity.ok(storeMapper.toResponse(store)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping()
    public ResponseEntity<List<StoreResponse>> getAllStoresByOwnerId(HttpServletRequest httpRequest) {
        Long currentUser = Long.valueOf(httpRequest.getAttribute("userId").toString());
        List<StoreResponse> stores = storeService.findStoresByCurrentUser(currentUser)
                .stream()
                .map(storeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStoreById(@PathVariable Long id) {
        storeService.deleteStoreById(id);
        return ResponseEntity.noContent().build();
    }
}
