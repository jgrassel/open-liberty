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

package txsync.web;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import txsync.logic.TxSynchronizationTestLogic;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TxSyncTestServlet")
public class TxSyncTestServlet extends FATServlet {
    @PersistenceContext(unitName = "TxSync",
                        type = PersistenceContextType.TRANSACTION,
                        synchronization = SynchronizationType.SYNCHRONIZED)
    private EntityManager emCMTSTxSync;

    @PersistenceContext(unitName = "TxSync",
                        type = PersistenceContextType.TRANSACTION,
                        synchronization = SynchronizationType.UNSYNCHRONIZED)
    private EntityManager emCMTSTxUnSync;

    @PersistenceUnit(unitName = "TxSync_RL")
    private EntityManagerFactory emf;

    @Resource
    private UserTransaction tx;

    private TxSynchronizationTestLogic testlogic = new TxSynchronizationTestLogic(System.out);

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
    @Test
    public void jpa21_txsync_web_testIsJoinedToTransaction001() throws Exception {
        testlogic.testIsJoinedToTransaction001(emCMTSTxSync, tx);
    }

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
    @Test
    public void jpa21_txsync_web_testIsJoinedToTransaction002() throws Exception {
        testlogic.testIsJoinedToTransaction002(emCMTSTxUnSync, tx);
    }

    /**
     * Simple CRUD Test #001-SYNC
     *
     * Verify that CRUD operations can be executed successfully.
     * Variant: Test @PersistenceContext with transaction=SYNCHRONIZED
     */
    @Test
    public void jpa21_txsync_web_testCRUD001_SYNC() throws Exception {
        testlogic.testCRUD001_SYNC(emCMTSTxSync, tx);
    }

    /**
     * Simple CRUD Test #001-UNSYNC
     *
     * Verify that CRUD operations can be executed successfully.
     * Variant: Test @PersistenceContext with transaction=UNSYNCHRONIZED
     */
    @Test
    public void jpa21_txsync_web_testCRUD001_UNSYNC() throws Exception {
        testlogic.testCRUD001_UNSYNC(emCMTSTxUnSync, tx);
    }

    /**
     * Simple CRUD Test #002
     *
     * Verify that CRUD operations are not persisted to the database with
     *
     * @PersistenceContext(transaction=UNSYNCHRONIZED) when em.joinTransaction is not invoked.
     */
    @Test
    public void jpa21_txsync_web_testCRUD002() throws Exception {
        testlogic.testCRUD002(emCMTSTxUnSync, tx);
    }

    /**
     * Test Flush #001
     *
     * Verify that the em.flush() operation requires an active JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFlush001_SYNC() throws Exception {
        testlogic.testFlush001_SYNC(emCMTSTxSync, tx);
    }

    /**
     * Test Flush #001
     *
     * Verify that the em.flush() operation requires an active JTA transaction and that unsynchronized
     * entitymanagers must be joined to the transaction.
     */
    @Test
    public void jpa21_txsync_web_testFlush001_UNSYNC() throws Exception {
        testlogic.testFlush001_UNSYNC(emCMTSTxUnSync, tx);
    }

    /**
     * Test em.find() with Lock Type: NONE
     *
     * Verify that em.find() with lock type NONE can be executed regardless of tx synchronicity or the
     * presence of an active JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock001_SYNC() throws Exception {
        testlogic.testFindWithLock001_SYNC(emf, emCMTSTxSync, tx);
    }

    /**
     * Test em.find() with Lock Type: NONE
     *
     * Verify that em.find() with lock type NONE can be executed regardless of tx synchronicity or the
     * presence of an active JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock001_UNSYNC() throws Exception {
        testlogic.testFindWithLock001_UNSYNC(emf, emCMTSTxUnSync, tx);
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC
     *
     * Verify that em.find() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock002_SYNC() throws Exception {
        testlogic.testFindWithLock002_SYNC(emf, emCMTSTxSync, tx);
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC
     *
     * Verify that em.find() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock002_UNSYNC() throws Exception {
        testlogic.testFindWithLock002_UNSYNC(emf, emCMTSTxUnSync, tx);
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock003() throws Exception {

    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock004() throws Exception {

    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.find() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock005() throws Exception {

    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.find() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock006() throws Exception {

    }

    /**
     * Test em.find() with Lock Type: READ
     *
     * Verify that em.find() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock007() throws Exception {

    }

    /**
     * Test em.find() with Lock Type: WRITE
     *
     * Verify that em.find() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testFindWithLock008() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: NONE
     *
     * Verify that em.lock() with lock type NONE requires an active JTA transaction and that the
     * EntityManager is joined to the transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock001() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC
     *
     * Verify that em.lock() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock002() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock003() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock004() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.lock() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock005() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.lock() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock006() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: READ
     *
     * Verify that em.lock() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock007() throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: WRITE
     *
     * Verify that em.lock() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void jpa21_txsync_web_testEmLock008() throws Exception {

    }

}
