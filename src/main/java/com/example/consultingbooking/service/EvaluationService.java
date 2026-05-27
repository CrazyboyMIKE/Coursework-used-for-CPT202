package com.example.consultingbooking.service;

import com.example.consultingbooking.dto.EvaluationDtos;
import com.example.consultingbooking.entity.Booking;
import com.example.consultingbooking.entity.BookingStatus;
import com.example.consultingbooking.entity.Evaluation;
import com.example.consultingbooking.entity.UserAccount;
import com.example.consultingbooking.entity.UserRole;
import com.example.consultingbooking.exception.BusinessException;
import com.example.consultingbooking.repository.BookingRepository;
import com.example.consultingbooking.repository.EvaluationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final BookingRepository bookingRepository;
    private final AuthService authService;

    public EvaluationService(
            EvaluationRepository evaluationRepository,
            BookingRepository bookingRepository,
            AuthService authService
    ) {
        this.evaluationRepository = evaluationRepository;
        this.bookingRepository = bookingRepository;
        this.authService = authService;
    }

    @Transactional
    public EvaluationDtos.EvaluationResponse submit(
            UserAccount customer,
            Long bookingId,
            EvaluationDtos.SubmitEvaluationRequest request
    ) {
        authService.ensureRole(customer, UserRole.CUSTOMER);
        Booking booking = ownedBooking(customer, bookingId);
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Only completed appointments can be evaluated");
        }
        if (evaluationRepository.existsByBookingId(bookingId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "An evaluation has already been submitted");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setBooking(booking);
        evaluation.setRating(request.rating());
        evaluation.setComment(request.comment().trim());
        return mapEvaluation(evaluationRepository.save(evaluation));
    }

    @Transactional(readOnly = true)
    public EvaluationDtos.EvaluationResponse findForBooking(UserAccount customer, Long bookingId) {
        authService.ensureRole(customer, UserRole.CUSTOMER);
        ownedBooking(customer, bookingId);
        Evaluation evaluation = evaluationRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Evaluation not found"));
        return mapEvaluation(evaluation);
    }

    private Booking ownedBooking(UserAccount customer, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Customers can only evaluate their own appointments");
        }
        return booking;
    }

    private EvaluationDtos.EvaluationResponse mapEvaluation(Evaluation evaluation) {
        return new EvaluationDtos.EvaluationResponse(
                evaluation.getId(),
                evaluation.getBooking().getId(),
                evaluation.getRating(),
                evaluation.getComment(),
                evaluation.getCreatedAt()
        );
    }
}
