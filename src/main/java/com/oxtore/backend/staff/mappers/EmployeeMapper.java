package com.oxtore.backend.staff.mappers;

import com.oxtore.backend.staff.DTOs.EmployeeResponse;
import com.oxtore.backend.staff.entity.StoreStaff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    @Mapping(target = "boutiqueId", source = "store.id")
    @Mapping(target = "boutiqueName", source = "store.storeName")
    @Mapping(target = "fullName", ignore = true) // pull from User if needed — join or skip for demo
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "status", constant = "active")
    EmployeeResponse toResponse(StoreStaff staff);
}