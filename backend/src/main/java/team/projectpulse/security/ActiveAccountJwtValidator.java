package team.projectpulse.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

/**
 * Fails a token whose user is missing or deactivated, so an open session ends at its next request rather than
 * when its two hours run out (FR-SEC-active-account). Signature and expiry are still checked by the default
 * validators this is composed with on the {@code jwtDecoder()} bean.
 *
 * <p>A failure here surfaces as an {@code InvalidBearerTokenException}, which the resource server hands to
 * {@code CustomBearerTokenAuthenticationEntryPoint} and so to a {@code 401} in the {@code Result} envelope.
 *
 * <p>Like every guard in the filter chain, this returns a failure for anything it cannot resolve, a missing or
 * non-numeric {@code userId} claim included, and never throws: an exception here would escape
 * {@code ExceptionHandlerAdvice} as a {@code 500}.
 */
@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2TokenValidatorResult INACTIVE_ACCOUNT = OAuth2TokenValidatorResult.failure(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The account this token was issued to is missing or deactivated.", null));

    private final UserRepository userRepository;


    public ActiveAccountJwtValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Integer userId = readUserId(jwt);
        if (userId == null) {
            return INACTIVE_ACCOUNT;
        }
        return this.userRepository.findById(userId)
                .filter(PeerEvaluationUser::isEnabled)
                .map(user -> OAuth2TokenValidatorResult.success())
                .orElse(INACTIVE_ACCOUNT);
    }

    /**
     * @return the token's {@code userId} claim as an id, or {@code null} if it is missing or not an integer.
     * {@code JwtProvider} writes it as a number, which the decoder reads back as a {@code Long}.
     */
    private static @Nullable Integer readUserId(Jwt jwt) {
        Object claim = jwt.getClaims().get("userId");
        if (claim instanceof Integer i) {
            return i;
        }
        if (claim instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return l.intValue();
        }
        if (claim instanceof String s) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

}
