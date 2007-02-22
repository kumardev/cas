/*
 * Copyright 2005 The JA-SIG Collaborative. All rights reserved. See license
 * distributed with this file and available online at
 * http://www.uportal.org/license.html
 */
package org.jasig.cas.adaptors.spnego.authentication.principal;

import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.SimplePrincipal;

import junit.framework.TestCase;


public class SpnegoCredentialsTests extends TestCase {

    
    public void testToStringWithNoPrincipal() {
        final SpnegoCredentials credentials = new SpnegoCredentials(new byte[] {});
        
        assertTrue(credentials.toString().contains("null"));
    }
    
    public void testToStringWithPrincipal() {
        final SpnegoCredentials credentials = new SpnegoCredentials(new byte[] {});
        final Principal principal = new SimplePrincipal("test");
        credentials.setPrincipal(principal);
        assertEquals("test", credentials.toString());
    }
}
