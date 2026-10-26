package com.pulse.core.data;

import com.pulse.core.model.UserProfile;
import com.pulse.core.validation.ProfileValidator;

/** Command: saving and undoing a profile are independent of buttons and storage details. */
public final class ProfileCommand {
    private final Repositories.Profiles profiles;
    private final UserProfile replacement;
    private UserProfile previous;
    private boolean executed;

    public ProfileCommand(Repositories.Profiles profiles, UserProfile replacement) {
        this.profiles = profiles;
        this.replacement = replacement;
    }

    public void execute() {
        ProfileValidator.validateForStorage(replacement);
        previous = profiles.get();
        profiles.save(replacement);
        executed = true;
    }

    public boolean canUndo() {
        return executed && previous != null;
    }

    public void undo() {
        if (!canUndo()) throw new IllegalStateException("没有可撤销的档案");
        profiles.save(previous);
        executed = false;
    }
}
