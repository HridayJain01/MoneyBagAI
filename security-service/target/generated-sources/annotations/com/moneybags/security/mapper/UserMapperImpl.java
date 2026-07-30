package com.moneybags.security.mapper;

import com.moneybags.security.dto.UserRequest;
import com.moneybags.security.dto.UserResponse;
import com.moneybags.security.entity.User;
import com.moneybags.security.enums.UserStatus;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T16:53:29+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toEntity(UserRequest request) {
        if ( request == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.username( request.username() );
        user.email( request.email() );
        user.passwordHash( request.passwordHash() );
        user.fullName( request.fullName() );
        user.mobile( request.mobile() );
        user.status( request.status() );

        user.failedAttempts( 0 );

        return user.build();
    }

    @Override
    public UserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        Long userId = null;
        String username = null;
        String email = null;
        String fullName = null;
        String mobile = null;
        UserStatus status = null;
        LocalDateTime lastLoginAt = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        userId = user.getUserId();
        username = user.getUsername();
        email = user.getEmail();
        fullName = user.getFullName();
        mobile = user.getMobile();
        status = user.getStatus();
        lastLoginAt = user.getLastLoginAt();
        createdAt = user.getCreatedAt();
        updatedAt = user.getUpdatedAt();

        UserResponse userResponse = new UserResponse( userId, username, email, fullName, mobile, status, lastLoginAt, createdAt, updatedAt );

        return userResponse;
    }
}
