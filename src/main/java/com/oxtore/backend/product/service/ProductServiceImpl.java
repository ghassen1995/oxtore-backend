package com.oxtore.backend.product.service;


import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.product.exceptions.CommissionRuleNotAllowedException;
import com.oxtore.backend.product.exceptions.MarketPriceNotAllowedException;
import com.oxtore.backend.product.exceptions.WholeSaleIsEmptyException;
import com.oxtore.backend.product.exceptions.WholeSaleIsNotAllowedException;
import com.oxtore.backend.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public Product save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        if (product.getSaleType() != SaleType.WHOLESALE && product.getMarketPrice() != null) {
            throw new MarketPriceNotAllowedException();
        }

        if (product.getCommissionRule() != null) {
            boolean isAllowed = product.getSaleType() == SaleType.WHOLESALE
                    && product.getTransactionMode() == TransactionMode.CONSIGNMENT;

            if (!isAllowed) {
                throw new CommissionRuleNotAllowedException();
            }
        }

        if (product.getSaleType() == SaleType.WHOLESALE && product.getWholesalePriceTiers().isEmpty()) {
            throw new WholeSaleIsEmptyException();
        }

        if (product.getSaleType() == SaleType.RETAIL && !product.getWholesalePriceTiers().isEmpty()) {
            throw new WholeSaleIsNotAllowedException();
        }

        Product savedProduct = productRepository.save(product);

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
