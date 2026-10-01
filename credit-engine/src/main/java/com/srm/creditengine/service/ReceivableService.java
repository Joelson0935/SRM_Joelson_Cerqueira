package com.srm.creditengine.service;

import com.srm.creditengine.domain.entity.Receivable;
import com.srm.creditengine.dto.request.ReceivableRequest;
import com.srm.creditengine.exception.ReceivableNotFoundException;
import com.srm.creditengine.repository.ReceivableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivableService {

    private final ReceivableRepository receivableRepository;

    public ReceivableService(ReceivableRepository receivableRepository) {
        this.receivableRepository = receivableRepository;
    }

    @Transactional
    public Receivable create(ReceivableRequest request) {
        Receivable receivable = Receivable.builder()
                .cedente(request.cedente())
                .documentNumber(request.documentNumber())
                .type(request.type())
                .faceValue(request.faceValue())
                .termInMonths(request.termInMonths())
                .dueDate(request.dueDate())
                .paymentCurrency(request.paymentCurrency())
                .build();

        return receivableRepository.save(receivable);
    }

    @Transactional(readOnly = true)
    public Receivable findById(Long id) {
        return receivableRepository.findById(id)
                .orElseThrow(() -> new ReceivableNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Receivable> findAll(Pageable pageable) {
        return receivableRepository.findAll(pageable);
    }
}
