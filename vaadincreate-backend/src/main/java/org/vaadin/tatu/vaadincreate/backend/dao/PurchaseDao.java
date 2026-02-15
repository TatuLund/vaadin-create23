package org.vaadin.tatu.vaadincreate.backend.dao;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.data.Purchase;
import org.vaadin.tatu.vaadincreate.backend.data.PurchaseStatus;
import org.vaadin.tatu.vaadincreate.backend.data.User;
import org.vaadin.tatu.vaadincreate.backend.data.UserSupervisor;

/**
 * Data access object for managing purchases. Provides CRUD operations and
 * queries for Purchase entities.
 */
@NullMarked
@SuppressWarnings("java:S1602")
public class PurchaseDao {

    /**
     * Saves or updates a purchase.
     *
     * @param purchase
     *            the purchase to save or update
     * @return the persisted purchase
     */
    public Purchase updatePurchase(Purchase purchase) {
        Objects.requireNonNull(purchase, "Purchase must not be null");
        logger.info("Persisting Purchase: ({})", purchase.getId());
        return HibernateUtil.saveOrUpdate(purchase);
    }

    /**
     * Retrieves a Purchase by its ID.
     *
     * @param id
     *            the ID of the purchase
     * @return the Purchase, or null if not found
     */
    @Nullable
    public Purchase getPurchase(Integer id) {
        Objects.requireNonNull(id, "Purchase ID must not be null");
        logger.info("Fetching Purchase: ({})", id);
        return HibernateUtil.inSession(session -> {
            @Nullable
            Purchase purchase = session.get(Purchase.class, id);
            return purchase;
        });
    }

    /**
     * Finds purchases by requester.
     *
     * @param requester
     *            the user who created the purchases
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @return list of purchases
     */
    public List<@NonNull Purchase> findByRequester(User requester, int offset,
            int limit) {
        Objects.requireNonNull(requester, "Requester must not be null");
        logger.info("Fetching Purchases by requester: ({})", requester.getId());
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "select p from Purchase p where p.requester = :requester order by p.createdAt desc",
                    Purchase.class).setParameter("requester", requester)
                    .setFirstResult(offset).setMaxResults(limit).list();
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of findByRequester is null");
        }
        return result;
    }

    /**
     * Counts purchases by requester.
     *
     * @param requester
     *            the user who created the purchases
     * @return the count of purchases
     */
    public long countByRequester(User requester) {
        Objects.requireNonNull(requester, "Requester must not be null");
        logger.info("Counting Purchases by requester: ({})", requester.getId());
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "select count(p) from Purchase p where p.requester = :requester",
                    Long.class).setParameter("requester", requester)
                    .uniqueResult();
        });
        return result != null ? result : 0L;
    }

    /**
     * Finds purchases by approver and status.
     *
     * @param approver
     *            the approver user
     * @param status
     *            the purchase status to filter by
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @return list of purchases
     */
    public List<@NonNull Purchase> findByApproverAndStatus(User approver,
            PurchaseStatus status, int offset, int limit) {
        Objects.requireNonNull(approver, "Approver must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        logger.info("Fetching Purchases by approver: ({}) and status: {}",
                approver.getId(), status);
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "select p from Purchase p where p.approver = :approver and p.status = :status order by p.createdAt desc",
                    Purchase.class).setParameter("approver", approver)
                    .setParameter("status", status).setFirstResult(offset)
                    .setMaxResults(limit).list();
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of findByApproverAndStatus is null");
        }
        return result;
    }

    /**
     * Counts purchases by approver and status.
     *
     * @param approver
     *            the approver user
     * @param status
     *            the purchase status to filter by
     * @return the count of purchases
     */
    public long countByApproverAndStatus(User approver, PurchaseStatus status) {
        Objects.requireNonNull(approver, "Approver must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        logger.info("Counting Purchases by approver: ({}) and status: {}",
                approver.getId(), status);
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "select count(p) from Purchase p where p.approver = :approver and p.status = :status",
                    Long.class).setParameter("approver", approver)
                    .setParameter("status", status).uniqueResult();
        });
        return result != null ? result : 0L;
    }

    /**
     * Finds all purchases with pagination.
     *
     * @param offset
     *            the starting offset for pagination
     * @param limit
     *            the maximum number of results
     * @return list of all purchases
     */
    public List<@NonNull Purchase> findAll(int offset, int limit) {
        logger.info("Fetching all Purchases");
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "select p from Purchase p order by p.createdAt desc",
                    Purchase.class).setFirstResult(offset).setMaxResults(limit)
                    .list();
        });
        if (result == null) {
            throw new IllegalStateException("Result of findAll is null");
        }
        return result;
    }

    /**
     * Counts all purchases.
     *
     * @return the total count of purchases
     */
    public long countAll() {
        logger.info("Counting all Purchases");
        var result = HibernateUtil.inSession(session -> {
            return session
                    .createQuery("select count(p) from Purchase p", Long.class)
                    .uniqueResult();
        });
        return result != null ? result : 0L;
    }

    /**
     * Finds the default supervisor for an employee.
     *
     * @param employee
     *            the employee user
     * @return the supervisor, or null if not found
     */
    @Nullable
    public User findSupervisorForEmployee(User employee) {
        Objects.requireNonNull(employee, "Employee must not be null");
        logger.info("Fetching supervisor for employee: ({})", employee.getId());
        return HibernateUtil.inSession(session -> {
            @Nullable
            UserSupervisor mapping = session.createQuery(
                    "select us from UserSupervisor us where us.employee = :employee",
                    UserSupervisor.class).setParameter("employee", employee)
                    .uniqueResult();
            return mapping != null ? mapping.getSupervisor() : null;
        });
    }

    /**
     * Finds purchases for a requester that have been decided (COMPLETED,
     * REJECTED, or CANCELLED) since a given timestamp.
     *
     * @param requester
     *            the user who created the purchases
     * @param since
     *            the timestamp to filter from
     * @return list of decided purchases since the given time
     */
    public List<@NonNull Purchase> findRecentlyDecidedByRequester(
            User requester, java.time.Instant since) {
        Objects.requireNonNull(requester, "Requester must not be null");
        Objects.requireNonNull(since, "Since timestamp must not be null");
        logger.info(
                "Fetching recently decided Purchases by requester: ({}) since: {}",
                requester.getId(), since);
        var result = HibernateUtil.inSession(session -> {
            return session.createQuery(
                    "select p from Purchase p where p.requester = :requester and p.status in (:completed, :rejected, :cancelled) and p.decidedAt > :since order by p.decidedAt desc",
                    Purchase.class).setParameter("requester", requester)
                    .setParameter("completed", PurchaseStatus.COMPLETED)
                    .setParameter("rejected", PurchaseStatus.REJECTED)
                    .setParameter("cancelled", PurchaseStatus.CANCELLED)
                    .setParameter("since", since).list();
        });
        if (result == null) {
            throw new IllegalStateException(
                    "Result of findRecentlyDecidedByRequester is null");
        }
        return result;
    }

    /**
     * Saves or updates a user-supervisor mapping.
     *
     * @param mapping
     *            the mapping to save
     * @return the persisted mapping
     */
    public UserSupervisor updateUserSupervisor(UserSupervisor mapping) {
        Objects.requireNonNull(mapping,
                "UserSupervisor mapping must not be null");
        logger.info("Persisting UserSupervisor mapping: ({})", mapping.getId());
        return HibernateUtil.saveOrUpdate(mapping);
    }

    @SuppressWarnings("null")
    private Logger logger = LoggerFactory.getLogger(this.getClass());
}
