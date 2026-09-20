package io.github.cs32272610mp2xcode.finderskeepers.auth.provisioning;

import java.io.Console;
import java.nio.file.Path;
import java.util.Arrays;

import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.JsonUserRepository;
import io.github.cs32272610mp2xcode.finderskeepers.auth.persistence.UserStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.auth.security.Pbkdf2PasswordHasher;

/** Command-line utility for one-time local account provisioning. */
public final class AccountProvisioningTool {
    private static final int ARGUMENT_COUNT = 4;

    private AccountProvisioningTool() {
    }

    /**
     * Provisions one account while reading its password through the masked console.
     *
     * @param args store path, user ID, username, and canonical role
     * @throws UserStoreException when the store cannot be safely updated
     */
    public static void main(String[] args) throws UserStoreException {
        if (args.length != ARGUMENT_COUNT) {
            System.err.println("Usage: AccountProvisioningTool <store> <user-id> <username> <role>");
            return;
        }
        Console console = System.console();
        if (console == null) {
            System.err.println("A secure interactive console is required.");
            return;
        }
        char[] password = console.readPassword("Password: ");
        try {
            AccountProvisioner provisioner = new AccountProvisioner(
                    new JsonUserRepository(Path.of(args[0])), new Pbkdf2PasswordHasher());
            provisioner.provision(args[1], args[2], args[3], password);
            System.out.println("Local account created successfully.");
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
