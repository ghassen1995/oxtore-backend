package com.oxtore.backend.store.service;

import com.oxtore.backend.product.entity.Category;
import com.oxtore.backend.product.repository.CategoryRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.enums.BusinessSector;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Boutique categories use the same slugs as products and the marketplace (categories table: "mode", "electronique"...),
 * so the app translates them with its existing categories.* keys. The legacy single business_sector column (NOT NULL)
 * is derived from the first category.
 */
@Component
public class StoreCategories {

    public static final int MAX_CATEGORIES = 3;
    private static final String ALL = "all";
    private static final String OTHERS = "autres";

    private final CategoryRepository categoryRepository;

    public StoreCategories(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Keeps known slugs only (order kept, duplicates and "all" dropped), at most {@value #MAX_CATEGORIES}.
     * @throws IllegalArgumentException when the list is missing, empty or contains an unknown slug
     */
    public List<String> validate(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one category");
        }
        Set<String> known = categoryRepository.findAll().stream().map(Category::getSlug).collect(Collectors.toSet());
        Set<String> slugs = new LinkedHashSet<>();
        for (String raw : requested) {
            String slug = raw == null ? "" : raw.trim().toLowerCase();
            if (slug.isEmpty() || ALL.equals(slug)) continue;
            if (!known.contains(slug)) throw new IllegalArgumentException("Unknown category: " + raw);
            slugs.add(slug);
        }
        if (slugs.isEmpty()) throw new IllegalArgumentException("Choose at least one category");
        if (slugs.size() > MAX_CATEGORIES) {
            throw new IllegalArgumentException("Choose at most " + MAX_CATEGORIES + " categories");
        }
        return List.copyOf(slugs);
    }

    /** Legacy sector for the first category. */
    public static BusinessSector sectorFor(List<String> slugs) {
        String first = slugs.isEmpty() ? OTHERS : slugs.get(0);
        return switch (first) {
            case "mode", "enfants" -> BusinessSector.FASHION;
            case "electronique", "informatique" -> BusinessSector.ELECTRONICS;
            case "maison-jardin", "bricolage" -> BusinessSector.HOME_AND_GARDEN;
            case "beaute" -> BusinessSector.BEAUTY_AND_HEALTH;
            case "alimentaire" -> BusinessSector.FOOD_AND_BEVERAGE;
            case "sports" -> BusinessSector.SPORTS_AND_LEISURE;
            case "automobile" -> BusinessSector.AUTOMOTIVE;
            default -> BusinessSector.OTHER;
        };
    }

    /** Categories to show: the chosen ones, or for boutiques created before categories existed, their sector's slug. */
    public static List<String> of(Store store) {
        if (store.getCategories() != null && !store.getCategories().isEmpty()) return List.copyOf(store.getCategories());
        BusinessSector sector = store.getBusinessSector();
        if (sector == null) return List.of();
        return List.of(switch (sector) {
            case FASHION -> "mode";
            case ELECTRONICS -> "electronique";
            case HOME_AND_GARDEN -> "maison-jardin";
            case BEAUTY_AND_HEALTH -> "beaute";
            case FOOD_AND_BEVERAGE -> "alimentaire";
            case SPORTS_AND_LEISURE -> "sports";
            case AUTOMOTIVE -> "automobile";
            case OTHER -> OTHERS;
        });
    }
}
