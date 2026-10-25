package com.oxtore.backend.product.service;


import com.oxtore.backend.product.DTOs.CommissionRuleRequest;
import com.oxtore.backend.product.DTOs.CreateProductRequest;
import com.oxtore.backend.product.DTOs.WholesalePriceTierRequest;
import com.oxtore.backend.product.entity.CommissionRule;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.entity.ProductImage;
import com.oxtore.backend.product.entity.WholesalePriceTier;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.product.exceptions.CommissionRuleNotAllowedException;
import com.oxtore.backend.product.exceptions.MarketPriceNotAllowedException;
import com.oxtore.backend.product.exceptions.WholeSaleIsEmptyException;
import com.oxtore.backend.product.exceptions.WholeSaleIsNotAllowedException;
import com.oxtore.backend.product.repository.ProductRepository;
import com.oxtore.backend.stock.service.StockService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final StockService stockService;

    @Override
    @Transactional
    public Product createProduct(CreateProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Product product = Product.builder()
                .condition(request.condition())
                .productName(request.productName())
                .unitOfSale(request.unitOfSale())
                .status(request.status())
                .storeId(request.storeId())
                .build();

        if (request.images() != null) {
            request.images().forEach(img -> {
                ProductImage image = ProductImage.builder()
                        .url(img.url())
                        .displayOrder(img.displayOrder())
                        .build();
                image.setProduct(product);
                product.getImages().add(image);
            });
        }

        Product savedProduct = productRepository.save(product);

        stockService.createStock(
                request.storeId(),
                savedProduct.getId(),
                request.initialStock(),
                request.saleType(),
                request.transactionMode(),
                request.sellingPrice(),
                request.marketPrice(),
                request.commissionRule(),          // CommissionRuleRequest — DTO, unmapped
                request.wholesalePriceTiers(),      // List<WholesalePriceTierRequest> — DTO, unmapped
                null
        );

        return savedProduct;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<Product> findAllProductsByStoreId(Long storeId) {
        return productRepository.findAllProductsByStoreId(storeId);
    }

}
