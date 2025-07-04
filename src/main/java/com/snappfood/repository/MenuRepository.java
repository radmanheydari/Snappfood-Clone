package com.snappfood.repository;

import com.snappfood.model.Menu;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;

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
}
