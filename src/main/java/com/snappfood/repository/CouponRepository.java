package com.snappfood.repository;

import com.snappfood.model.Coupon;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;

import java.util.Optional;

public class CouponRepository {
    public Optional<Coupon> findByCode(String code) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "SELECT c FROM Coupon c WHERE c.coupon_code = :code",
                            Coupon.class
                    )
                    .setParameter("code", code)
                    .uniqueResultOptional();
        }
    }

    public Coupon save(Coupon coupon) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            var tx = session.beginTransaction();
            session.saveOrUpdate(coupon);
            tx.commit();
            return coupon;
        }
    }
}
