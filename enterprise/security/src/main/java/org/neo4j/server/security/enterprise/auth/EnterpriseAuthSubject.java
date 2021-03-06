/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.security.enterprise.auth;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphdb.security.AuthorizationViolationException;
import org.neo4j.kernel.api.security.AccessMode;
import org.neo4j.kernel.api.security.AuthSubject;
import org.neo4j.kernel.api.security.AuthenticationResult;
import org.neo4j.kernel.api.security.exception.InvalidArgumentsException;

import static java.util.stream.Collectors.toSet;

public class EnterpriseAuthSubject implements AuthSubject
{
    static final String SCHEMA_READ_WRITE = "schema:read,write";
    static final String READ_WRITE = "data:read,write";
    static final String READ = "data:read";

    private final EnterpriseAuthManager authManager;
    private final ShiroSubject shiroSubject;

    public static EnterpriseAuthSubject castOrFail( AuthSubject authSubject )
    {
        if ( !(authSubject instanceof EnterpriseAuthSubject) )
        {
            throw new IllegalArgumentException( "Incorrect AuthSubject type " + authSubject.getClass().getTypeName() );
        }
        return (EnterpriseAuthSubject) authSubject;
    }

    public EnterpriseAuthSubject( EnterpriseAuthManager authManager, ShiroSubject shiroSubject )
    {
        this.authManager = authManager;
        this.shiroSubject = shiroSubject;
    }

    @Override
    public void logout()
    {
        shiroSubject.logout();
    }

    @Override
    public AuthenticationResult getAuthenticationResult()
    {
        return shiroSubject.getAuthenticationResult();
    }

    @Override
    public void setPassword( String password, boolean requirePasswordChange )
            throws IOException, InvalidArgumentsException
    {
        getUserManager().setUserPassword( (String) shiroSubject.getPrincipal(), password, requirePasswordChange );

        // Make user authenticated if successful
        if ( getAuthenticationResult() == AuthenticationResult.PASSWORD_CHANGE_REQUIRED )
        {
            shiroSubject.setAuthenticationResult( AuthenticationResult.SUCCESS );
        }
    }

    @Override
    public boolean allowsProcedureWith( String[] roleNames ) throws InvalidArgumentsException
    {
        Set<String> roleNamesForUser = getUserManager().getRoleNamesForUser( name() );
        Set<String> allowedRoleNames = Stream.of( roleNames ).collect( toSet() );
        return roleNamesForUser.stream().anyMatch( allowedRoleNames::contains );
    }

    public EnterpriseUserManager getUserManager()
    {
        return authManager.getUserManager();
    }

    public boolean isAdmin()
    {
        return shiroSubject.isAuthenticated() && shiroSubject.isPermitted( "*" );
    }

    public boolean doesUsernameMatch( String username )
    {
        Object principal = shiroSubject.getPrincipal();
        return principal != null && username.equals( principal );
    }

    @Override
    public boolean allowsReads()
    {
        return shiroSubject.isAuthenticated() && shiroSubject.isPermitted( READ );
    }

    @Override
    public boolean allowsWrites()
    {
        return shiroSubject.isAuthenticated() && shiroSubject.isPermitted( READ_WRITE );
    }

    @Override
    public boolean allowsSchemaWrites()
    {
        return shiroSubject.isAuthenticated() && shiroSubject.isPermitted( SCHEMA_READ_WRITE );
    }

    @Override
    public boolean overrideOriginalMode()
    {
        return false;
    }

    @Override
    public AuthorizationViolationException onViolation( String msg )
    {
        if ( shiroSubject.getAuthenticationResult() == AuthenticationResult.PASSWORD_CHANGE_REQUIRED )
        {
            return AccessMode.Static.CREDENTIALS_EXPIRED.onViolation( msg );
        }
        else
        {
            return new AuthorizationViolationException( msg );
        }
    }

    @Override
    public String name()
    {
        String username = username();
        if ( username.isEmpty() )
        {
            return "<missing_principal>";
        }
        return username;
    }

    @Override
    public String username()
    {
        Object principal = shiroSubject.getPrincipal();
        if ( principal != null )
        {
            return principal.toString();
        }
        else
        {
            return ""; // Should never clash with a valid username
        }
    }

    ShiroSubject getShiroSubject()
    {
        return shiroSubject;
    }
}
