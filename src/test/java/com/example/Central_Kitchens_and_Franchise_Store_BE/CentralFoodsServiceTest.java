//package com.example.Central_Kitchens_and_Franchise_Store_BE.service;
//
//import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.reponse.CentralFoodsResponse;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsRequest;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.dto.request.CentralFoodsUpdateRequest;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.domain.entities.*;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.exception.custom.ResourceNotFoundException;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.mapper.CentralFoodsMapper;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.CentralFoodCategoryRepository;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.CentralFoodsRepository;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.OrderRepository;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.repository.RecipeRepository;
//import com.example.Central_Kitchens_and_Franchise_Store_BE.util.RandomGeneratorUtil;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CentralFoodsServiceTest {
//
//    @Mock private CentralFoodsRepository centralFoodsRepository;
//    @Mock private RecipeRepository recipeRepository;
//    @Mock private CentralFoodsRepository foodsRepository;
//    @Mock private CentralFoodsMapper centralFoodsMapper;
//    @Mock private CentralFoodCategoryRepository centralFoodCategoryRepository;
//    @Mock private OrderRepository orderRepository;
//    @Mock private RandomGeneratorUtil randomGeneratorUtil;
//
//    @InjectMocks private CentralFoodsService service;
//
//    private CentralFoodsRequest request;
//
//    @BeforeEach
//    void setUp() {
//        request = CentralFoodsRequest.builder()
//                .foodName("Pho Bo")
//                .amount(new BigDecimal("10"))
//                .expiryDate(LocalDate.now().plusDays(10))
//                .manufacturingDate(LocalDate.now())
//                .centralFoodStatus(com.example.Central_Kitchens_and_Franchise_Store_BE.domain.enums.FoodStatus.AVAILABLE)
//                .unitPriceFood(50000)
//                .recipeId("RE_001")
//                .weight(1000)
//                .length(10)
//                .width(10)
//                .height(10)
//                .centralFoodTypeId("CT_01")
//                .build();
//    }
//
//    @Test
//    @DisplayName("createFood: maps DTO, validates refs, generates id, saves and returns response")
//    void createFood_Success() {
//        CentralFoods mapped = CentralFoods.builder().foodName(request.getFoodName()).build();
//        CentralFoodCategory category = CentralFoodCategory.builder().centralFoodTypeId("CT_01").centralFoodTypeName("Noodle").build();
//        Recipe recipe = Recipe.builder().recipeId("RE_001").build();
//        CentralFoods saved = CentralFoods.builder().centralFoodId("CE_01FO_123456").foodName("Pho Bo").build();
//        CentralFoodsResponse response = CentralFoodsResponse.builder().foodId("CE_01FO_123456").foodName("Pho Bo").build();
//
//        when(centralFoodsMapper.convertToEntity(request)).thenReturn(mapped);
//        when(centralFoodCategoryRepository.findById("CT_01")).thenReturn(Optional.of(category));
//        when(recipeRepository.findById("RE_001")).thenReturn(Optional.of(recipe));
//        when(randomGeneratorUtil.randomSix()).thenReturn("123456");
//        when(foodsRepository.save(any(CentralFoods.class))).thenReturn(saved);
//        when(centralFoodsMapper.convertToDTO(saved)).thenReturn(response);
//
//        CentralFoodsResponse result = service.createFood(request);
//
//        assertNotNull(result);
//        assertEquals("CE_01FO_123456", result.getFoodId());
//        verify(centralFoodsMapper).convertToEntity(request);
//        verify(foodsRepository).save(any(CentralFoods.class));
//        verify(centralFoodsMapper).convertToDTO(saved);
//    }
//
//    @Test
//    @DisplayName("createFood: throws when category not found")
//    void createFood_CategoryNotFound() {
//        when(centralFoodsMapper.convertToEntity(request)).thenReturn(new CentralFoods());
//        when(centralFoodCategoryRepository.findById("CT_01")).thenReturn(Optional.empty());
//
//        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.createFood(request));
//        assertTrue(ex.getMessage().contains("Central food category"));
//    }
//
//    @Test
//    @DisplayName("getFoodById: returns mapped response when found")
//    void getFoodById_Found() {
//        CentralFoods food = CentralFoods.builder().centralFoodId("ID1").foodName("Pho").build();
//        CentralFoodsResponse dto = CentralFoodsResponse.builder().foodId("ID1").foodName("Pho").build();
//        when(foodsRepository.findById("ID1")).thenReturn(Optional.of(food));
//        when(centralFoodsMapper.convertToDTO(food)).thenReturn(dto);
//
//        CentralFoodsResponse result = service.getFoodById("ID1");
//        assertEquals("ID1", result.getFoodId());
//    }
//
//    @Test
//    @DisplayName("getFoodById: throws when not found")
//    void getFoodById_NotFound() {
//        when(foodsRepository.findById("MISSING")).thenReturn(Optional.empty());
//        assertThrows(ResourceNotFoundException.class, () -> service.getFoodById("MISSING"));
//    }
//
//    @Test
//    @DisplayName("getExpiredFoods: delegates to repository and mapper")
//    void getExpiredFoods() {
//        List<CentralFoods> foods = List.of(new CentralFoods());
//        List<CentralFoodsResponse> dtos = List.of(CentralFoodsResponse.builder().foodId("X").build());
//        when(foodsRepository.findByExpiryDateBefore(any(LocalDate.class))).thenReturn(foods);
//        when(centralFoodsMapper.convertToDTOList(foods)).thenReturn(dtos);
//
//        List<CentralFoodsResponse> result = service.getExpiredFoods();
//        assertEquals(1, result.size());
//        verify(foodsRepository).findByExpiryDateBefore(any(LocalDate.class));
//        verify(centralFoodsMapper).convertToDTOList(foods);
//    }
//
//    @Test
//    @DisplayName("decreaseFoodAmountByOrder: deducts quantities and saves each food")
//    void decreaseFoodAmountByOrder_DeductsQuantities() {
//        // Arrange order with two items
//        Order order = Order.builder().orderId("O1").build();
//        OrderDetail detail = OrderDetail.builder().orderDetailId("OD1").build();
//        OrderDetailItem item1 = OrderDetailItem.builder().centralFoodId("F1").quantity(2).build();
//        OrderDetailItem item2 = OrderDetailItem.builder().centralFoodId("F2").quantity(3).build();
//        detail.setOrderDetailItems(Arrays.asList(item1, item2));
//        order.setOrderDetail(detail);
//
//        CentralFoods f1 = CentralFoods.builder().centralFoodId("F1").foodName("Food1").amount(new BigDecimal("10")).build();
//        CentralFoods f2 = CentralFoods.builder().centralFoodId("F2").foodName("Food2").amount(new BigDecimal("5")).build();
//
//        when(orderRepository.findById("O1")).thenReturn(Optional.of(order));
//        when(foodsRepository.findById("F1")).thenReturn(Optional.of(f1));
//        when(foodsRepository.findById("F2")).thenReturn(Optional.of(f2));
//
//        // Act
//        service.decreaseFoodAmountByOrder("O1");
//
//        // Assert
//        assertEquals(new BigDecimal("8"), f1.getAmount());
//        assertEquals(new BigDecimal("2"), f2.getAmount());
//        verify(foodsRepository, times(2)).save(any(CentralFoods.class));
//    }
//
//    @Test
//    @DisplayName("decreaseFoodAmountByOrder: throws when insufficient stock")
//    void decreaseFoodAmountByOrder_InsufficientStock() {
//        Order order = Order.builder().orderId("O1").build();
//        OrderDetail detail = OrderDetail.builder().orderDetailId("OD1").build();
//        OrderDetailItem item = OrderDetailItem.builder().centralFoodId("F1").quantity(10).build();
//        detail.setOrderDetailItems(List.of(item));
//        order.setOrderDetail(detail);
//
//        CentralFoods f1 = CentralFoods.builder().centralFoodId("F1").foodName("Food1").amount(new BigDecimal("5")).build();
//
//        when(orderRepository.findById("O1")).thenReturn(Optional.of(order));
//        when(foodsRepository.findById("F1")).thenReturn(Optional.of(f1));
//
//        assertThrows(IllegalStateException.class, () -> service.decreaseFoodAmountByOrder("O1"));
//        verify(foodsRepository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("updateFood: updates fields, optional refs and saves")
//    void updateFood_Success() {
//        CentralFoods existing = CentralFoods.builder().centralFoodId("F1").build();
//        when(foodsRepository.findById("F1")).thenReturn(Optional.of(existing));
//        when(foodsRepository.save(existing)).thenReturn(existing);
//        CentralFoodsResponse mapped = CentralFoodsResponse.builder().foodId("F1").build();
//        when(centralFoodsMapper.convertToDTO(existing)).thenReturn(mapped);
//
//        CentralFoodsUpdateRequest upd = CentralFoodsUpdateRequest.builder()
//                .foodName("New")
//                .amount(new BigDecimal("2"))
//                .expiryDate(LocalDate.now().plusDays(1))
//                .manufacturingDate(LocalDate.now())
//                .unitPriceFood(1)
//                .weight(1).length(1).width(1).height(1)
//                .recipeId("")
//                .centralFoodTypeId("")
//                .build();
//
//        CentralFoodsResponse result = service.updateFood("F1", upd);
//        assertEquals("F1", result.getFoodId());
//        assertEquals("New", existing.getFoodName());
//        verify(foodsRepository).save(existing);
//    }
//
//    @Test
//    @DisplayName("deleteFood: deletes when exists, else throws")
//    void deleteFood() {
//        when(foodsRepository.existsById("F1")).thenReturn(true);
//        service.deleteFood("F1");
//        verify(foodsRepository).deleteById("F1");
//
//        when(foodsRepository.existsById("F2")).thenReturn(false);
//        assertThrows(RuntimeException.class, () -> service.deleteFood("F2"));
//    }
//}
