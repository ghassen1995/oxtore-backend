package com.oxtore.backend.identity.service;

import com.oxtore.backend.identity.command.LoginCommand;
import com.oxtore.backend.identity.command.UserCommand;
import com.oxtore.backend.identity.entity.User;

public interface IdentityService {
    User createUser(UserCommand user);
    User authenticateUser(LoginCommand command);
}
