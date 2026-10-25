package com.oxtore.backend.product.mapper;

import com.oxtore.backend.product.DTOs.CreateProductRequest;
import com.oxtore.backend.product.DTOs.ProductResponse;
import com.oxtore.backend.product.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ProductImageUrlMapping.class)
public interface ProductMapper {
    ProductResponse productToProductResponse(Product product);
    Product productRequestToProduct(CreateProductRequest productRequest);
}
