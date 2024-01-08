package com.cm327.repository;

import com.cm327.entity.Company;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositoryManager<T,ID> implements ICrud<T,ID>{ // TODO: NEEDS UPDATE METHOD. CHECK FOR UNCHECKED CAST WARNING
    private final EntityManagerFactory emf;
    private EntityManager em;
    private final T t;
    public RepositoryManager(T t){
        emf = Persistence.createEntityManagerFactory("CRM");
        this.t = t;
    }

    private void openSession(){
        em = emf.createEntityManager();
        em.getTransaction().begin();

    }

    private void closeSession(){
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e; // veya başka bir işlem yapabilirsiniz
        } finally {
            System.out.println("session closed");
            em.close();
        }
    }

    @Override
    public T save(T entity) {
       try{
           System.out.println("save called");
         openSession();
         em.persist(entity);
         closeSession();
       }catch (Exception exception){
        if (em.isOpen())
            closeSession();
       }
       return  entity;
    }

    @Override
    public Iterable<T> saveAll(Iterable<T> entities) {
        try{
            openSession();
            entities.forEach(entity->{
                em.persist(entity);
            });
            closeSession();
        }catch (Exception exception){
            if (em.isOpen())
                closeSession();
        }
        return  entities;
    }

    @Override
    public Optional<T> findById(ID id) {
        em = emf.createEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(t.getClass()); // select * from tblsecmen where id=? ->
        Root<T> root = (Root<T>) criteriaQuery.from(t.getClass());
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("id"),id));
        T t1;
        try{
            t1 =  em.createQuery(criteriaQuery).getSingleResult();
            return Optional.of(t1);
        }catch (NoResultException exception){
            return Optional.empty();
        }
    }

    @Override
    public boolean existById(ID id) {
        em = emf.createEntityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(t.getClass()); // select * from tblsecmen where id=? ->
        Root<T> root = (Root<T>) criteriaQuery.from(t.getClass());
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("id"),id));
        try{
            em.createQuery(criteriaQuery).getSingleResult();
            return true;
        }catch (NoResultException exception){
            return false;
        }
    }

    @Override
    public List<T> findAll() {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(t.getClass()); // select * from tblsecmen where id=? ->
        Root<T> root = (Root<T>) criteriaQuery.from(t.getClass());
        criteriaQuery.select(root);
        return em.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public List<T> findByColumnAndValue(String columnName, Object value) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(t.getClass()); // select * from tblsecmen where id=? ->
        Root<T> root = (Root<T>) criteriaQuery.from(t.getClass());
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get(columnName),value));
        return em.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public void deleteById(ID id) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(t.getClass()); // select * from tblsecmen where id=? ->
        Root<T> root = (Root<T>) criteriaQuery.from(t.getClass());
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("id"),id));
        T removeElement;
        try{
            removeElement =  em.createQuery(criteriaQuery).getSingleResult();
        }catch (NoResultException exception){
            removeElement = null;
        }
        try {
            openSession();
            em.remove(removeElement);
            closeSession();
        }catch (Exception e){
            if (em.isOpen())
                closeSession();
        }
    }

    @Override
    public List<T> findAllEntity(T entity) {
        List<T> list;
        Class<?> clazz = entity.getClass();
        Field[] fields = clazz.getDeclaredFields();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = (CriteriaQuery<T>) criteriaBuilder.createQuery(t.getClass()); // select * from tblsecmen where id=? ->
        Root<T> root = (Root<T>) criteriaQuery.from(t.getClass());
        criteriaQuery.select(root);
        List<Predicate> predicateList = new ArrayList<>();
        for(int i=1;i<fields.length;i++){
            fields[i].setAccessible(true);
            try{
                Object value = fields[i].get(entity);
                String column = fields[i].getName();
                if(value != null)
                    if(value instanceof String){
                        predicateList.add(criteriaBuilder.like(root.get(column),"%"+value+"%"));
                    }else {
                        predicateList.add(criteriaBuilder.equal(root.get(column),value));
                    }
            }catch (Exception exception){
                System.out.println("Hata oluştu....: "+ exception);
            }

        }
        criteriaQuery.where(predicateList.toArray(new Predicate[]{}));
        list = em.createQuery(criteriaQuery).getResultList();
        return list;
    }

    public T update(T entity) {
        EntityTransaction transaction = null;

        try {
            openSession();
            transaction = em.getTransaction();
            transaction.begin();

            em.merge(entity);  // merge kullanarak varlığı güncelleme

            transaction.commit();
        } catch (Exception exception) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }

            exception.printStackTrace();  // veya loglama yapabilirsiniz

        } finally {
            if (em.isOpen()) {
                closeSession();
            }
        }

        return entity;
    }



}