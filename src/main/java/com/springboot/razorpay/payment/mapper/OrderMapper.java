package com.springboot.razorpay.payment.mapper;

import com.springboot.razorpay.payment.dto.response.OrderResponse;
import com.springboot.razorpay.payment.entity.OrderRecord;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {
    OrderResponse toOrderResponse(OrderRecord orderRecord);
}
