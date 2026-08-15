package com.oxtore.backend.product.controller;

import com.oxtore.backend.product.DTOs.CreateProductRequest;
import com.oxtore.backend.product.DTOs.ProductResponse;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.mapper.ProductMapper;
import com.oxtore.backend.product.service.ProductService;
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

    @PostMapping()
    public ResponseEntity<ProductResponse> addProduct(@RequestBody CreateProductRequest createProductRequest) {
        Product product = productMapper.productRequestToProduct(createProductRequest);
        product.getWholesalePriceTiers().forEach(tier -> tier.setProduct(product));
        if(createProductRequest.commissionRule() != null) {
            product.getCommissionRule().setProduct(product);
        }
        productService.save(product);
        return ResponseEntity.ok(productMapper.productToProductResponse(product));
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
