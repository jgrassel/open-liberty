/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package txsync.ejblocal;

public interface TxSyncSL {
    /**
     * Test EntityManager.isJoinedToTransaction() contract with PersistenceContexts configured with
     * synchronization=SynchronizationType.SYNCHRONIZED. A SynchronizationType of SYNCHRONIZED
     * means the application does not need to invoke EntityManager.joinTransaction() for it
     * to enlist to the active transaction.
     *
     * Tests JPA 2.1 Specification Contract:
     *
     * 3.3.1 Synchronization with the Current Transaction
     * By default, a container-managed persistence context is of SynchronizationType.SYNCHRONIZED and is
     * automatically joined to the current transaction. A persistence context of SynchronizationType.UNSYNCHRONIZED
     * will not be enlisted in the current transaction, unless the EntityManager joinTransaction method is invoked.
     */
    public void testIsJoinedToTransaction001() throws Exception;

    /**
     * Test EntityManager.isJoinedToTransaction() contract with PersistenceContexts configured with
     * synchronization=SynchronizationType.UNSYNCHRONIZED. A SynchronizationType of UNSYNCHRONIZED
     * means the application does need to invoke EntityManager.joinTransaction() for it
     * to enlist to the active transaction.
     *
     * Tests JPA 2.1 Specification Contract:
     *
     * 3.3.1 Synchronization with the Current Transaction
     * By default, a container-managed persistence context is of SynchronizationType.SYNCHRONIZED and is
     * automatically joined to the current transaction. A persistence context of SynchronizationType.UNSYNCHRONIZED
     * will not be enlisted in the current transaction, unless the EntityManager joinTransaction method is invoked.
     */
    public void testIsJoinedToTransaction002() throws Exception;
}
