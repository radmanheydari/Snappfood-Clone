package com.snappfood.repository;

import com.snappfood.model.Food;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

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

    @SuppressWarnings("unchecked")
    public List<Food> findAllWithFilters(String nameFilter,
                                         Integer minPrice,
                                         Integer maxPrice,
                                         Long categoryId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("FROM Food f WHERE 1=1");
            if (nameFilter != null && !nameFilter.isBlank()) {
                hql.append(" AND lower(f.name) LIKE :name");
            }
            if (minPrice != null) {
                hql.append(" AND f.price >= :minPrice");
            }
            if (maxPrice != null) {
                hql.append(" AND f.price <= :maxPrice");
            }
            if (categoryId != null) {
                hql.append(" AND f.category.id = :catId");
            }
            Query<Food> q = session.createQuery(hql.toString(), Food.class);
            if (nameFilter != null && !nameFilter.isBlank()) {
                q.setParameter("name", "%" + nameFilter.toLowerCase() + "%");
            }
            if (minPrice != null) {
                q.setParameter("minPrice", minPrice);
            }
            if (maxPrice != null) {
                q.setParameter("maxPrice", maxPrice);
            }
            if (categoryId != null) {
                q.setParameter("catId", categoryId);
            }
            return q.list();
        }
    }
}