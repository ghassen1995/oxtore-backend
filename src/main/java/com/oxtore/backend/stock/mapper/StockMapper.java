package com.oxtore.backend.stock.mapper;

import com.oxtore.backend.product.DTOs.CommissionRuleResponse;
import com.oxtore.backend.product.DTOs.WholesalePriceTierResponse;
import com.oxtore.backend.product.entity.CommissionRule;
import com.oxtore.backend.product.entity.WholesalePriceTier;
import com.oxtore.backend.stock.DTO.StockResponse;
import com.oxtore.backend.stock.entity.Stock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "storeName", source = "store.storeName")
    StockResponse toResponse(Stock stock);

    CommissionRuleResponse toResponse(CommissionRule commissionRule);

    WholesalePriceTierResponse toResponse(WholesalePriceTier tier);

    List<WholesalePriceTierResponse> toTierResponseList(List<WholesalePriceTier> tiers);
}