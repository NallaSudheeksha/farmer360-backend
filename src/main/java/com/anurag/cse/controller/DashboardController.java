package com.anurag.cse.controller;
import com.anurag.cse.dto.Dtos.OrderDto;
import com.anurag.cse.entity.*;
import com.anurag.cse.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor
public class DashboardController {
    private final OrderRepository orders;

    /** Earnings are counted on the day the order was delivered. */
    private static LocalDateTime when(CropOrder o) {
        return o.getDeliveredAt() != null ? o.getDeliveredAt() : o.getCreatedAt();
    }

    @GetMapping("/farmer")
    @Transactional(readOnly = true)
    public Map<String, Object> farmer(Authentication a) {
        List<CropOrder> all = orders.findByCropFarmerPhoneOrderByCreatedAtDesc(a.getName());
        List<CropOrder> done = all.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).toList();
        Set<OrderStatus> open = EnumSet.of(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.ASSIGNED,
                OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT);
        LocalDateTime week = LocalDateTime.now().minusDays(7), month = LocalDateTime.now().minusDays(30);

        List<Map<String, Object>> chart = new ArrayList<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM");
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            double sum = done.stream().filter(o -> when(o).toLocalDate().equals(day))
                    .mapToDouble(CropOrder::getTotalAmount).sum();
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("day", day.format(f));
            p.put("amount", sum);
            chart.add(p);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("weeklyEarnings", done.stream().filter(o -> when(o).isAfter(week)).mapToDouble(CropOrder::getTotalAmount).sum());
        m.put("monthlyEarnings", done.stream().filter(o -> when(o).isAfter(month)).mapToDouble(CropOrder::getTotalAmount).sum());
        m.put("totalEarnings", done.stream().mapToDouble(CropOrder::getTotalAmount).sum());
        m.put("totalOrders", all.size());
        m.put("pendingOrders", all.stream().filter(o -> open.contains(o.getStatus())).count());
        m.put("chart", chart);
        m.put("cropsSold", done.stream().collect(Collectors.groupingBy(o -> o.getCrop().getName(),
                Collectors.summingDouble(CropOrder::getQuantityKg))));
        m.put("shopkeepers", done.stream().map(o -> o.getShopkeeper().getName()).collect(Collectors.toCollection(LinkedHashSet::new)));
        m.put("history", done.stream().map(OrderDto::from).toList());
        return m;
    }
}