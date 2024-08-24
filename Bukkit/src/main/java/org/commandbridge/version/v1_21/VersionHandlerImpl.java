package org.commandbridge.version.v1_21;

import org.commandbridge.api.VersionSpecificHandler;

public class VersionHandlerImpl implements VersionSpecificHandler {
    @Override
    public void handle() {
        System.out.println("Handling version specific code for 1.21");
    }
}
