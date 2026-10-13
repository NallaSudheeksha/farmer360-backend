package com.anurag.cse.service;
import com.anurag.cse.dto.Dtos.*;
import com.anurag.cse.entity.*;
import com.anurag.cse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final CropRepository crops;
    private final UserRepository users;

    /** Transporter can only move an order forward, one step at a time. */
    private static final Map<OrderStatus, OrderStatus> NEXT = Map.of(
            OrderStatus.ASSIGNED, OrderStatus.PICKED_UP,
            OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT,
            OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED);

    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private CropOrder find(Long id) { return orders.findById(id).orElseThrow(() -> bad("Order not found")); }

    private void log(CropOrder o, OrderStatus s) {
        OrderEvent e = new OrderEvent();
        e.setOrder(o);
        e.setStatus(s);
        events.save(e);
    }

    @Transactional
    public OrderDto place(OrderRequest r, String phone) {
        Crop c = crops.findById(r.cropId()).orElseThrow(() -> bad("Crop not found"));
        if (r.quantityKg() <= 0 || r.quantityKg() > c.getQuantityKg()) throw bad("Not enough quantity available");
        c.setQuantityKg(c.getQuantityKg() - r.quantityKg());
        CropOrder o = new CropOrder();
        o.setCrop(c);
        o.setShopkeeper(users.findByPhone(phone).orElseThrow());
        o.setQuantityKg(r.quantityKg());
        o.setTotalAmount(r.quantityKg() * c.getPricePerKg());
        o.setDeliveryAddress(r.deliveryAddress());
        CropOrder saved = orders.save(o);
        log(saved, OrderStatus.PLACED);
        return OrderDto.from(saved);
    }

    @Transactional
    public OrderDto decide(Long id, boolean accept, String phone) {
        CropOrder o = find(id);
        if (!o.getCrop().getFarmer().getPhone().equals(phone) || o.getStatus() != OrderStatus.PLACED)
            throw bad("Not allowed");
        if (accept) o.setStatus(OrderStatus.ACCEPTED);
        else {
            o.setStatus(OrderStatus.REJECTED);
            o.getCrop().setQuantityKg(o.getCrop().getQuantityKg() + o.getQuantityKg());
        }
        log(o, o.getStatus());
        return OrderDto.from(o);
    }

    @Transactional
    public OrderDto claim(Long id, String phone) {
        CropOrder o = find(id);
        if (o.getStatus() != OrderStatus.ACCEPTED || o.getTransporter() != null) throw bad("Job not available");
        o.setTransporter(users.findByPhone(phone).orElseThrow());
        o.setStatus(OrderStatus.ASSIGNED);
        log(o, OrderStatus.ASSIGNED);
        return OrderDto.from(o);
    }

    @Transactional
    public OrderDto updateStatus(Long id, OrderStatus s, String phone) {
        CropOrder o = find(id);
        if (o.getTransporter() == null || !o.getTransporter().getPhone().equals(phone)) throw bad("Not your delivery");
        if (NEXT.get(o.getStatus()) != s) throw bad("Invalid status change");
        o.setStatus(s);
        if (s == OrderStatus.DELIVERED) o.setDeliveredAt(LocalDateTime.now());
        log(o, s);
        return OrderDto.from(o);
    }

    @Transactional(readOnly = true)
    public List<EventDto> timeline(Long id, String phone) {
        CropOrder o = find(id);
        boolean involved = o.getCrop().getFarmer().getPhone().equals(phone)
                || o.getShopkeeper().getPhone().equals(phone)
                || (o.getTransporter() != null && o.getTransporter().getPhone().equals(phone));
        if (!involved) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        List<EventDto> list = events.findByOrderIdOrderByCreatedAtAsc(id).stream()
                .map(e -> new EventDto(e.getStatus().name(), e.getCreatedAt())).toList();
        // orders created before tracking existed have no events
        return list.isEmpty() ? List.of(new EventDto(o.getStatus().name(), o.getCreatedAt())) : list;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> farmerOrders(String p) { return orders.findByCropFarmerPhoneOrderByCreatedAtDesc(p).stream().map(OrderDto::from).toList(); }
    @Transactional(readOnly = true)
    public List<OrderDto> shopOrders(String p) { return orders.findByShopkeeperPhoneOrderByCreatedAtDesc(p).stream().map(OrderDto::from).toList(); }
    @Transactional(readOnly = true)
    public List<OrderDto> myJobs(String p) { return orders.findByTransporterPhoneOrderByCreatedAtDesc(p).stream().map(OrderDto::from).toList(); }
    @Transactional(readOnly = true)
    public List<OrderDto> availableJobs() { return orders.findByStatusAndTransporterIsNull(OrderStatus.ACCEPTED).stream().map(OrderDto::from).toList(); }
}