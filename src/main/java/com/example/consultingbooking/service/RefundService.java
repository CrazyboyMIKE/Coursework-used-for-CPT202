package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.RefundDtos;
import com.example.consultingbooking.entity.Booking;
import com.example.consultingbooking.entity.RefundRecord;
import com.example.consultingbooking.entity.RefundStatus;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.RefundRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private static final String NO_PAYMENT_POLICY =
            "No online payment was captured by this platform; the refund synchronisation completed with no transfer required.";

    private final RefundRecordRepository refundRecordRepository;

    public RefundService(RefundRecordRepository refundRecordRepository) {
        this.refundRecordRepository = refundRecordRepository;
    }

    @Transactional
    public RefundDtos.RefundResponse synchroniseCancellation(Booking booking) {
        RefundRecord refund = refundRecordRepository.findByBookingId(booking.getId())
                .orElseGet(RefundRecord::new);
        refund.setBooking(booking);
        refund.setStatus(RefundStatus.NOT_REQUIRED);
        refund.setAmount(BigDecimal.ZERO.setScale(2));
        refund.setCurrency(BusinessConstants.DEFAULT_CURRENCY);
        refund.setPolicyMessage(NO_PAYMENT_POLICY);
        refund.setSynchronisedAt(LocalDateTime.now());
        return map(refundRecordRepository.save(refund));
    }

    @Transactional(readOnly = true)
    public RefundDtos.RefundResponse findForBooking(Long bookingId) {
        return refundRecordRepository.findByBookingId(bookingId)
                .map(this::map)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Refund synchronisation record not found"));
    }

    private RefundDtos.RefundResponse map(RefundRecord refund) {
        return new RefundDtos.RefundResponse(
                refund.getBooking().getId(),
                refund.getStatus(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getPolicyMessage(),
                refund.getSynchronisedAt()
        );
    }
}
