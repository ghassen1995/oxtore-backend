package com.oxtore.backend.store.mapper;

import com.oxtore.backend.store.DTO.ResellerRelationResponse;
import com.oxtore.backend.store.entity.ResellerRelation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResellerRelationMapper {

    @Mapping(target = "sourceStoreId", source = "sourceStore.id")
    @Mapping(target = "sourceStoreName", source = "sourceStore.storeName")
    @Mapping(target = "resellerStoreId", source = "resellerStore.id")
    @Mapping(target = "resellerStoreName", source = "resellerStore.storeName")
    ResellerRelationResponse toResponse(ResellerRelation relation);
}