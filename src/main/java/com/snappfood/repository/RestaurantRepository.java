package com.snappfood.repository;

import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RestaurantRepository {

    public Restaurant save(Restaurant restaurant) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(restaurant);
            transaction.commit();
            return restaurant;
        }
    }

    public List<Restaurant> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Restaurant", Restaurant.class).list();
        }
    }

    public Optional<Restaurant> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Restaurant.class, id));
        }
    }

    public List<Restaurant> findByOwner(User owner) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Using HQL (Hibernate Query Language)
            String hql = "FROM Restaurant r WHERE r.owner = :owner";
            return session.createQuery(hql, Restaurant.class)
                    .setParameter("owner", owner)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Restaurant update(Restaurant restaurant) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.update(restaurant);
            transaction.commit();
            return restaurant;
        }
    }

    public void delete(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Restaurant restaurant = session.get(Restaurant.class, id);
            if (restaurant != null) {
                session.delete(restaurant);
            }
            transaction.commit();
        }
    }

    public List<Restaurant> findByOwnerId(Long ownerId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Restaurant r WHERE r.owner.id = :oid", Restaurant.class)
                    .setParameter("oid", ownerId)
                    .list();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Restaurant> findAll(String nameFilter, String addressFilter, String phoneFilter) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("FROM Restaurant r WHERE 1=1");
            if (nameFilter != null && !nameFilter.isBlank()) {
                hql.append(" AND lower(r.name) LIKE :name");
            }
            if (addressFilter != null && !addressFilter.isBlank()) {
                hql.append(" AND lower(r.address) LIKE :addr");
            }
            if (phoneFilter != null && !phoneFilter.isBlank()) {
                hql.append(" AND r.phone LIKE :phone");
            }
            Query<Restaurant> q = session.createQuery(hql.toString(), Restaurant.class);
            if (nameFilter != null && !nameFilter.isBlank()) {
                q.setParameter("name", "%" + nameFilter.toLowerCase() + "%");
            }
            if (addressFilter != null && !addressFilter.isBlank()) {
                q.setParameter("addr", "%" + addressFilter.toLowerCase() + "%");
            }
            if (phoneFilter != null && !phoneFilter.isBlank()) {
                q.setParameter("phone", "%" + phoneFilter + "%");
            }
            return q.list();
        }
    }
}