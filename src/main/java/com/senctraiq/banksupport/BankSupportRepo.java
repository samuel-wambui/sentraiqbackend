package com.senctraiq.banksupport;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankSupportRepo extends JpaRepository<BankSupport, Long> {
    BankSupport findByMessageIdAndDeleted(String messageId, boolean b);

    List<BankSupport> findAllByTicketNumberAndDeletedOrderByTicketAssignAtAscIdAsc(String ticketNumber, boolean deleted);

    boolean existsByTicketNumberAndDeletedAndHandOverAtIsNull(String ticketNumber, boolean deleted);

    boolean existsByTicketNumberAndDeletedAndHandOverAtIsNotNull(String ticketNumber, boolean deleted);

    BankSupport findFirstByTicketNumberAndDeletedAndHandOverAtIsNullOrderByTicketAssignAtDescIdDesc(String ticketNumber, boolean deleted);
}
