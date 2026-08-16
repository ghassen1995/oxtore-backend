package com.oxtore.backend.staff.mappers;

import com.oxtore.backend.staff.DTOs.AddStaffRequest;
import com.oxtore.backend.staff.DTOs.StoreStaffResponse;
import com.oxtore.backend.staff.entity.StoreStaff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreStaffMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "addedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StoreStaff toEntity(AddStaffRequest request);

    StoreStaffResponse toResponse(StoreStaff storeStaff);
}