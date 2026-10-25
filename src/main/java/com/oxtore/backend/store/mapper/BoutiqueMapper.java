package com.oxtore.backend.store.mapper;

import com.oxtore.backend.store.DTO.BoutiqueResponse;
import com.oxtore.backend.store.entity.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BoutiqueMapper {
    @Mapping(target = "name", source = "storeName")
    @Mapping(target = "logo", ignore = true)
    @Mapping(target = "address", source = "location")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "description", source = "slogan")
    @Mapping(target = "ownerIds", ignore = true)
    @Mapping(target = "managerId", ignore = true)
    @Mapping(target = "managerName", ignore = true)
    @Mapping(target = "status", constant = "active")
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "language", constant = "en")
    @Mapping(target = "currency", constant = "TND")
    @Mapping(target = "categories", expression = "java(java.util.List.of(store.getBusinessSector().name()))")
    @Mapping(target = "employeeCount", ignore = true)
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "revenue", constant = "0")
    @Mapping(target = "createdAt", source = "createdAt")
    BoutiqueResponse toResponse(Store store);
}
