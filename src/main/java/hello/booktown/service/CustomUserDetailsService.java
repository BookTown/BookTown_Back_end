package hello.booktown.service;

import hello.booktown.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final BCryptPasswordEncoder passwordEncoder;

    public CustomUserDetailsService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!username.equals("taeyang")) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        String encodedPassword = passwordEncoder.encode("1234");

        return new CustomUserDetails("taeyang", encodedPassword);
    }
}