package com.oxtore.backend.stock.mapper;

import com.oxtore.backend.stock.DTO.SupplyRequestResponse;
import com.oxtore.backend.stock.entity.SupplyRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupplyRequestMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "sourceStoreId", source = "sourceStore.id")
    @Mapping(target = "sourceStoreName", source = "sourceStore.storeName")
    @Mapping(target = "resellerStoreId", source = "resellerStore.id")
    @Mapping(target = "resellerStoreName", source = "resellerStore.storeName")
    SupplyRequestResponse toResponse(SupplyRequest request);
}