package com.oxtore.backend.stock.service;
import com.oxtore.backend.stock.entity.StockActivity;


public interface StockActivityService {
    StockActivity recordSale(Long stockId, Integer quantitySold, Long soldBy);
}
