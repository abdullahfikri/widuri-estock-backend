package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.UserPrincipal;
import dev.mfikri.widuriestock.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;


    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findById(username).orElseThrow(() -> new UsernameNotFoundException("Username or password wrong"));
        return new UserPrincipal(user.getUsername(), "{bcrypt}"+ user.getPassword(), "ROLE_"+user.getRole());
    }
}
