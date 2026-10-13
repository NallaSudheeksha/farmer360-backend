package com.anurag.cse.repository;
import com.anurag.cse.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CropRepository extends JpaRepository<Crop, Long> {
    List<Crop> findByQuantityKgGreaterThan(double q);
    List<Crop> findByFarmerPhone(String phone);
    List<Crop> findByNameIgnoreCase(String name);
}