package fr.gesta.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GestaUserDetailsServiceTest {


    @Mock
    private UserRepository userRepository;

    private GestaUserDetailsService gestaUserDetailsService;

    @BeforeEach
    void init() {
        gestaUserDetailsService = new GestaUserDetailsService(userRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void loadUserByUsernameTest(boolean enabled) {
        User user = new User("test", "hash");
        user.setEnabled(enabled);
        when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));

        UserDetails response = gestaUserDetailsService.loadUserByUsername("test");

        assertThat(response.getUsername()).isEqualTo("test");
        assertThat(response.getPassword()).isEqualTo("hash");
        assertThat(response.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(response.isEnabled()).isEqualTo(enabled);
    }

    @Test
    void loadUserByUsernameTestReturnUsernameNotFound() {
        when(userRepository.findByUsername("test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gestaUserDetailsService.loadUserByUsername("test"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("test");

    }
}
