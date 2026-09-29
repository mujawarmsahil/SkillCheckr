package com.skillcheckr.exception;

/**
 * Raised when the credentials are correct but the account has been deactivated by an administrator.
 *
 * <p>Kept separate from {@link UnauthorizedException} on purpose: an invalid password is an
 * authentication failure and stays 401, while a disabled account is a known user who is no longer
 * allowed in, which the client has to be able to tell apart in order to show "contact your
 * administrator" instead of "wrong password".
 */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String message) {
        super(message);
    }
}
