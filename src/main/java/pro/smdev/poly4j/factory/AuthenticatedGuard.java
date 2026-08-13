package pro.smdev.poly4j.factory;

import pro.smdev.poly4j.exception.NotAuthenticatedException;
import pro.smdev.poly4j.model.Authentication;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AuthenticatedGuard {

    private final AtomicReference<Authentication> authentication;

    protected AuthenticatedGuard(AtomicReference<Authentication> authentication) {
        this.authentication = authentication;
    }

    protected Authentication validateL1() {
        Authentication result = authentication.get();
        if (result == null || result.getSignerAddress() == null) {
            throw new NotAuthenticatedException("No data for L1 authentication");
        }

        return result;
    }

    protected Authentication validateL2() {
        Authentication result = authentication.get();
        validateL1();
        if (result.getClobSecrets() == null) {
            throw new NotAuthenticatedException("No data for L2 authentication");
        }
        return result;
    }

    protected Authentication validateBuilder() {
        Authentication result = authentication.get();
        validateL1();
        if (result.getBuilderSecrets() == null) {
            throw new NotAuthenticatedException("No data for L2 authentication");
        }
        return result;
    }

}
