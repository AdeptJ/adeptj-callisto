/*
###############################################################################
#                                                                             # 
#    Copyright 2016, AdeptJ (http://www.adeptj.com)                           #
#                                                                             #
#    Licensed under the Apache License, Version 2.0 (the "License");          #
#    you may not use this file except in compliance with the License.         #
#    You may obtain a copy of the License at                                  #
#                                                                             #
#        http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                             #
#    Unless required by applicable law or agreed to in writing, software      #
#    distributed under the License is distributed on an "AS IS" BASIS,        #
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
#    See the License for the specific language governing permissions and      #
#    limitations under the License.                                           #
#                                                                             #
###############################################################################
*/

package com.adeptj.runtime.server;

import com.typesafe.config.Config;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * Simple IdentityManager implementation that does the authentication from provisioning file or from
 * the OsgiManager.config file if it is created when password is updated from OSGi Web Console.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
final class SimpleIdentityManager implements IdentityManager {

    private static final String KEY_USER_ROLES_MAPPING = "common.user-roles-mapping";

    /**
     * User to Roles mapping.
     */
    private Map<String, List<String>> userRolesMapping;

    private CredentialMatcher matcher;

    @SuppressWarnings("unchecked")
    SimpleIdentityManager(Config cfg) {
        this.userRolesMapping = new HashMap<>(Map.class.cast(cfg.getObject(KEY_USER_ROLES_MAPPING).unwrapped()));
        this.matcher = new CredentialMatcher();
    }

    /**
     * In our case, this method is called by CachedAuthenticatedSessionMechanism.
     * <p>
     * This is queried on each request for protected resources after user is successfully logged in.
     */
    @Override
    public Account verify(Account account) {
        return this.userRolesMapping.entrySet()
                .stream()
                .anyMatch(this.verifyAccountPredicate(account)) ? account : null;
    }

    /**
     * Called by FormAuthenticationMechanism when user submits the login form.
     */
    @Override
    public Account verify(String id, Credential credential) {
        return this.userRolesMapping.entrySet().stream()
                .filter(this.credentialMatchPredicate(id, credential))
                .map(entry -> new SimpleAccount(new SimplePrincipal(entry.getKey()), new HashSet<>(entry.getValue())))
                .findFirst()
                .orElse(null);
    }

    /**
     * Used here:
     * <p>
     * 1. ClientCertAuthenticationMechanism.
     * 2. GSSAPIAuthenticationMechanism
     * <p>
     * We are not covering both the use cases therefore returning a null.
     */
    @Override
    public Account verify(Credential credential) {
        return null;
    }

    private Predicate<Entry<String, List<String>>> verifyAccountPredicate(Account account) {
        return (Entry<String, List<String>> entry) -> entry.getKey()
                .equals(account.getPrincipal().getName()) && entry.getValue().containsAll(account.getRoles());
    }

    private Predicate<Entry<String, List<String>>> credentialMatchPredicate(String id, Credential cred) {
        PasswordCredential credential = (PasswordCredential) cred;
        return entry -> StringUtils.equals(entry.getKey(), id) && ArrayUtils.isNotEmpty(credential.getPassword())
                && this.matcher.match(entry.getKey(), new String(credential.getPassword()));
    }
}
