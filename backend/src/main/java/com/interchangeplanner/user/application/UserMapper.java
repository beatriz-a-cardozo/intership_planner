package com.interchangeplanner.user.application;

import com.interchangeplanner.user.api.MeResponse;
import com.interchangeplanner.user.api.UserSettingsResponse;
import com.interchangeplanner.user.domain.AppUser;
import com.interchangeplanner.user.domain.UserSettings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    UserSettingsResponse toResponse(UserSettings settings);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "displayName", source = "user.displayName")
    @Mapping(target = "onboardingCompleted", source = "user.onboardingCompleted")
    @Mapping(target = "settings", source = "settings")
    MeResponse toMe(AppUser user, UserSettings settings);
}
