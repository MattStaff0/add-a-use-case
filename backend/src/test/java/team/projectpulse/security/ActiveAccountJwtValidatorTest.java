package team.projectpulse.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The validator runs inside the filter chain, so every case it cannot resolve has to come back as a failed token,
 * never an exception (FR-SEC-active-account).
 */
@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtValidatorTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ActiveAccountJwtValidator activeAccountJwtValidator;


    private static Jwt tokenWithUserId(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("j.smith@abc.edu");
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.build();
    }

    private static Student student(boolean enabled) {
        return new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", enabled, "student");
    }

    @Test
    @DisplayName("An active account's token passes")
    void anActiveAccountPasses() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(student(true)));

        assertThat(this.activeAccountJwtValidator.validate(tokenWithUserId(4L)).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("A deactivated account's token fails")
    void aDeactivatedAccountFails() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(student(false)));

        assertThat(this.activeAccountJwtValidator.validate(tokenWithUserId(4L)).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("A token whose user no longer exists fails")
    void aMissingAccountFails() {
        given(this.userRepository.findById(4)).willReturn(Optional.empty());

        assertThat(this.activeAccountJwtValidator.validate(tokenWithUserId(4L)).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("A token with no userId claim fails without a lookup")
    void aTokenWithNoUserIdFails() {
        assertThat(this.activeAccountJwtValidator.validate(tokenWithUserId(null)).hasErrors()).isTrue();
        verifyNoInteractions(this.userRepository);
    }

    @Test
    @DisplayName("A non-numeric userId claim fails rather than throwing")
    void aNonNumericUserIdFails() {
        assertThat(this.activeAccountJwtValidator.validate(tokenWithUserId("four")).hasErrors()).isTrue();
        verifyNoInteractions(this.userRepository);
    }

}
