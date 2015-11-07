package org.genepattern.junitutil;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of authorization permissions; Proposed replacement for private static strings declared in 
 *     org.genepattern.server.webapp.jsf.AuthorizationHelper.class
 *     
 * @author pcarr
 */
public enum Permission {
    ADMIN_JOBS("adminJobs"),
    ADMIN_MODULES("adminModules"),
    ADMIN_PIPELINES("adminPipelines"),
    ADMIN_SERVER("adminServer"),
    ADMIN_SUITES("adminSuites"),
    CREATE_MODULE("createModule"),
    CREATE_PRIVATE_PIPELINE("createPrivatePipeline"),
    CREATE_PRIVATE_SUITE("createPrivateSuite"),
    CREATE_PUBLIC_PIPELINE("createPublicPipeline"),
    CREATE_PUBLIC_SUITE("createPublicSuite");
    
    private static final Map<String,Permission> ALIAS_MAP = new HashMap<String,Permission>();
    static {
        for(final Permission p : Permission.values()) {
            ALIAS_MAP.put(p.name().toUpperCase(), p);
            ALIAS_MAP.put(p.permissionName.toUpperCase(), p);
        }
    }

    public static Permission fromPermissionName(final String permissionName) throws IllegalArgumentException {
        if (permissionName==null) {
            throw new IllegalArgumentException("permissionName==null");
        }
        final Permission p=ALIAS_MAP.get(permissionName.toUpperCase());
        if (p != null) {
            return p;
        }
        throw new IllegalArgumentException("Not a valid permissionName="+permissionName);
    }
    
    private final String permissionName;
    Permission(final String permissionName) {
        this.permissionName=permissionName;
    }
}