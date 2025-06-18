package com.snappfood.repository;

import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
}