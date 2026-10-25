package com.oxtore.backend.store.DTO;

import java.util.List;

public record CreateBoutiqueRequest(String name, String address, String phone, String description,
                                    String language, String currency, List<String> categories,
                                    String logo) {}
