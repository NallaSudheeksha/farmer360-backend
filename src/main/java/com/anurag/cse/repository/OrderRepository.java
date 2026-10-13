package com.anurag.cse.repository;
import com.anurag.cse.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface OrderRepository extends JpaRepository<CropOrder, Long> {
    List<CropOrder> findByCropFarmerPhoneOrderByCreatedAtDesc(String phone);
    List<CropOrder> findByShopkeeperPhoneOrderByCreatedAtDesc(String phone);
    List<CropOrder> findByTransporterPhoneOrderByCreatedAtDesc(String phone);
    List<CropOrder> findByStatusAndTransporterIsNull(OrderStatus status);
    boolean existsByCropId(Long cropId);
}