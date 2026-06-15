package com.senctraiq.security;



import com.senctraiq.roles.Role;
import com.senctraiq.users.User;
import com.senctraiq.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public DetailsService(UserRepository userRepository) {
        this.userRepository= userRepository;
    }

    @Override
    public org.springframework.security.core.userdetails.User loadUserByUsername(String username) throws UsernameNotFoundException {

       User user = userRepository.findByUsernameAndDeleted(username, false)
                .orElseThrow(() -> new UsernameNotFoundException("email not found" ));

        // Mapping roles to authorities (permissions)
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), mapRolesToAuthorities(user.getRole()));
    }

    // Adjusted to work with the Erole enum for authorities
    private Collection<GrantedAuthority> mapRolesToAuthorities(List<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.getAuthorities().stream())
                .collect(Collectors.toList());
    }
}
