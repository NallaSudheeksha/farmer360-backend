package com.anurag.cse.controller;
import com.anurag.cse.dto.Dtos.*;
import com.anurag.cse.entity.Crop;
import com.anurag.cse.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController @RequestMapping("/api/crops") @RequiredArgsConstructor
public class CropController {
    private final CropRepository crops;
    private final UserRepository users;
    private final OrderRepository orders;
    @Value("${app.upload-dir}") String uploadDir;

    private ResponseStatusException err(HttpStatus s, String m) { return new ResponseStatusException(s, m); }

    private Crop own(Long id, String phone) {
        Crop c = crops.findById(id).orElseThrow(() -> err(HttpStatus.NOT_FOUND, "Crop not found"));
        if (!c.getFarmer().getPhone().equals(phone)) throw err(HttpStatus.FORBIDDEN, "Not your crop");
        return c;
    }

    @GetMapping
    public List<CropDto> available() { return crops.findByQuantityKgGreaterThan(0).stream().map(CropDto::from).toList(); }

    @GetMapping("/mine")
    public List<CropDto> mine(Authentication a) { return crops.findByFarmerPhone(a.getName()).stream().map(CropDto::from).toList(); }

    /** Simple market-price helper: average / min / max price of the same crop listed by other farmers. */
    @GetMapping("/price-suggestion")
    public Map<String, Object> suggest(@RequestParam String name) {
        List<Double> p = crops.findByNameIgnoreCase(name.trim()).stream().map(Crop::getPricePerKg).toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", p.size());
        m.put("average", p.isEmpty() ? 0.0 : Math.round(p.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) / 100.0);
        m.put("min", p.isEmpty() ? 0.0 : Collections.min(p));
        m.put("max", p.isEmpty() ? 0.0 : Collections.max(p));
        return m;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CropDto add(@RequestParam String name, @RequestParam double quantityKg, @RequestParam double pricePerKg,
                       @RequestParam String pickupLocation, @RequestParam(required = false) MultipartFile image,
                       Authentication a) throws IOException {
        if (name.isBlank() || pickupLocation.isBlank()) throw err(HttpStatus.BAD_REQUEST, "Crop name and pickup place are required");
        if (quantityKg <= 0 || pricePerKg <= 0) throw err(HttpStatus.BAD_REQUEST, "Quantity and price must be greater than 0");
        Crop c = new Crop();
        c.setName(name.trim()); c.setQuantityKg(quantityKg); c.setPricePerKg(pricePerKg); c.setPickupLocation(pickupLocation.trim());
        c.setFarmer(users.findByPhone(a.getName()).orElseThrow());
        if (image != null && !image.isEmpty()) {
            String type = image.getContentType();
            if (type == null || !type.startsWith("image/")) throw err(HttpStatus.BAD_REQUEST, "Only image files are allowed");
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);
            String orig = image.getOriginalFilename() == null ? "photo" : image.getOriginalFilename();
            String fn = UUID.randomUUID() + "_" + orig.replaceAll("[^a-zA-Z0-9._-]", "");
            image.transferTo(dir.resolve(fn).toAbsolutePath());
            c.setImageUrl("/uploads/" + fn);
        }
        return CropDto.from(crops.save(c));
    }

    @PutMapping("/{id}")
    public CropDto update(@PathVariable Long id, @Valid @RequestBody UpdateCropRequest r, Authentication a) {
        Crop c = own(id, a.getName());
        c.setQuantityKg(r.quantityKg());
        c.setPricePerKg(r.pricePerKg());
        return CropDto.from(crops.save(c));
    }

    /** Crops that already have orders cannot be deleted (history), so they are just delisted (quantity 0). */
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, Authentication a) {
        Crop c = own(id, a.getName());
        if (orders.existsByCropId(id)) {
            c.setQuantityKg(0);
            crops.save(c);
            return Map.of("result", "delisted");
        }
        crops.delete(c);
        return Map.of("result", "deleted");
    }
}