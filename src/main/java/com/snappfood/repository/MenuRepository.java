package com.snappfood.repository;

import com.snappfood.model.Food;
import com.snappfood.model.Menu;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Optional;

public class MenuRepository {

    public Optional<Menu> findByRestaurantId(Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Menu m WHERE m.restaurant.id = :restaurantId", Menu.class)
                    .setParameter("restaurantId", restaurantId)
                    .uniqueResultOptional();
        }
    }

    public Menu save(Menu menu) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.save(menu);
            session.getTransaction().commit();
            return menu;
        }
    }

    public Optional<Menu> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.get(Menu.class, id));
        }
    }

    public void delete(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            Menu menu = session.get(Menu.class, id);
            if (menu != null) {
                session.delete(menu);
            }
            transaction.commit();
        }
    }
}
