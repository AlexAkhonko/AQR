package org.aqr.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.aqr.entity.User;
import org.aqr.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User save(User user) {
        //user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User findByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User update(Long id, User userDetails) {
        User user = findById(id);
        user.setLogin(userDetails.getLogin());
        if (userDetails.getPassword() != null) {
            //user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        return userRepository.save(user);
    }

    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }

    public UserDetails loadUserByUsername(String username) {
        User user = findByLogin(username);  // ваша логика из БД
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getLogin())
                .password(user.getPassword())  // BCrypt!
                .authorities("ROLE_USER")
                .build();
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
