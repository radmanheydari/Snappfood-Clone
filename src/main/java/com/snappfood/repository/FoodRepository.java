package com.snappfood.repository;

import com.snappfood.model.Food;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class FoodRepository {

    public Food save(Food food) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(food);
            transaction.commit();
            return food;
        }
    }

    public List<Food> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Food", Food.class).list();
        }
    }

    public Optional<Food> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Food.class, id));
        }
    }

    public Food update(Food food) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.update(food);
            transaction.commit();
            return food;
        }
    }

    public void delete(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Food food = session.get(Food.class, id);
            if (food != null) {
                session.delete(food);
            }
            transaction.commit();
        }
    }
}