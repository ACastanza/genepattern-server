package org.genepattern.server.auth;

import junit.framework.TestCase;

/**
 * Unit tests for the GroupPermissions class.
 * 
 * @author pcarr
 */
public class GroupPermissionTest extends TestCase {
    private static String groupId = "testGroup";

    /**
     * Test the constructor, making sure that the int ids map to the correct enum values.
     * This validates the mapping from the database 'PERMISSION_FLAG.ID' column.
     */
    public void testGroupPermissionConstructor() {
        GroupPermission gp = new GroupPermission(groupId, -1);
        assertEquals("-1 maps to NONE", GroupPermission.Permission.NONE, gp.getPermission());
        assertEquals("0 maps to NONE", GroupPermission.Permission.NONE, new GroupPermission(groupId, 0).getPermission());
        assertEquals("1 maps to READ_WRITE", GroupPermission.Permission.READ_WRITE, new GroupPermission(groupId, 1).getPermission());
        assertEquals("2 maps to READ", GroupPermission.Permission.READ, new GroupPermission(groupId, 2).getPermission());
        assertEquals("3 maps to NONE", GroupPermission.Permission.NONE, new GroupPermission(groupId, 3).getPermission());
    }
}
