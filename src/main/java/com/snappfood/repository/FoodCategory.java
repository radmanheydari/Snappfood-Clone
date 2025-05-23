package com.snappfood.repository;

import com.snappfood.repository.FoodCategory;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class FoodCategory {

    public FoodCategory save(FoodCategory foodcategory) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(foodcategory);
            transaction.commit();
            return foodcategory;
        }
    }

    public List<FoodCategory> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM FoodCategory", FoodCategory.class).list();
        }
    }

    public Optional<FoodCategory> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(FoodCategory.class, id));
        }
    }

    public FoodCategory update(FoodCategory foodcategory) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.update(foodcategory);
            transaction.commit();
            return foodcategory;
        }
    }

    public void delete(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            FoodCategory foodcategory = session.get(FoodCategory.class, id);
            if (foodcategory != null) {
                session.delete(foodcategory);
            }
            transaction.commit();
        }
    }
}