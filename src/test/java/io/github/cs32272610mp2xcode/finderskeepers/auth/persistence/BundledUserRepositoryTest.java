package io.github.cs32272610mp2xcode.finderskeepers.auth.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserAccount;
import io.github.cs32272610mp2xcode.finderskeepers.auth.model.UserRole;

class BundledUserRepositoryTest {
    @Test
    void loadsBothDefaultDemoRolesCaseInsensitively() throws UserStoreException {
        BundledUserRepository repository = new BundledUserRepository();

        assertEquals(UserRole.STUDENT,
                repository.findByUsername("DEMO.STUDENT").orElseThrow().role());
        assertEquals(UserRole.DESK_OFFICER,
                repository.findByUsername("demo.officer").orElseThrow().role());
    }

    @Test
    void refusesToModifyBundledAccounts() throws UserStoreException {
        BundledUserRepository repository = new BundledUserRepository();
        UserAccount existing = repository.findByUsername("demo.student").orElseThrow();

        assertThrows(UserStoreException.class, () -> repository.add(existing));
    }
}
