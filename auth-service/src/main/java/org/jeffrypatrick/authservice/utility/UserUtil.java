package org.jeffrypatrick.authservice.utility;

import org.jeffrypatrick.authservice.model.Gender;
import org.jeffrypatrick.authservice.model.Role;
import org.jeffrypatrick.authservice.model.Status;
import org.jeffrypatrick.authservice.model.User;

import java.time.LocalDate;

public final class UserUtil {
    public static User commonCreateUser(
            String firstName,
            String lastName,
            String email,
            String password,
            LocalDate dob,
            Gender gender,
            String mobile,
            Status status,
            Role role

    ) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setDob(dob);
        user.setGender(gender);
        user.setMobile(mobile);
        user.setStatus(status);
        user.setRole(role);
        return user;
    }
}
