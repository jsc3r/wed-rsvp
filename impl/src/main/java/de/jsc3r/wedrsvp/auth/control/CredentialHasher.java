package de.jsc3r.wedrsvp.auth.control;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@ApplicationScoped
public class CredentialHasher {

    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]\\$1[2-6]\\$[./A-Za-z0-9]{53}");
    private final String dummyHash = BcryptUtil.bcryptHash("unused-login-credential", 12);

    public boolean isValidHash(String hash) {
        return hash != null && BCRYPT_HASH.matcher(hash).matches();
    }

    public boolean matches(String credential, String hash) {
        var validHash = isValidHash(hash);
        var validLength = credential.getBytes(StandardCharsets.UTF_8).length <= 72;
        // Elytron recognizes 2y, the equivalent marker for modern 2b hashes.
        var verificationHash = validHash && hash.startsWith("$2b$") ? "$2y$" + hash.substring(4) : hash;
        // Unknown accounts and legacy plaintext values still pay the password verification cost.
        var matches = BcryptUtil.matches(validLength ? credential : "overlong-credential",
                validHash ? verificationHash : dummyHash);
        return validHash && validLength && matches;
    }
}
