package com.oxtore.backend.product.controller;

import com.oxtore.backend.common.security.RequireStoreRole;
import com.oxtore.backend.product.DTOs.CreateProductRequest;
import com.oxtore.backend.product.DTOs.ProductResponse;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.mapper.ProductMapper;
import com.oxtore.backend.product.service.ProductService;
import com.oxtore.backend.staff.enums.StaffRole;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @RequireStoreRole(value = {StaffRole.SUPERVISOR}, storeIdSource = "body")
    @PostMapping()
    public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productMapper.productToProductResponse(product));
    }

    @GetMapping()
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<Product> products = productService.findAll();
        List<ProductResponse> productResponses = products.stream().map(productMapper::productToProductResponse).toList();
        return ResponseEntity.ok(productResponses);
    }

    @GetMapping("/{storeId}")
    ResponseEntity<List<ProductResponse>>findAllProductsByStoreId(@PathVariable Long storeId) {
        List<Product> products = productService.findAllProductsByStoreId(storeId);
        List<ProductResponse> productResponses = products.stream().map(productMapper::productToProductResponse).toList();
        return ResponseEntity.ok(productResponses);
    }

    /*@GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .map(product -> {
                    ProductResponse response = productMapper.productToProductResponse(product);
                    ProductStockResponse stock = stockServiceClient.getStockByProduct(id);
                    if (stock != null) {
                        response = new ProductResponse(
                                response.id(),
                                response.condition(),
                                response.saleType(),
                                response.transactionMode(),
                                response.productName(),
                                response.images(),
                                response.marketPrice(),
                                response.retailPrice(),
                                stock.quantity(),
                                response.unitOfSale(),
                                response.wholesalePriceTiers(),
                                response.status(),
                                response.commissionRule(),
                                response.createdAt(),
                                response.updatedAt(),
                                response.storeId()
                        );
                    }
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }*/

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteById(id);
        return ResponseEntity.ok().build();
    }

}
