package com.ricash.ricash.config;

import com.ricash.ricash.model.Admin;
import com.ricash.ricash.model.Agent;
import com.ricash.ricash.model.User;
import com.ricash.ricash.repository.adminRepository;
import com.ricash.ricash.repository.agentRepository;
import com.ricash.ricash.repository.userRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
public class MyUserDetailService implements UserDetailsService {

    private final userRepository userRepository;
    private final agentRepository agentRepository;
    private final adminRepository adminRepository;

    public MyUserDetailService(userRepository userRepository,
                               agentRepository agentRepository,
                               adminRepository adminRepository) {
        this.userRepository = userRepository;
        this.agentRepository = agentRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Chercher d'abord dans les utilisateurs
        Optional<User> user = userRepository.findByEmail(username);
        if (user.isPresent()) {
            return createUserDetails(user.get().getEmail(), user.get().getMotDePasse(),
                    "USER", user.get().isActif()); // Correction ici: isActif()
        }

        // Chercher dans les agents
        Optional<Agent> agent = agentRepository.findByEmail(username);
        if (agent.isPresent()) {
            return createUserDetails(agent.get().getEmail(), agent.get().getMotDePasse(),
                    "AGENT", agent.get().isEstActif());
        }

        // Chercher dans les admins
        Optional<Admin> admin = adminRepository.findByEmail(username);
        if (admin.isPresent()) {
            return createUserDetails(admin.get().getEmail(), admin.get().getMotDePasse(),
                    "ADMIN", admin.get().isEstActif());
        }

        throw new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + username);
    }

    private UserDetails createUserDetails(String username, String password, String role, boolean enabled) {
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        return new org.springframework.security.core.userdetails.User(
                username,
                password,
                enabled,
                true, // account non expired
                true, // credentials non expired
                true, // account non locked
                authorities
        );
    }
}