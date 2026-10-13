package com.anurag.cse.controller;
import com.anurag.cse.dto.Dtos.*;
import com.anurag.cse.entity.OrderStatus;
import com.anurag.cse.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class OrderController {
    private final OrderService svc;

    @PostMapping public OrderDto place(@Valid @RequestBody OrderRequest r, Authentication a) { return svc.place(r, a.getName()); }
    @GetMapping("/farmer") public List<OrderDto> farmer(Authentication a) { return svc.farmerOrders(a.getName()); }
    @GetMapping("/shop") public List<OrderDto> shop(Authentication a) { return svc.shopOrders(a.getName()); }
    @PutMapping("/{id}/accept") public OrderDto accept(@PathVariable Long id, Authentication a) { return svc.decide(id, true, a.getName()); }
    @PutMapping("/{id}/reject") public OrderDto reject(@PathVariable Long id, Authentication a) { return svc.decide(id, false, a.getName()); }
    @GetMapping("/{id}/timeline") public List<EventDto> timeline(@PathVariable Long id, Authentication a) { return svc.timeline(id, a.getName()); }

    @GetMapping("/transport/available") public List<OrderDto> available() { return svc.availableJobs(); }
    @GetMapping("/transport/mine") public List<OrderDto> mine(Authentication a) { return svc.myJobs(a.getName()); }
    @PutMapping("/{id}/claim") public OrderDto claim(@PathVariable Long id, Authentication a) { return svc.claim(id, a.getName()); }
    @PutMapping("/{id}/status") public OrderDto status(@PathVariable Long id, @RequestParam OrderStatus status, Authentication a) {
        return svc.updateStatus(id, status, a.getName());
    }
}