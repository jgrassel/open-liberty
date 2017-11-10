/**
 *
 */
package txsync.logic;

import java.io.PrintStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.transaction.UserTransaction;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;

import txsync.model.TxSyncEntity;

/**
 *
 */
public class TxSynchronizationTestLogic {
    private final PrintStream ps;

    private long nextId = System.currentTimeMillis();

    public TxSynchronizationTestLogic(PrintStream ps) {
        this.ps = ps;
    }

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
    public void testIsJoinedToTransaction001(EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testIsJoinedToTransaction001 testlogic ...");

        try {
            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin Transaction
            ps.println("Begin global transaction ...");
            tx.begin();
            Assert.assertTrue("Assert that there is an active transaction.", isTransactionActive(tx));
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            ps.println("Commit global transaction ...");
            tx.commit();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin a transaction
            ps.println("Begin global transaction ...");
            tx.begin();
            Assert.assertTrue("Assert that there is an active transaction.", isTransactionActive(tx));
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Rollback a transaction
            ps.println("Rollback global transaction ...");
            tx.rollback();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());
        } finally {
            ps.println("Ending testIsJoinedToTransaction001 testlogic ...");
        }

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
    public void testIsJoinedToTransaction002(EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testIsJoinedToTransaction002 testlogic ...");

        try {
            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin Transaction
            ps.println("Begin global transaction ...");
            tx.begin();
            Assert.assertTrue("Assert that there is an active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Join Transaction Manually
            em.joinTransaction();
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            ps.println("Commit global transaction ...");
            tx.commit();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Begin a transaction
            ps.println("Begin global transaction ...");
            tx.begin();
            Assert.assertTrue("Assert that there is an active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Join Transaction Manually
            em.joinTransaction();
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Rollback a transaction
            ps.println("Rollback global transaction ...");
            tx.rollback();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());
        } finally {
            ps.println("Ending testIsJoinedToTransaction002 testlogic ...");
        }
    }

    /**
     * Simple CRUD Test #001
     *
     * Verify that CRUD operations can be executed successfully.
     * Variant: Test @PersistenceContext with transaction=SYNCHRONIZED
     */
    public void testCRUD001_SYNC(EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testCRUD001_SYNC testlogic ...");

        try {
            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            // Begin Transaction
            ps.println("Begin global transaction ...");
            tx.begin();
            Assert.assertTrue("Assert that there is an active transaction.", isTransactionActive(tx));

            ps.println("Persisting " + newEntity + " ...");
            em.persist(newEntity);
            Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            ps.println("Commit global transaction ...");
            tx.commit();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            // Clear persistence context
            ps.println("Clearing persistence context ...");
            em.clear();

            // Find the entity
            ps.println("Finding the entity ...");
            final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntity);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntity);
            Assert.assertEquals("Simple String", findEntity.getStrData());
        } finally {
            ps.println("Ending testCRUD001_SYNC testlogic ...");
        }
    }

    /**
     * Simple CRUD Test #001B
     *
     * Verify that CRUD operations can be executed successfully.
     * Variant: Test @PersistenceContext with transaction=UNSYNCHRONIZED
     */
    public void testCRUD001_UNSYNC(EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testCRUD001_UNSYNC testlogic ...");

        try {
            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            // Begin Transaction
            ps.println("Begin global transaction ...");
            tx.begin();
            Assert.assertTrue("Assert that there is an active transaction.", isTransactionActive(tx));
            em.joinTransaction();
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            ps.println("Persisting " + newEntity + " ...");
            em.persist(newEntity);
            Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Commit a transaction
            ps.println("Commit global transaction ...");
            tx.commit();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            // Clear persistence context
            ps.println("Clearing persistence context ...");
            em.clear();

            // Find the entity
            ps.println("Finding the entity ...");
            final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntity);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntity);
            Assert.assertEquals("Simple String", findEntity.getStrData());
        } finally {
            ps.println("Ending testCRUD001_UNSYNC testlogic ...");
        }
    }

    /**
     * Simple CRUD Test #002
     *
     * Verify that CRUD operations are not persisted to the database with
     *
     * @PersistenceContext(transaction=UNSYNCHRONIZED) when em.joinTransaction is not invoked.
     */
    public void testCRUD002(EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testCRUD002 testlogic ...");

        try {
            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            // Begin Transaction
            ps.println("Begin global transaction ...");
            tx.begin();
            Assert.assertTrue("Assert that there is an active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            ps.println("Persisting " + newEntity + " ...");
            em.persist(newEntity);
            Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Commit a transaction
            ps.println("Commit global transaction ...");
            tx.commit();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that the new entity is no longer managed.", em.contains(newEntity));

            // Clear persistence context
            ps.println("Clearing persistence context ...");
            em.clear();

            // Find the entity
            ps.println("Finding the entity (should return null) ...");
            final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity);
            Assert.assertNull("Assert that em.find() did return null.", findEntity);
        } finally {
            ps.println("Ending testCRUD002 testlogic ...");
        }
    }

    /**
     * Test Flush #001
     *
     * Verify that the em.flush() operation requires an active JTA transaction with a SYNCHRONIZED EntityManager.
     */
    public void testFlush001_SYNC(EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testFlush001_SYNC testlogic ...");

        try {
            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            try {
                em.flush();
                Assert.fail("No Exception was thrown with flush() outside of transaction.");
            } catch (Exception e) {
                Assert.assertThat(e, getExceptionChainMatcher(javax.persistence.TransactionRequiredException.class));
            }

            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            // Begin Transaction
            ps.println("Begin global transaction ...");
            tx.begin();

            ps.println("Persisting " + newEntity + " ...");
            em.persist(newEntity);
            Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            // Call flush(), no exception should be thrown.
            em.flush();

            // Commit a transaction
            ps.println("Commit global transaction ...");
            tx.commit();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that the new entity is no longer managed.", em.contains(newEntity));

        } finally {
            ps.println("Ending testFlush001_SYNC testlogic ...");
        }
    }

    /**
     * Test Flush #001
     *
     * Verify that the em.flush() operation requires an active JTA transaction and that unsynchronized
     * entitymanagers must be joined to the transaction.
     */
    public void testFlush001_UNSYNC(EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testFlush001_UNSYNC testlogic ...");
        try {
            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            try {
                em.flush();
                Assert.fail("No Exception was thrown with flush() outside of transaction.");
            } catch (Exception e) {
                Assert.assertThat(e, getExceptionChainMatcher(javax.persistence.TransactionRequiredException.class));
            }

            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            // Begin Transaction
            ps.println("Begin global transaction ...");
            tx.begin();

            ps.println("Persisting " + newEntity + " ...");
            em.persist(newEntity);
            Assert.assertTrue("Assert that the new entity is managed.", em.contains(newEntity));
            Assert.assertFalse("Assert that em.isJoinedToTransaction() returns false.", em.isJoinedToTransaction());

            // Call flush(), exception should be thrown because the EntityManager is not joined to the tx.
            try {
                em.flush();
                Assert.fail("No Exception was thrown with flush() outside of transaction.");
            } catch (Exception e) {
                Assert.assertThat(e, getExceptionChainMatcher(javax.persistence.TransactionRequiredException.class));
            }

            tx.rollback();
            tx.begin();

            // Join the EntityManager to the transaction
            em.joinTransaction();
            Assert.assertTrue("Assert that em.isJoinedToTransaction() returns true.", em.isJoinedToTransaction());

            ps.println("Persisting " + newEntity + " ...");
            em.persist(newEntity);

            // Call flush(), no exception should be thrown.
            em.flush();
            em.clear();

            // Commit a transaction
            ps.println("Commit global transaction ...");
            tx.commit();
            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));
            Assert.assertFalse("Assert that the new entity is no longer managed.", em.contains(newEntity));

            // Find the entity
            ps.println("Finding the entity ...");
            final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntity);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntity);
            Assert.assertEquals("Simple String", findEntity.getStrData());
        } finally {
            ps.println("Ending testFlush001_UNSYNC testlogic ...");
        }
    }

    /**
     * Test em.find() with Lock Type: NONE
     *
     * Verify that em.find() with lock type NONE can be executed regardless of tx synchronicity or the
     * presence of an active JTA transaction.
     */
    @Test
    public void testFindWithLock001_SYNC(EntityManagerFactory emf, EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testFindWithLock001_SYNC testlogic ...");
        EntityManager emRL = null;

        try {
            emRL = emf.createEntityManager();

            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            // Populate the database
            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            emRL.getTransaction().begin();
            emRL.persist(newEntity);;
            emRL.getTransaction().commit();
            emRL.clear();

            // Verify that em.find(LockTypeMode.NONE) does not require a JTA tran to work, regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.NONE) does not require a JTA tran to work.");
            final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntity);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntity);
            Assert.assertEquals("Simple String", findEntity.getStrData());
            em.clear();

            // Verify that em.find(LockTypeMode.NONE) operates within the bounds of a JTA transaction , regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.NONE) operates fine within a JTA tran.");
            tx.begin();
            final TxSyncEntity findEntityUnderJTA = em.find(TxSyncEntity.class, identity);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntityUnderJTA);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntityUnderJTA);
            Assert.assertTrue("Assert entity is managed by the persistence context.", em.contains(findEntityUnderJTA));
            Assert.assertEquals("Simple String", findEntityUnderJTA.getStrData());
            tx.rollback();
            em.clear();
        } finally {
            ps.println("Ending testFindWithLock001_SYNC testlogic ...");
            try {
                if (emRL != null) {
                    if (emRL.getTransaction().isActive()) {
                        emRL.getTransaction().rollback();
                    }
                    emRL.close();
                }
            } catch (Throwable t) {
            }
        }
    }

    /**
     * Test em.find() with Lock Type: NONE
     *
     * Verify that em.find() with lock type NONE can be executed regardless of tx synchronicity or the
     * presence of an active JTA transaction.
     */
    @Test
    public void testFindWithLock001_UNSYNC(EntityManagerFactory emf, EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testFindWithLock001_UNSYNC testlogic ...");
        EntityManager emRL = null;

        try {
            emRL = emf.createEntityManager();

            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            // Populate the database
            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            emRL.getTransaction().begin();
            emRL.persist(newEntity);;
            emRL.getTransaction().commit();
            emRL.clear();

            // Verify that em.find(LockTypeMode.NONE) does not require a JTA tran to work, regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.NONE) does not require a JTA tran to work.");
            final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntity);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntity);
            Assert.assertEquals("Simple String", findEntity.getStrData());
            em.clear();

            // Verify that em.find(LockTypeMode.NONE) operates within the bounds of a JTA transaction , regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.NONE) operates fine within a JTA tran.");
            tx.begin();
            final TxSyncEntity findEntityUnderJTA = em.find(TxSyncEntity.class, identity);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntityUnderJTA);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntityUnderJTA);
            Assert.assertTrue("Assert entity is managed by the persistence context.", em.contains(findEntityUnderJTA));
            Assert.assertEquals("Simple String", findEntityUnderJTA.getStrData());
            tx.rollback();
            em.clear();
        } finally {
            ps.println("Ending testFindWithLock001_UNSYNC testlogic ...");
            try {
                if (emRL != null) {
                    if (emRL.getTransaction().isActive()) {
                        emRL.getTransaction().rollback();
                    }
                    emRL.close();
                }
            } catch (Throwable t) {
            }
        }
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC
     *
     * Verify that em.find() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testFindWithLock002_SYNC(EntityManagerFactory emf, EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testFindWithLock002_SYNC testlogic ...");
        EntityManager emRL = null;
        try {
            emRL = emf.createEntityManager();

            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            // Populate the database
            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            emRL.getTransaction().begin();
            emRL.persist(newEntity);;
            emRL.getTransaction().commit();
            emRL.clear();

            // Verify that em.find(LockTypeMode.OPTIMISTIC) does require a JTA tran to work, regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.OPTIMISTIC) does require a JTA tran to work.");
            try {
                final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity, LockModeType.OPTIMISTIC);
                Assert.fail("No Exeption was thrown");
            } catch (Exception e) {
                Assert.assertThat(e, getExceptionChainMatcher(javax.persistence.TransactionRequiredException.class));
            }

            em.clear();

            // Verify that em.find(LockTypeMode.OPTIMISTIC) operates within the bounds of a JTA transaction , regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.OPTIMISTIC) operates (actually is required) within a JTA tran.");
            tx.begin();
            final TxSyncEntity findEntityUnderJTA = em.find(TxSyncEntity.class, identity, LockModeType.OPTIMISTIC);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntityUnderJTA);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntityUnderJTA);
            Assert.assertTrue("Assert entity is managed by the persistence context.", em.contains(findEntityUnderJTA));
            Assert.assertEquals("Simple String", findEntityUnderJTA.getStrData());
            tx.rollback();
            em.clear();
        } finally {
            ps.println("Ending testFindWithLock002_SYNC testlogic ...");
            try {
                if (emRL != null) {
                    if (emRL.getTransaction().isActive()) {
                        emRL.getTransaction().rollback();
                    }
                    emRL.close();
                }
            } catch (Throwable t) {
            }
        }
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC
     *
     * Verify that em.find() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testFindWithLock002_UNSYNC(EntityManagerFactory emf, EntityManager em, UserTransaction tx) throws Exception {
        ps.println("Starting testFindWithLock002_UNSYNC testlogic ...");
        EntityManager emRL = null;
        try {
            emRL = emf.createEntityManager();

            // Clean up any active transaction
            ps.println("Rollback global transaction ...");
            if (isTransactionActive(tx)) {
                tx.rollback();
            }

            Assert.assertFalse("Assert that there is no active transaction.", isTransactionActive(tx));

            // Populate the database
            final long identity = nextId++;
            ps.println("Creating TxSyncEntity with identity " + identity);
            final TxSyncEntity newEntity = new TxSyncEntity();
            newEntity.setId(identity);
            newEntity.setStrData("Simple String");

            emRL.getTransaction().begin();
            emRL.persist(newEntity);;
            emRL.getTransaction().commit();
            emRL.clear();

            // Verify that em.find(LockTypeMode.OPTIMISTIC) does require a JTA tran to work, regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.OPTIMISTIC) does require a JTA tran to work.");
            try {
                final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity, LockModeType.OPTIMISTIC);
                Assert.fail("No Exeption was thrown");
            } catch (Exception e) {
                Assert.assertThat(e, getExceptionChainMatcher(javax.persistence.TransactionRequiredException.class));
            }

            em.clear();

            // Verify that em.find(LockTypeMode.OPTIMISTIC) operates within the bounds of a JTA transaction , regardless of TX Synchronicity
            ps.println("Verify that em.find(LockTypeMode.OPTIMISTIC) operates (actually is required) within a JTA tran.");
            tx.begin();
            em.joinTransaction();
            final TxSyncEntity findEntityUnderJTA = em.find(TxSyncEntity.class, identity, LockModeType.OPTIMISTIC);
            Assert.assertNotNull("Assert that em.find() did not return null.", findEntityUnderJTA);
            Assert.assertNotSame("Assert that em.find() did not return original entity.", newEntity, findEntityUnderJTA);
            Assert.assertTrue("Assert entity is managed by the persistence context.", em.contains(findEntityUnderJTA));
            Assert.assertEquals("Simple String", findEntityUnderJTA.getStrData());
            tx.rollback();
            em.clear();

            // Verify that em.find(LockTypeMode.OPTIMISTIC) with an unsynchronized entity manager operates
            // requires both an active JTA transaction and that the em to be joined to the transaction
            // to operate
            ps.println("Verify that em.find(LockTypeMode.OPTIMISTIC) does require a JTA tran and the em joined to the JTA tran to work.");
            try {
                tx.begin();
                final TxSyncEntity findEntity = em.find(TxSyncEntity.class, identity, LockModeType.OPTIMISTIC);
                Assert.fail("No Exeption was thrown");
            } catch (Exception e) {
                Assert.assertThat(e, getExceptionChainMatcher(javax.persistence.TransactionRequiredException.class));
            } finally {
                tx.rollback();
            }
            em.clear();
        } finally {
            ps.println("Ending testFindWithLock002_UNSYNC testlogic ...");
            try {
                if (emRL != null) {
                    if (emRL.getTransaction().isActive()) {
                        emRL.getTransaction().rollback();
                    }
                    emRL.close();
                }
            } catch (Throwable t) {
            }
        }
    }

    /**
     * Test em.find() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testFindWithLock003(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.find() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testFindWithLock004(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.find() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void testFindWithLock005(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.find() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.find() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    @Test
    public void testFindWithLock006(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.find() with Lock Type: READ
     *
     * Verify that em.find() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testFindWithLock007(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.find() with Lock Type: WRITE
     *
     * Verify that em.find() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testFindWithLock008(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: NONE
     *
     * Verify that em.lock() with lock type NONE requires an active JTA transaction and that the
     * EntityManager is joined to the transaction.
     */
    @Test
    public void testEmLock001(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC
     *
     * Verify that em.lock() with lock type OPTIMISTIC must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testEmLock002(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: OPTIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type OPTIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testEmLock003(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_FORCE_INCREMENT
     *
     * Verify that em.lock() with lock type PESSIMISTIC_FORCE_INCREMENT must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testEmLock004(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_READ
     *
     * Verify that em.lock() with lock type PESSIMISTIC_READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testEmLock005(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: PESSIMISTIC_WRITE
     *
     * Verify that em.lock() with lock type PESSIMISTIC_WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testEmLock006(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: READ
     *
     * Verify that em.lock() with lock type READ must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testEmLock007(EntityManager em, UserTransaction tx) throws Exception {

    }

    /**
     * Test em.lock() with Lock Type: WRITE
     *
     * Verify that em.lock() with lock type WRITE must be executed within a JTA transaction boundary
     * and that unsynchronized EntityManagers must be joined to the JTA transaction.
     */
    public void testEmLock008(EntityManager em, UserTransaction tx) throws Exception {

    }

    public boolean isTransactionActive(UserTransaction tx) throws Exception {
        int status = tx.getStatus();
        if (status == javax.transaction.Status.STATUS_NO_TRANSACTION || status == javax.transaction.Status.STATUS_UNKNOWN)
            return false;
        else
            return true;
    }

    @SuppressWarnings("rawtypes")
    public Matcher getExceptionChainMatcher(final Class t) {
        return new BaseMatcher() {
            protected Class<?> expectedThrowableClass = t;

            @Override
            public boolean matches(Object obj) {
                if (obj == null) {
                    return (expectedThrowableClass == null);
                }

                if (!(obj instanceof Throwable)) {
                    return false;
                }

                Throwable t = (Throwable) obj;
                while (t != null) {
                    if (expectedThrowableClass.equals(t.getClass())) {
                        return true;
                    }

                    t = t.getCause();
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(expectedThrowableClass.toString());
            }

        };
    }
}
