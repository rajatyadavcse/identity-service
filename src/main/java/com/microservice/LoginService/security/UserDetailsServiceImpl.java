package com.microservice.LoginService.security;

import com.microservice.LoginService.entity.User;
import com.microservice.LoginService.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // TEMPORARY DEBUG — remove after fixing auth issue
        String pwd = user.getPassword();
        String preview = (pwd != null && pwd.length() >= 4) ? pwd.substring(0, 4) : pwd;
        log.warn("DEBUG loadUserByUsername('{}') — stored password prefix='{}', isActive={}",
                username, preview, user.getIsActive());

        return new UserPrincipal(user);
    }
}
