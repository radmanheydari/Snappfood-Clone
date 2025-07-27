package com.snappfood.repository;

import com.snappfood.model.Order;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class OrderRepository {

    public Order save(Order order) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.save(order);
            tx.commit();
            return order;
        }
    }

    public Optional<Order> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Order order = session.createQuery(
                            "SELECT o FROM Order o " +
                                    "LEFT JOIN FETCH o.items " +
                                    "LEFT JOIN FETCH o.customer " +
                                    "LEFT JOIN FETCH o.vendor " +
                                    "LEFT JOIN FETCH o.coupon " +
                                    "LEFT JOIN FETCH o.courier " +
                                    "WHERE o.id = :id",
                            Order.class
                    )
                    .setParameter("id", id)
                    .uniqueResult();
            return Optional.ofNullable(order);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Order> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Order").list();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Order> findByCustomerId(Long customerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT o FROM Order o " +
                                    "LEFT JOIN FETCH o.items " +
                                    "LEFT JOIN FETCH o.vendor " +
                                    "LEFT JOIN FETCH o.coupon " +
                                    "LEFT JOIN FETCH o.courier " +
                                    "WHERE o.customer.id = :cid " +
                                    "ORDER BY o.createdAt DESC",
                            Order.class
                    )
                    .setParameter("cid", customerId)
                    .list();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Order> findByRestaurantId(Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Order o WHERE o.vendor.id = :rid")
                    .setParameter("rid", restaurantId)
                    .list();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Order> findAvailableForDelivery() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT o FROM Order o " +
                                    "LEFT JOIN FETCH o.items " +
                                    "LEFT JOIN FETCH o.customer " +
                                    "LEFT JOIN FETCH o.vendor " +
                                    "WHERE o.status = 'submitted' " +
                                    "ORDER BY o.createdAt DESC",
                            Order.class)
                    .list();
        }
    }

    public Order update(Order order) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.update(order);
            tx.commit();
            return order;
        }
    }

    public void delete(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Order order = session.get(Order.class, id);
            if (order != null) {
                session.delete(order);
            }
            tx.commit();
        }
    }
}
