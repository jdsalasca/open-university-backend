package co.edu.uptc.universiry.identity.application;

public class IdentityNotRegisteredException extends RuntimeException {

    public IdentityNotRegisteredException() {
        super("Authenticated identity has not been registered");
    }
}
