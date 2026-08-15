package com.oxtore.backend.product.service;


import com.oxtore.backend.product.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    Product save(Product product);
    List<Product> findAll();
    Optional<Product> findById(Long id);
    void deleteById(Long id);
    List<Product> findAllProductsByStoreId(Long storeId);

}
