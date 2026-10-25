package com.oxtore.backend.stock.service;

import com.oxtore.backend.stock.DTO.ResellerActivityResponse;

import java.time.LocalDate;

public interface ResellerActivityService {

    /** Received / sold / remaining / owed per reseller and product, plus the sales list, for the source store's products. */
    ResellerActivityResponse getResellerActivity(Long sourceStoreId, Long resellerStoreId, LocalDate from, LocalDate to);
}
