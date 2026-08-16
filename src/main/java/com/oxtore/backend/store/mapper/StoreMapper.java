package com.oxtore.backend.store.mapper;

import com.oxtore.backend.store.DTO.CreateStoreRequest;
import com.oxtore.backend.store.DTO.StoreResponse;
import com.oxtore.backend.store.entity.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Store toEntity(CreateStoreRequest request);

    StoreResponse toResponse(Store store);
}