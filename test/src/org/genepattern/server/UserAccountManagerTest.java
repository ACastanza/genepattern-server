/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import static org.junit.Assert.*;

import org.genepattern.server.auth.AuthenticationException;
import org.junit.Test;

/**
 * Unit tests for validating usernames upon account creation.
 * 
 * @author pcarr
 */
public class UserAccountManagerTest {

    @Test
    public void testValidateUsernameExceptions() {
        String bogus = "\"test user\"";        
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "test user\"";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "'test user'";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "test user'";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
        bogus = "Tab\tcharacter";
        try {
            UserAccountManager.validateUsername(bogus);
            fail("Expecting AuthenticationException for username: "+bogus);
        }
        catch (AuthenticationException e) {
            assertEquals(AuthenticationException.Type.INVALID_USERNAME, e.getType());
        }
    }
    
    @Test
    public void testValidateUsername() {
        try {
            UserAccountManager.validateUsername("test@emailaddress.com");
            UserAccountManager.validateUsername("space character");
            UserAccountManager.validateUsername("");
            //TODO: 
            UserAccountManager.validateUsername("Newline character \n");
        }
        catch (Exception e) {
            fail(e.getLocalizedMessage());
        }
    }

}
