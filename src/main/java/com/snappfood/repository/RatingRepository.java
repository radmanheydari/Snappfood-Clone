package com.snappfood.repository;

import com.snappfood.model.Rating;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class RatingRepository {
    public Rating save(Rating rating) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.save(rating);
            tx.commit();
            return rating;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
