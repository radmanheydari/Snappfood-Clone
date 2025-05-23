package com.snappfood.repository;

import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Optional;

public class FoodCategoryRepository {

    public FoodCategoryRepository save(FoodCategoryRepository foodcategory) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(foodcategory);
            transaction.commit();
            return foodcategory;
        }
    }

    public List<FoodCategoryRepository> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM FoodCategory", FoodCategoryRepository.class).list();
        }
    }

    public Optional<FoodCategoryRepository> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(FoodCategoryRepository.class, id));
        }
    }

    public FoodCategoryRepository update(FoodCategoryRepository foodcategory) {
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
            FoodCategoryRepository foodcategory = session.get(FoodCategoryRepository.class, id);
            if (foodcategory != null) {
                session.delete(foodcategory);
            }
            transaction.commit();
        }
    }
}