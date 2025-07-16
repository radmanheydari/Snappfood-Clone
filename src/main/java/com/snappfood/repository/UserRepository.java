package com.snappfood.repository;

import com.snappfood.model.Restaurant;
import com.snappfood.model.User;
import com.snappfood.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    public User save(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(user);
            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving user", e);
        }
    }

    public User update(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.update(user);
            tx.commit();
            return user;
        }
    }

    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.get(User.class, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            throw new RuntimeException("Error finding user by id", e);
        }
    }

    public boolean existsByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
            query.setParameter("email", email);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error checking email existence", e);
        }
    }

    public boolean existsByPhone(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(u) FROM User u WHERE u.phone = :phone", Long.class);
            query.setParameter("phone", phone);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error checking phone existence", e);
        }
    }

    public Optional<User> findByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User u WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            return Optional.ofNullable(query.uniqueResult());
        } catch (Exception e) {
            throw new RuntimeException("Error finding user by email", e);
        }
    }

    public Optional<User> findByPhone(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User u WHERE u.phone = :phone", User.class);
            query.setParameter("phone", phone);
            return Optional.ofNullable(query.uniqueResult());
        } catch (Exception e) {
            throw new RuntimeException("Error finding user by phone", e);
        }
    }

    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class).list();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching all users", e);
        }
    }

    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            User user = session.get(User.class, id);
            if (user != null) {
                session.delete(user);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting user", e);
        }
    }

    public void addRestaurantToFavorites(Long userId, Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = session.get(User.class, userId);
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
            Restaurant rest = session.get(Restaurant.class, restaurantId);
            if (rest == null) {
                throw new IllegalArgumentException("Restaurant not found");
            }

            user.getFavoriteRestaurants().add(rest);
            session.update(user);

            tx.commit();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<User> findWithFavorites(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<User> list = session.createQuery(
                            "SELECT u FROM User u " +
                                    "LEFT JOIN FETCH u.favoriteRestaurants fav " +
                                    "WHERE u.id = :uid", User.class)
                    .setParameter("uid", userId)
                    .list();
            if (list.isEmpty()) return Optional.empty();
            return Optional.of(list.get(0));
        }
    }

    public void removeRestaurantFromFavorites(Long userId, Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = session.get(User.class, userId);
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }

            Restaurant rest = session.get(Restaurant.class, restaurantId);
            if (rest == null) {
                throw new IllegalArgumentException("Restaurant not found");
            }

            user.getFavoriteRestaurants().remove(rest);
            tx.commit();
        }
    }
}