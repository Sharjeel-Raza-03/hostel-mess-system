package com.hostel.mess.repository;

import com.hostel.mess.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    /** Pass "" (never null) for search and category when not filtering. */
    @Query("select m from Medicine m "
            + "where (:search = '' or lower(m.name) like lower(concat('%', :search, '%'))) "
            + "and (:category = '' or m.category = :category) "
            + "and (:availableOnly = false or (m.quantity > 0 and m.expiryDate >= :today)) "
            + "order by m.name")
    List<Medicine> search(@Param("search") String search,
                          @Param("category") String category,
                          @Param("availableOnly") boolean availableOnly,
                          @Param("today") LocalDate today);

    @Query("select distinct m.category from Medicine m where m.category is not null order by m.category")
    List<String> findDistinctCategories();

    @Query("select m from Medicine m where m.expiryDate < :today or m.quantity <= :threshold "
            + "order by m.name")
    List<Medicine> findAlerts(@Param("today") LocalDate today, @Param("threshold") int threshold);

    boolean existsByNameIgnoreCaseAndExpiryDate(String name, LocalDate expiryDate);
}